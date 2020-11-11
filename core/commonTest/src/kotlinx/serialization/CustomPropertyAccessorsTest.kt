/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

@Suppress("RedundantSetter", "RedundantGetter", "JoinDeclarationAndAssignment")
class CustomPropertyAccessorsTest {
    @Serializable
    class VarPropertiesClass {
        var simple: String = "initial1"
        var simpleInferred = "initial2"

        var getterSetter: String = "initial3"
            set(value) {
                field = value
            }
            get() = field

        var getterSetterInferred = "initial4"
            set(value) {
                field = value
            }
            get() {
                return field
            }

        var setter: String = "initial5"
            set(value) {
                field = value
            }

        var deferredInit: String

        var getterDeferredInit: String
            get() {
                return field
            }


        var noBackingField: String
            set(value) {
                println(value)
            }
            get() {
                return "initial8"
            }

        init {
            deferredInit = "initial6"
            getterDeferredInit = "initial7"
        }

    }

    @Serializable
    class ValPropertiesClass {
        val simple: String = "initial1"
        val simpleInferred = "initial2"

        val getter: String = "initial3"
            get() {
                return field
            }

        val getterInferred = "initial4"
            get() {
                return field
            }

        val deferredInit: String

        val noBackingField: String
            get() {
                return "initial6"
            }

        init {
            deferredInit = "initial5"
        }
    }

    /*

         This class can't be instantiated because it's hard to check val property with deferred init having backing
         field on constructor resolve stage. So synthetic constructor's signature differs from it's body and it always
         throw exception during call.
         In IR back-end this class would not be compiled.
         @Serializable
         class BrokenValPropertiesClass {
             private val getterDeferredInit: String
                 get() {
                     return field
                 }
             init {
                 getterDeferredInit = "initial6"
             }
         }
     */



    private class CommonStringDecoder(private val elementCount: Int) : AbstractDecoder() {
        override val serializersModule: SerializersModule = EmptySerializersModule
        private var elementIndex = 0

        override fun decodeString(): String {
            return "decoded$elementIndex"
        }

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            if (elementIndex == elementCount) return CompositeDecoder.DECODE_DONE
            return elementIndex++
        }

        override fun decodeSequentially(): Boolean = false
    }


    @Test
    fun testVarProperties() {
        val varPropertiesClass = VarPropertiesClass.serializer().deserialize(CommonStringDecoder(7))

        assertEquals("decoded1", varPropertiesClass.simple)
        assertEquals("decoded2", varPropertiesClass.simpleInferred)
        assertEquals("decoded3", varPropertiesClass.getterSetter)
        assertEquals("decoded4", varPropertiesClass.getterSetterInferred)
        assertEquals("decoded5", varPropertiesClass.setter)

        // properties with deferred init always has value from init block - deserialize value ignored
        assertEquals("initial6", varPropertiesClass.deferredInit)
        assertEquals("initial7", varPropertiesClass.getterDeferredInit)
        assertEquals("initial8", varPropertiesClass.noBackingField)
    }

    @Test
    fun testValProperties() {
        val valPropertiesClass = ValPropertiesClass.serializer().deserialize(CommonStringDecoder(5))

        assertEquals("decoded1", valPropertiesClass.simple)
        assertEquals("decoded2", valPropertiesClass.simpleInferred)
        assertEquals("decoded3", valPropertiesClass.getter)
        assertEquals("decoded4", valPropertiesClass.getterInferred)

        // properties with deferred init always has value from init block - deserialize value ignored
        assertEquals("initial5", valPropertiesClass.deferredInit)
        assertEquals("initial6", valPropertiesClass.noBackingField)
    }
}
