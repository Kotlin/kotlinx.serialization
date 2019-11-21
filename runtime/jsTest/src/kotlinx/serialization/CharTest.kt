/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.json.JsonTestBase
import kotlin.test.Ignore
import kotlin.test.Test

@Serializable
data class SingleChar(val c: Char)

@Serializable
data class NullableChar(val c: Char?)

@Serializable
data class ListOfChars(val c: List<Char>, val cn: List<Char?>)

class CharTest : JsonTestBase() {
    @Test
    fun testSingleChar() = assertJsonFormAndRestored(SingleChar.serializer(), SingleChar('c'), "{c:c}")

    @Test
    @Ignore // TODO [JS IR]
    fun testNullableChar() = assertJsonFormAndRestored(NullableChar.serializer(), NullableChar('c'), "{c:c}")

    @Test
    fun testNullableCharWithNull() =
        assertJsonFormAndRestored(NullableChar.serializer(), NullableChar(null), "{c:null}")

    @Test
    fun testListOfChars() = assertJsonFormAndRestored(
        ListOfChars.serializer(),
        ListOfChars(listOf('a', 'b', 'c'), listOf('c', null)),
        "{c:[a,b,c],cn:[c,null]}"
    )
}
