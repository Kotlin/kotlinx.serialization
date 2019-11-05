/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.benchmarks.readall

import java.nio.file.*

fun generateClass(fields: Int): String = buildString {
    appendln("package kotlinx.benchmarks.fields;")
    append(
        """
import kotlin.jvm.internal.Intrinsics;
import kotlinx.serialization.*;
    """.trimIndent()
    )

    appendln("\npublic class Fields$fields {")
    for (i in 1..fields) {
        appendln("int i$i;")
    }

    append("public Fields$fields(int mask, ")
    appendln((1..fields).toList().joinToString(separator = ",", postfix = ") {") { "int i$it" })
    for (i in 1..fields) {
        appendln("this.i$i = i$i;")
    }
    appendln("}")

    appendln(
        """
          public static Fields$fields deserialize(Decoder decoder) {
        Intrinsics.checkParameterIsNotNull(decoder, "decoder");
        SerialDescriptor var2 = null;
        boolean readAll = false;
        int mask = 0;
        CompositeDecoder composite = decoder.beginStructure(var2, new KSerializer[0]);
    """.trimIndent()
    )

    for (i in 1..fields) {
        appendln("int i$i = 0;")
    }

    appendln(
        """
        while (true) {
            int idx = composite.decodeElementIndex(var2);
            switch (idx) {
                case -2:
                    readAll = true;
        
    """.trimIndent()
    )

    for (i in 0 until fields) {
        val pow = 2 shl (i - 1)
        appendln(
            """
            case $i:
                i${i + 1} = composite.decodeIntElement(var2, $i);
                mask |= $pow;
                if (!readAll) {
                    break;
                }
        """.trimIndent()
        )
    }

    appendln(
        """
            case -1:
                    composite.endStructure(var2);
                    return new Fields$fields(mask, ${(1..fields).toList().joinToString(separator = ",") { "i$it" }});
                default:
                    throw new RuntimeException();
            }
        }
    """.trimIndent()
    )
    appendln("}}")
}


fun generateClassNew(fields: Int): String = buildString {
    val clzName = "Fields${fields}New"
    appendln("package kotlinx.benchmarks.fields;")
    append(
        """
import kotlin.jvm.internal.Intrinsics;
import kotlinx.serialization.*;
    """.trimIndent()
    )

    appendln("\npublic class $clzName {")
    for (i in 1..fields) {
        appendln("int i$i;")
    }

    append("public $clzName(int mask, ")
    appendln((1..fields).toList().joinToString(separator = ",", postfix = ") {") { "int i$it" })
    for (i in 1..fields) {
        appendln("this.i$i = i$i;")
    }
    appendln("}")

    appendln(
        """
        public static $clzName deserialize(Decoder decoder) {
        Intrinsics.checkParameterIsNotNull(decoder, "decoder");
        SerialDescriptor var2 = null;
        CompositeDecoder composite = decoder.beginStructure(var2, new KSerializer[0]);
    """.trimIndent()
    )

    for (i in 1..fields) {
        appendln("int i$i = 0;")
    }

    appendln("if (composite.readAll()) {")
    for (i in 1..fields) {
        appendln("i$i = composite.decodeIntElement(var2, $i);")
    }
    appendln("""
        composite.endStructure(var2);
        return new $clzName(Integer.MAX_VALUE, ${(1..fields).toList().joinToString(separator = ",") { "i$it" }});
        }
    """.trimIndent())
    appendln("""
        else {
        int mask = 0;
         while (true) {
            int idx = composite.decodeElementIndex(var2);
            switch (idx) {
    """.trimIndent())
    for (i in 0 until fields) {
        val pow = 2 shl (i - 1)
        appendln(
            """
            case $i:
                i${i + 1} = composite.decodeIntElement(var2, $i);
                mask |= $pow;
            break;
        """.trimIndent()
        )
    }

    appendln(
        """
            case -1:
                    composite.endStructure(var2);
                    return new $clzName(mask, ${(1..fields).toList().joinToString(separator = ",") { "i$it" }});
                default:
                    throw new RuntimeException();
            }
        }
    """.trimIndent()
    )
    appendln("}}}")
}

fun writeClass(fields: Int) {
    val p =
        Paths.get("/Users/qwwdfsad/workspace/kotlinx.serialization/benchmark/src/jmh/java/kotlinx/benchmarks/fields/Fields$fields.java")
    Files.write(p, listOf(generateClass(fields)))
}

fun writeClassNew(fields: Int) {
    val p =
        Paths.get("/Users/qwwdfsad/workspace/kotlinx.serialization/benchmark/src/jmh/java/kotlinx/benchmarks/fields/Fields${fields}New.java")
    Files.write(p, listOf(generateClassNew(fields)))
}

fun main() {
    for (i in 1..31) {
        writeClass(i)
        writeClassNew(i)
    }
}