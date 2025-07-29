/*
 * Copyright 2017-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.r8

import java.io.File
import kotlin.reflect.KClass

fun getR8Checker(): R8Checker {
    val usageFile = File(System.getProperty("r8.output.usage"))
    val mapFile = File(System.getProperty("r8.output.map"))

    return R8Checker(parseR8Output(mapFile, usageFile))
}

class R8Checker(private val classes: Map<String, ClassEntry>) {
    /**
     * Find R8 information about class by its binary name.
     */
    fun findClass(binaryName: String): ClassChecker {
        return classes[binaryName]?.let { ClassChecker(it) }
            ?: throw IllegalArgumentException("Class with binary name '$binaryName' not found")
    }

    /**
     * Find R8 information about class by given [clazz].
     */
    fun findClass(clazz: KClass<*>): ClassChecker {
        val obfuscatedName = clazz.java.name
        val found = classes.values.singleOrNull { it.obfuscatedName == obfuscatedName } ?: throw IllegalArgumentException("Class with obfuscated name '$obfuscatedName' not found")
        return ClassChecker(found)
    }
}

class ClassChecker(private val clazz: ClassEntry) : R8Result(clazz.originalName != clazz.obfuscatedName, clazz.removed) {
    val originalName: String = clazz.originalName

    /**
     * Find R8 information about a field by its name.
     *
     * If there is no field with name [name], [IllegalArgumentException] will be thrown.
     */
    fun findField(name: String): R8Result {
        return clazz.fields[name]
            ?.let { R8Result(it.name != it.obfuscatedName, it.removed) }
            ?: throw IllegalArgumentException("Field with name '$name' not found in class '${clazz.originalName}'")
    }

    /**
     * Find R8 information about a method by its name.
     * If there are several methods with name [name], [IllegalArgumentException] will be thrown.
     * If there is no method with name [name], [IllegalArgumentException] will be thrown.
     */
    fun findMethod(name: String): R8Result {
        val methods = clazz.methods.values.filter { it.name == name }
        if (methods.isEmpty()) {
            throw IllegalArgumentException("Method with name '$name' not found in class '${clazz.originalName}'")
        } else if (methods.size > 1) {
            throw IllegalArgumentException("Several methods with name '$name' found in class '${clazz.originalName}'")
        } else {
            return methods.single().let { R8Result(it.name != it.obfuscatedName, it.removed) }
        }
    }
}


data class ClassEntry(
    val originalName: String,
    val obfuscatedName: String,
    var removed: Boolean = false,
    val fields: MutableMap<String, FieldEntry> = mutableMapOf(),
    val methods: MutableMap<String, MethodEntry> = mutableMapOf()
)

data class FieldEntry(
    val name: String,
    val type: String,
    val obfuscatedName: String,
    var removed: Boolean = false
)

data class MethodEntry(
    val name: String,
    val returnType: String,
    val descriptor: String,
    val obfuscatedName: String,
    var removed: Boolean = false
)


open class R8Result(
    val isObfuscated: Boolean,
    val isShrunk: Boolean
)

private fun parseR8Output(mappingFile: File, usageFile: File): Map<String, ClassEntry> {
    val classMap = mutableMapOf<String, ClassEntry>()
    var currentClass: ClassEntry? = null

    // process mapping.txt
    val classRegex = Regex("""^(\S+) -> (\S+):$""")
    val methodRegex = Regex("""^(?:(\d+):\d+:)?(\S+)\s+([^\s\\(]+)(\(.*\))?:\d+(:\d+)? -> (\S+)$""")
    val fieldRegex = Regex("""^(\S+)\s+(\S+)\s+->\s+(\S+)$""")

    mappingFile.forEachLine { raw ->
        val line = raw.trim()
        if (line.startsWith("#") || line.isEmpty()) return@forEachLine

        classRegex.matchEntire(line)?.let {
            val (original, obfuscated) = it.destructured
            currentClass = ClassEntry(originalName = original, obfuscatedName = obfuscated)
            classMap[original] = currentClass
            return@forEachLine
        }

        methodRegex.matchEntire(line)?.let {
            val current = currentClass ?: throw IllegalStateException("No current class")

            val (num, returnType, name, desc, _, obfuscated) = it.destructured
            if (num.isNotEmpty()) {
                val signature = name + desc
                val existed = current.methods[signature]
                if (existed == null) {
                    current.methods[signature] = MethodEntry(name, returnType, desc, obfuscated)
                } else {
                    if (existed.obfuscatedName != obfuscated) {
                        if (obfuscated == name) {
                            current.methods.remove(signature)
                            current.methods[signature] = existed.copy(obfuscatedName = obfuscated)
                        }
                    }
                }
            }
            return@forEachLine
        }

        fieldRegex.matchEntire(line)?.let {
            val current = currentClass ?: throw IllegalStateException("No current class")

            val (type, name, obfuscated) = it.destructured
            current.fields.put(name, FieldEntry(name, type, obfuscated))
            return@forEachLine
        }

        // Special handling for Companion fields
        if (line.contains(" Companion -> ")) {
            val current = currentClass ?: throw IllegalStateException("No current class")
            val parts = line.split(" -> ")
            if (parts.size == 2) {
                val typeName = parts[0].trim()
                val obfuscatedName = parts[1].trim()
                val name = "Companion"
                current.fields.put(name, FieldEntry(name, typeName, obfuscatedName))
            }
        }
    }

    // process usage.txt
    var currentUsageClass: ClassEntry? = null

    usageFile.forEachLine { raw ->
        val line = raw.trimEnd()
        if (line.isBlank()) return@forEachLine

        if (line.startsWith("    ")) {
            // member
            val memberLine = line.trim()
            val current = currentUsageClass ?: throw IllegalStateException("No current class")
            if (memberLine.endsWith(")")) {
                val methodName = memberLine.substringBefore("(").substringAfterLast(" ")
                val desc = "(" + memberLine.substringAfter("(").removeSuffix(")") + ")"
                // Skip modifiers like static, public, final
                val parts = memberLine.substringBefore("(").split(" ")
                val returnType = parts.dropWhile { it in listOf("static", "public", "private", "protected", "final", "abstract", "synthetic") }.first()
                val signature = methodName + desc

                current.methods[signature] = MethodEntry(
                    name = methodName,
                    returnType = returnType,
                    descriptor = desc,
                    obfuscatedName = methodName,
                    removed = true
                )
            } else {
                val parts = memberLine.split(" ")
                val fieldType = parts.dropLast(1).joinToString(" ")
                val fieldName = parts.last()
                current.fields.put(
                    fieldName,
                    FieldEntry(
                        name = fieldName,
                        type = fieldType,
                        obfuscatedName = fieldName,
                        removed = true
                    )
                )
            }
        } else {
            // class
            if (line.endsWith(":")) {
                // remove class members
                val className = line.removeSuffix(":").trim()
                currentUsageClass = classMap[className]
            } else {
                // remove whole class
                val className = line.trim()
                classMap[className] = ClassEntry(className, className, removed = true)
            }
        }
    }
    return classMap
}
