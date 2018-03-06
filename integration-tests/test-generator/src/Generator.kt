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

import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import kotlinx.serialization.sourcegen.GeneratedFile
import kotlinx.serialization.sourcegen.RenderMode
import kotlinx.serialization.sourcegen.generateFile
import java.io.File

fun genFiles(): List<GeneratedFile> {
    val testFile1 = generateFile("", "SimpleData") {
        serializableClass("MyData") {
            dataClass = true
            property("x", Int::class.asTypeName().asNullable())
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
    val generatedFiles = genFiles()
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
