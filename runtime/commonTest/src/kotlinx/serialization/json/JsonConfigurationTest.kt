/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlin.test.*

class JsonConfigurationTest {

    @Test
    fun testPrettyPrint() {
        json(true, "")
        json(true, "\n")
        json(true, "\r")
        json(true, "\t")
        json(true, " ")
        json(true, "    ")
        json(true, " \t\r\n\t   ")
        assertFailsWith<IllegalArgumentException> { json(false, " ") }
        assertFailsWith<IllegalArgumentException> { json(false, " ") }
        assertFailsWith<IllegalArgumentException> { json(true, "f") }
        assertFailsWith<IllegalArgumentException> { json(true, "\tf\n") }
    }

    private fun json(prettyPrint: Boolean, prettyPrintIndent: String) = Json {
        this.prettyPrint = prettyPrint
        this.prettyPrintIndent = prettyPrintIndent
    }
}
