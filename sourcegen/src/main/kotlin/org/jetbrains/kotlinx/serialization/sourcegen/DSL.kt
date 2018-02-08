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

package org.jetbrains.kotlinx.serialization.sourcegen

import com.squareup.kotlinpoet.*
import kotlinx.serialization.*
import kotlinx.serialization.internal.SerialClassDescImpl
import kotlin.reflect.KClass

internal class SClass(
        val serializableClassName: ClassName,
        var dataClass: Boolean = false
) {
    private val properties: MutableList<SProperty> = arrayListOf()

    fun property(name: String, type: KClass<*>, init: SProperty.() -> Unit) {
        property(name, type.asTypeName(), init)
    }

    fun property(name: String, type: TypeName, init: SProperty.() -> Unit) {
        val prop = SProperty(name, type)
        prop.init()
        properties.add(prop)
    }

    fun render() = TypeSpec.classBuilder(serializableClassName).apply {
        addAnnotation(Serializable::class)
        if (dataClass) addModifiers(KModifier.DATA)
        primaryConstructor(FunSpec.constructorBuilder()
                .addParameters(properties.map(SProperty::asParameter))
                .build())
        addProperties(properties.map(SProperty::asProperty))
        companionObject(renderCompanion())
        addType(renderSerializer())
    }.build()

    private fun renderCompanion() = TypeSpec.companionObjectBuilder().build()

    private fun renderSerializer() = TypeSpec.objectBuilder("serializer").apply {
        addSuperinterface(ParameterizedTypeName.get(KSerializer::class.asTypeName(), serializableClassName))
        addProperty(renderDescriptor())
        addFunction(renderSave())
        addFunction(renderLoad())
    }.build()

    private fun renderLoad() = FunSpec.builder("load").apply {
        addModifiers(KModifier.OVERRIDE)
        addParameter("input", KInput::class)
        returns(serializableClassName)
        l("val input = input.readBegin(serialClassDesc)")
        properties.forEachIndexed { index, prop ->
            l("var local$index: %T = null", prop.type.asNullable())
        }
        l("var bitMask: Int = 0")


        beginControlFlow("mainLoop@while(true)")
        l("val idx = input.readElement(serialClassDesc)")
        beginControlFlow("when(idx)")
        beginControlFlow("-1 ->")
        l("break@mainLoop")
        endControlFlow() // end -1

        properties.forEachIndexed { index, it ->
            beginControlFlow("$index ->")
            val sti = it.getSerialTypeInfo()
            if (sti.serializer == null) l("local$index = input.read${sti.elementMethodPrefix}ElementValue(serialClassDesc, $index)")
            else l("local$index = input.read${sti.elementMethodPrefix}ElementValue(serialClassDesc, $index, %T)", sti.serializer)
            l("bitMask = bitMask or ${1 shl index}")
            endControlFlow()
        }

        endControlFlow() // end when
        endControlFlow() // end while

        l("input.readEnd(serialClassDesc)")
        properties.forEachIndexed { index, prop ->
            beginControlFlow("if (bitMask and ${1 shl index} == 0)")
            l("throw %T(%S)", MissingFieldException::class, prop.name)
            endControlFlow()
        }
        val constructorArgs = properties.mapIndexed { index, prop -> "local$index${if (prop.type.nullable) "" else "!!"}" }.joinToString()
        addStatement("return %T($constructorArgs)", serializableClassName)
    }.build()

    private fun renderDescriptor() = PropertySpec.builder("serialClassDesc", KSerialClassDesc::class).apply {
        addModifiers(KModifier.OVERRIDE)
        val initCodeBlock = CodeBlock.builder().apply {
            properties.map(SProperty::name).forEach {
                add("addElement(%S)\n", it)
            }
        }.build()
        initializer("object : %T(%S) {%>\ninit {%>\n%L%<\n}%<\n}", SerialClassDescImpl::class, serializableClassName.canonicalName, initCodeBlock)
    }.build()

    private fun FunSpec.Builder.l(s: String, vararg formatArgs: Any) = addStatement(s, *formatArgs)

    private fun renderSave() = FunSpec.builder("save").apply {
        addModifiers(KModifier.OVERRIDE)
        addParameter("output", KOutput::class)
        addParameter("obj", serializableClassName)
        l("val output = output.writeBegin(serialClassDesc)")

        properties.forEachIndexed { index, it ->
            val sti = it.getSerialTypeInfo()
            if (sti.serializer == null) l("output.write${sti.elementMethodPrefix}ElementValue(serialClassDesc, $index, obj.${it.name})")
            else l("output.write${sti.elementMethodPrefix}ElementValue(serialClassDesc, $index, %T, obj.${it.name})", sti.serializer)
        }
        l("output.writeEnd(serialClassDesc)")
    }.build()

    internal class SProperty(
            val name: String,
            val type: TypeName,
            var mutable: Boolean = false,
            var optional: Boolean = false,
            var defaultValue: String? = null,
            var transient: Boolean = false
    ) {
        fun asParameter() = ParameterSpec.builder(name, type).apply {
            if (defaultValue != null) defaultValue(defaultValue!!)
        }.build()

        fun asProperty() = (if (!mutable) PropertySpec.builder(name, type) else PropertySpec.varBuilder(name, type))
                .apply {
                    initializer(name)
                    if (optional) addAnnotation(Optional::class)
                    if (transient) addAnnotation(Transient::class)
                }.build()
    }
}


internal fun serializableClass(packageName: String, name: String, init: SClass.() -> Unit): SClass {
    val klass = SClass(ClassName(packageName, name))
    klass.init()
    return klass
}