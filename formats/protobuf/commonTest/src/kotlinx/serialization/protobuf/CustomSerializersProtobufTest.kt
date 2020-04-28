/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.*
import kotlinx.serialization.modules.*
import kotlin.test.*

@OptIn(ImplicitReflectionSerializer::class)
class CustomSerializersProtobufTest {

    private fun protoBufWithB() =
        ProtoBuf(context = serializersModuleOf(B::class, BSerializer))

    @Test
    fun writeCustom() {
        val a = A(B(2))
        val j = protoBufWithB()
        val s = j.dumps(a).toUpperCase()
        assertEquals("0802", s)
    }

    @Test
    fun readCustom() {
        val a = A(B(2))
        val j = protoBufWithB()
        val s = j.loads<A>("0802")
        assertEquals(a, s)
    }

    @Test
    fun writeCustomList() {
        val obj = BList(listOf(B(1), B(2), B(3)))
        val j = protoBufWithB()
        val s = j.dumps(obj).toUpperCase()
        assertEquals("080108020803", s)
    }

    @Test
    fun readCustomList() {
        val obj = BList(listOf(B(1), B(2), B(3)))
        val j = protoBufWithB()
        val bs = j.loads<BList>("080108020803")
        assertEquals(obj, bs)
    }

    @Test
    fun writeCustomInvertedOrder() {
        val obj = C(1, 2)
        val j = ProtoBuf()
        val s = j.dumps(obj).toUpperCase()
        assertEquals("10020801", s)
    }

    @Test
    fun readCustomInvertedOrder() {
        val obj = C(1, 2)
        val j = ProtoBuf()
        val s = j.loads<C>("10020801")
        assertEquals(obj, s)
    }

    @Test
    fun writeCustomOmitDefault() {
        val obj = C(b = 2)
        val j = ProtoBuf()
        val s = j.dumps(obj).toUpperCase()
        assertEquals("1002", s)
    }

    @Test
    fun readCustomOmitDefault() {
        val obj = C(b = 2)
        val j = ProtoBuf()
        val s = j.loads<C>("1002")
        assertEquals(obj, s)
    }

    @Test
    fun writeOptionalList1() {
        val obj = CList1(listOf(C(a = 1), C(b = 2), C(3, 4)))
        val s = ProtoBuf().dumps(obj).toUpperCase()
        assertEquals("0A04102A08010A0210020A0410040803", s)
    }

    @Test
    fun readOptionalList1() {
        val obj = CList1(listOf(C(a = 1), C(b = 2), C(3, 4)))
        val j = "0A04102A08010A0210020A0410040803"
        assertEquals(obj, ProtoBuf().loads(j))
    }

    @Test
    fun writeOptionalList2a() {
        val obj = CList2(7, listOf(C(a = 5), C(b = 6), C(7, 8)))
        val s = ProtoBuf().dumps(obj).toUpperCase()
        assertEquals("1204102A0805120210061204100808070807", s)
    }

    @Test
    fun readOptionalList2a() {
        val obj = CList2(7, listOf(C(a = 5), C(b = 6), C(7, 8)))
        val j = "08071204102A080512021006120410080807"
        assertEquals(obj, ProtoBuf().loads(j))
    }

    @Test
    fun writeOptionalList2b() {
        val obj = CList2(c = listOf(C(a = 5), C(b = 6), C(7, 8)))
        val s = ProtoBuf().dumps(obj).toUpperCase()
        assertEquals("1204102A080512021006120410080807", s)
    }

    @Test
    fun readOptionalList2b() {
        val obj = CList2(c = listOf(C(a = 5), C(b = 6), C(7, 8)))
        val j = "1204102A080512021006120410080807"
        assertEquals(obj, ProtoBuf().loads(j))
    }

    @Test
    fun writeOptionalList3a() {
        val obj = CList3(listOf(C(a = 1), C(b = 2), C(3, 4)), 99)
        val s = ProtoBuf().dumps(obj).toUpperCase()
        assertEquals("0A04102A08010A0210020A04100408031063", s)
    }

    @Test
    fun readOptionalList3a() {
        val obj = CList3(listOf(C(a = 1), C(b = 2), C(3, 4)), 99)
        val j = "10630A04102A08010A0210020A0410040803"
        assertEquals(obj, ProtoBuf().loads(j))
    }

    @Test
    fun writeOptionalList3b() {
        val obj = CList3(f = 99)
        val s = ProtoBuf().dumps(obj).toUpperCase()
        assertEquals("1063", s)
    }

    @Test
    fun readOptionalList3b() {
        val obj = CList3(f = 99)
        val j = "1063"
        assertEquals(obj, ProtoBuf().loads(j))
    }

    @Test
    fun writeOptionalList4a() {
        val obj = CList4(listOf(C(a = 1), C(b = 2), C(3, 4)), 54)
        val s = ProtoBuf().dumps(obj).toUpperCase()
        assertEquals("10360A04102A08010A0210020A0410040803", s)
    }

    @Test
    fun readOptionalList4a() {
        val obj = CList4(listOf(C(a = 1), C(b = 2), C(3, 4)), 54)
        val j = "10360A04102A08010A0210020A0410040803"
        assertEquals(obj, ProtoBuf().loads(j))
    }

    @Test
    fun writeOptionalList4b() {
        val obj = CList4(h = 97)
        val j = "1061"
        val s = ProtoBuf().dumps(obj).toUpperCase()
        assertEquals(j, s)
    }

    @Test
    fun readOptionalList4b() {
        val obj = CList4(h = 97)
        val j = "1061"
        assertEquals(obj, ProtoBuf().loads(j))
    }

    @Test
    fun writeOptionalList5a() {
        val obj = CList5(listOf(9, 8, 7, 6, 5), 5)
        val s = ProtoBuf().dumps(obj).toUpperCase()
        assertEquals("100508090808080708060805", s)
    }

    @Test
    fun readOptionalList5a() {
        val obj = CList5(listOf(9, 8, 7, 6, 5), 5)
        val j = "100508090808080708060805"
        assertEquals(obj, ProtoBuf().loads(j))
    }

    @Test
    fun writeOptionalList5b() {
        val obj = CList5(h = 999)
        val s = ProtoBuf().dumps(obj).toUpperCase()
        assertEquals("10E707", s)
    }

    @Test
    fun readOptionalList5b() {
        val obj = CList5(h = 999)
        val j = "10E707"
        assertEquals(obj, ProtoBuf().loads(j))
    }
}
