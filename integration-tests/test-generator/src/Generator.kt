/*
 *  Copyright 2018 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import com.squareup.kotlinpoet.*
import kotlinx.serialization.sourcegen.GeneratedFile
import kotlinx.serialization.sourcegen.RenderMode
import kotlinx.serialization.sourcegen.generateFile
import java.io.File
import kotlin.reflect.KClass

private val <T : Any> KClass<T>.nullableRef: ClassName
    get() = this.asTypeName().asNullable()

fun genZoo() = generateFile("", "Zoo") {
    val attitude = TypeSpec.enumBuilder("Attitude")
            .addEnumConstant("POSITIVE")
            .addEnumConstant("NEUTRAL")
            .addEnumConstant("NEGATIVE")
            .build()
    addType(attitude)
    val attitudeClass = ClassName.bestGuess(attitude.name!!)
    val simpleClass = serializableClass("Simple") {
        dataClass = true
        property("a", String::class)
    }.serializableClassName

    serializableClass("SmallZoo") {
        dataClass = true
        property("str", String::class)
        property("i", Int::class)
        property("nullable", Double::class.nullableRef)
        property("list", ParameterizedTypeName.get(List::class, Int::class))
        property("map", ParameterizedTypeName.get(Map::class, Int::class, Boolean::class))
        property("inner", serializableClassName)
        property("innerList", ParameterizedTypeName.get(List::class.asTypeName(), simpleClass))
    }
    serializableClass("Zoo") {
        dataClass = true
        property("unit", Unit::class)
        property("boolean", Boolean::class)
        property("byte", Byte::class)
        property("short", Short::class)
        property("int", Int::class)
        property("long", Long::class)
        property("float", Float::class)
        property("double", Double::class)
        property("char", Char::class)
        property("string", String::class)
        property("simple", simpleClass)
        property("enum", attitudeClass) {
            isEnum = true
        }
        property("booleanN", Boolean::class.nullableRef)
        property("byteN", Byte::class.nullableRef)
        property("shortN", Short::class.nullableRef)
        property("intN", Int::class.nullableRef)
        property("longN", Long::class.nullableRef)
        property("floatN", Float::class.nullableRef)
        property("doubleN", Double::class.nullableRef)
        property("charN", Char::class.nullableRef)
        property("stringN", String::class.nullableRef)
        property("simpleN", simpleClass.asNullable())
        property("enumN", attitudeClass.asNullable()) {
            isEnum = true
        }
        property("listInt", ParameterizedTypeName.get(List::class, Int::class))
        property("listIntN", ParameterizedTypeName.get(List::class.asTypeName(), Int::class.nullableRef))
        property("setNInt", ParameterizedTypeName.get(Set::class.nullableRef, Int::class.asTypeName()))
        property("mutableSetNIntN", ParameterizedTypeName.get(MutableSet::class.nullableRef, Int::class.nullableRef))
        property("listListSimple", ParameterizedTypeName.get(
                List::class.asTypeName(),
                ParameterizedTypeName.get(List::class.asTypeName(), simpleClass)
        ))
        property("listListSimpleN", ParameterizedTypeName.get(
                List::class.asTypeName(),
                ParameterizedTypeName.get(List::class.asTypeName(), simpleClass.asNullable())
        ))
        property("map", ParameterizedTypeName.get(
                Map::class,
                String::class, Int::class
        ))
        property("mapN", ParameterizedTypeName.get(
                Map::class.asTypeName(),
                Int::class.asTypeName(), String::class.nullableRef
        ))
    }
}

fun genFiles(): List<GeneratedFile> {
    val testFile1 = generateFile("", "SimpleData") {
        serializableClass("MyData") {
            dataClass = true
            property("x", Int::class.nullableRef)
            property("y", String::class) {
                optional = true
                defaultValue = "\"foo\""
            }
            property("intList", ParameterizedTypeName.get(List::class, Int::class)) {
                defaultValue = "listOf(1,2,3)"
            }
            property("trans", Int::class) {
                defaultValue = "42"
                transient = true
            }
        }
    }
    val testFile2 = generateFile("", "ObjectListCase") {
        val c1 = serializableClass("Data") {
            dataClass = true
            property("a", Int::class) {
                serialTag = 1
            }
            property("b", String::class) {
                serialTag = 2
            }
        }
        serializableClass("DataList") {
            dataClass = true
            property("list", ParameterizedTypeName.get(List::class.asClassName(), c1.serializableClassName)) {
                serialTag = 1
                optional = true
                defaultValue = "emptyList()"
            }
        }
    }
    return listOf(testFile1, testFile2)
}

fun main(args: Array<String>) {
    val generatedFiles = genFiles() + genZoo()
    generatedFiles.forEach { generatedFile ->
        if (args.isEmpty())
            generatedFile.print()
        else {
            generatedFile.saveTo(File(args[0]))
            generatedFile.renderMode = RenderMode.ANNOTATION
            generatedFile.saveTo(File(args[1]))
        }
    }
}
