package utils

import java.io.Reader

// Very simple char-by-char parser
class Parser(private val inp: Reader) {
    var cur: Int = inp.read()

    fun next() {
        cur = inp.read()
    }

    fun skipWhitespace(vararg c: Char) {
        while (cur >= 0 && (cur.toChar().isWhitespace() || cur.toChar() in c))
            next()
    }

    fun expect(c: Char) {
        check(cur == c.toInt()) { "Expected '$c'" }
        next()
    }

    fun expectAfterWhiteSpace(c: Char) {
        skipWhitespace()
        expect(c)
    }

    fun nextUntil(vararg c: Char): String {
        val sb = StringBuilder()
        while (cur >= 0 && cur.toChar() !in c) {
            sb.append(cur.toChar())
            next()
        }
        return sb.toString()
    }
}