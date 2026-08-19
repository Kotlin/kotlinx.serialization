/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.benchmarks.json.moshi

import benchmarks.model.DefaultPixelEvent
import benchmarks.model.pixelEvent

open class MoshiBaselineNonAscii : MoshiBaseline() {
    override val input: DefaultPixelEvent = pixelEvent.withNonAsciiStrings()
}

private fun DefaultPixelEvent.withNonAsciiStrings(): DefaultPixelEvent = copy(
    dateTime2 = dateTime2.toFullWidth(),
    serverName = serverName.toFullWidth(),
    domain = domain.toFullWidth(),
    method = method.toFullWidth(),
    clientIp = clientIp.toFullWidth(),
    queryString = queryString.toFullWidth(),
    userAgent = userAgent.toFullWidth(),
    contentType = contentType.toFullWidth(),
    browserLanguage = browserLanguage.toFullWidth(),
    postData = postData.toFullWidth(),
    cookies = cookies.toFullWidth(),
)

private fun String.toFullWidth(): String = buildString(length) {
    for (char in this@toFullWidth) {
        append(
            when {
                // Ideographic Space: https://www.compart.com/en/unicode/U+3000
                char == ' ' -> '\u3000'
                // My favourite meme: WIDE https://en.wikipedia.org/wiki/Halfwidth_and_Fullwidth_Forms_(Unicode_block)
                char in '!'..'~' -> (char.code + 0xfee0).toChar()
                // Wide enough
                char.code >= 0x80 -> char
                // MULTIOCULAR O ꙮ. If you are editing this, pick a next funny one!
                else -> '\uA66E'
            }
        )
    }
}
