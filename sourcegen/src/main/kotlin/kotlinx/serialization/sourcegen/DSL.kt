/*
 * Copyright 2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlinx.serialization.sourcegen

import com.squareup.kotlinpoet.*
import kotlinx.serialization.*
import kotlinx.serialization.internal.SerialClassDescImplTagged
import java.io.File
import java.lang.Appendable
import kotlin.reflect.KClass

class SClass(
        val serializableClassName: ClassName,
        var dataClass: Boolean = true
) {
    private val properties: MutableList<SProperty> = arrayListOf()
    private val allProperties: MutableList<SProperty> = arrayListOf()

    fun property(name: String, type: KClass<*>, init: SProperty.() -> Unit = {}) {
        property(name, type.asTypeName(), init)
    }

    fun property(name: String, type: TypeName, init: SProperty.() -> Unit = {}) {
        val prop = SProperty(name, type)
        prop.init()
        if (!prop.transient) properties.add(prop)
        allProperties.add(prop)
    }

    fun render(mode: RenderMode) = TypeSpec.classBuilder(serializableClassName).apply {
        if (dataClass) addModifiers(KModifier.DATA)
        primaryConstructor(FunSpec.constructorBuilder()
                .addParameters(allProperties.map(SProperty::asParameter))
                .build())
        addProperties(allProperties.map(SProperty::asProperty))
        if (mode == RenderMode.ANNOTATION) {
            addAnnotation(Serializable::class)
        } else {
            companionObject(renderCompanion())
            addType(renderSerializer())
        }
    }.build()

    private fun renderCompanion() = TypeSpec.companionObjectBuilder()
            .addFunction(FunSpec.builder("serializer").apply {
                addStatement("return serializer")
            }.build())
            .build()

    private fun renderSerializer() = TypeSpec.objectBuilder("serializer").apply {
        addSuperinterface(ParameterizedTypeName.get(KSerializer::class.asTypeName(), serializableClassName))
        addAnnotation(AnnotationSpec.builder(kotlin.Suppress::class).addMember("%S", "NAME_SHADOWING").build())
        addProperty(renderDescriptor())
        addFunction(renderSave())
        addFunction(renderLoad())
    }.build()

    private fun renderInvoke(serializer: TypeName, typeParam: TypeName? = null): CodeBlock {
        return when (serializer) {
            is ClassName -> if (typeParam == null) CodeBlock.of("%T", serializer) else CodeBlock.of("%T<%T>()", serializer, typeParam.asNonNullable())
            is ParameterizedTypeName -> {
                val args = serializer.typeArguments.map { renderInvoke(it, typeParam) }.toTypedArray()
                val literals = args.indices.joinToString(separator = ", ", transform = { "%L" })
                CodeBlock.of("%T($literals)", serializer.rawType, *args)
            }
            else -> TODO("Too complex type")
        }
    }

    private fun renderLoad() = FunSpec.builder("deserialize").apply {
        addModifiers(KModifier.OVERRIDE)
        addParameter("input", KInput::class)
        returns(serializableClassName)
        l("val input = input.readBegin(serialClassDesc)")
        properties.forEachIndexed { index, prop ->
            l("var local$index: %T = null", prop.type.asNullable())
        }
        l("var bitMask: Int = 0")


        beginControlFlow("mainLoop@while (true)")
        l("val idx = input.readElement(serialClassDesc)")
        beginControlFlow("when (idx)")
        beginControlFlow("-1 ->")
        l("break@mainLoop")
        endControlFlow() // end -1

        properties.forEachIndexed { index, it ->
            beginControlFlow("$index ->")
            val sti = it.getSerialTypeInfo()
            if (sti.serializer == null) l("local$index = input.read${sti.elementMethodPrefix}ElementValue(serialClassDesc, $index)")
            else {
                val invokeArgs = renderInvoke(sti.serializer, if (sti.needTypeParam) it.type else null)
                l("local$index = input.read${sti.elementMethodPrefix}ElementValue(serialClassDesc, $index, %L)", invokeArgs)
            }
            l("bitMask = bitMask or ${1 shl index}")
            endControlFlow()
        }

        endControlFlow() // end when
        endControlFlow() // end while

        l("input.readEnd(serialClassDesc)")
        properties.forEachIndexed { index, prop ->
            beginControlFlow("if (bitMask and ${1 shl index} == 0)")
            if (prop.optional) {
                l("local$index = %L", prop.defaultValue
                        ?: throw SerializationException("Optional without default value"))
            } else {
                l("throw %T(%S)", MissingFieldException::class, prop.name)
            }
            endControlFlow()
        }
        val constructorArgs = allProperties.mapIndexed { index, prop ->
            if (prop.transient) prop.defaultValue
                    ?: throw SerializationException("Transient without an initializer")
            else "local$index${if (prop.type.nullable) "" else "!!"}"
        }.joinToString()
        addStatement("return %T($constructorArgs)", serializableClassName)
    }.build()

    private fun renderDescriptor() = PropertySpec.builder("serialClassDesc", KSerialClassDesc::class).apply {
        addModifiers(KModifier.OVERRIDE)
        val initCodeBlock = CodeBlock.builder().apply {
            if (properties.all { it.serialTag == null }) {
                properties.map(SProperty::name).forEach {
                    add("addElement(%S)\n", it)
                }
            } else {
                properties.forEachIndexed { idx, prop ->
                    add("addTaggedElement(%S, %L)\n", prop.name, prop.serialTag ?: idx+1)
                }
            }
        }.build()
        val descImpl = TypeSpec.anonymousClassBuilder("%S", serializableClassName.canonicalName).apply {
            superclass(SerialClassDescImplTagged::class)
            // https://github.com/square/kotlinpoet/issues/332 ?
            // addSuperclassConstructorParameter("%S", serializableClassName.canonicalName)
            addInitializerBlock(initCodeBlock)
        }.build()
        initializer("%L", descImpl)
    }.build()

    private fun FunSpec.Builder.l(s: String, vararg formatArgs: Any) = addStatement(s, *formatArgs)

    private fun renderSave() = FunSpec.builder("serialize").apply {
        addModifiers(KModifier.OVERRIDE)
        addParameter("output", KOutput::class)
        addParameter("obj", serializableClassName)
        l("val output = output.writeBegin(serialClassDesc)")

        properties.forEachIndexed { index, it ->
            val sti = it.getSerialTypeInfo()
            if (sti.serializer == null) {
                if (it.type == Unit::class.asTypeName())
                    l("output.write${sti.elementMethodPrefix}ElementValue(serialClassDesc, $index)")
                else
                    l("output.write${sti.elementMethodPrefix}ElementValue(serialClassDesc, $index, obj.${it.name})")
            }
            else {
                val invokeArgs = renderInvoke(sti.serializer, if (sti.needTypeParam) it.type else null)
                l("output.write${sti.elementMethodPrefix}ElementValue(serialClassDesc, $index, %L, obj.${it.name})", invokeArgs)
            }
        }
        l("output.writeEnd(serialClassDesc)")
    }.build()

    class SProperty(
            val name: String,
            val type: TypeName,
            var mutable: Boolean = false,
            var optional: Boolean = false,
            var defaultValue: String? = null,
            var transient: Boolean = false,
            var isEnum: Boolean = false,
            var serialTag: Int? = null
    ) {
        internal fun asParameter() = ParameterSpec.builder(name, type).apply {
            if (defaultValue != null) defaultValue(defaultValue!!)
        }.build()

        internal fun asProperty() = (if (!mutable) PropertySpec.builder(name, type) else PropertySpec.varBuilder(name, type))
                .apply {
                    initializer(name)
                    if (optional) addAnnotation(Optional::class)
                    if (transient) addAnnotation(Transient::class)
                    if (serialTag != null) addAnnotation(AnnotationSpec.builder(SerialId::class).addMember("%L", serialTag!!).build())
                }.build()
    }
}

class GeneratedFile(val packageName: String, val fileName: String) {
    private val classes: MutableList<SClass> = arrayListOf<SClass>()
    private val otherTypes: MutableList<TypeSpec> = arrayListOf()

    var renderMode: RenderMode = RenderMode.SOURCE

    fun serializableClass(name: String, init: SClass.() -> Unit): SClass {
        val klass = SClass(ClassName(packageName, name))
        klass.init()
        classes.add(klass)
        return klass
    }

    fun addType(type: TypeSpec) = otherTypes.add(type)

    private fun render(): FileSpec = FileSpec.builder(packageName, fileName).apply {
        indent("    ")
        addComment("Auto-generated file, do not modify!")
        otherTypes.forEach { addType(it) }
        classes.forEach { addType(it.render(renderMode)) }
    }.build()

    /**
     * Prints content to stdout.
     */
    fun print() = render().writeTo(System.out)

    fun writeTo(out: Appendable) = render().writeTo(out)

    fun saveTo(directory: File) = render().writeTo(directory)
}

enum class RenderMode { ANNOTATION, SOURCE }

fun generateFile(packageName: String, fileName: String, init: (GeneratedFile.() -> Unit)): GeneratedFile {
    val f = GeneratedFile(packageName, fileName)
    f.init()
    return f
}

fun saveFile(outputDir: File, packageName: String, fileName: String, init: (GeneratedFile.() -> Unit)) {
    generateFile(packageName, fileName, init).saveTo(outputDir)
}
