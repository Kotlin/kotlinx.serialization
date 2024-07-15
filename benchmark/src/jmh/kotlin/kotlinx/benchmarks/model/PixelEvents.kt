/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package benchmarks.model // NOT A KOTLINX PACKAGE. Otherwise Moshi starts complaining on platform classes

import kotlinx.serialization.*
import kotlinx.serialization.json.*

// For Jackson/Moshi data


@Serializable
data class DefaultPixelEvent(
    val version: Int,
    val dateTime2: String,
    val serverName: String,
    val domain: String,
    val method: String,
    val clientIp: String,
    val queryString: String,
    val userAgent: String,
    val contentType: String,
    val browserLanguage: String,
    val postData: String,
    val cookies: String
)

val pixelEvent = DefaultPixelEvent(
    version = 1,
    dateTime2 = System.currentTimeMillis().toString(),
    serverName = "some-endpoint-qwer",
    domain = "some.domain.com",
    method = "POST",
    clientIp = "127.0.0.1",
    queryString = "anxa=CASCative&anxv=13.901.16.34566&anxe=FoolbarActive&anxt=E7AFBF15-1761-4343-92C1-78167ED19B1C&anxtv=13.901.16.34566&anxp=%5ECQ6%5Expt292%5ES33656%5Eus&anxsi&anxd=2019-10-08T17%3A03%3A57.246Z&f=00400000&anxr=1571945992297&coid=66abafd0d49f42e58dc7536109395306&userSegment&cwsid=opgkcnbminncdgghighmimmphiooeohh",
    userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.14; rv:70.0) Gecko/20100101 Firefox/70.0",
    contentType = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
    browserLanguage = "en-US,en;q=0.5",
    postData = "-",
    cookies = "_ga=GA1.2.971852807.1546968515"
)

val pixelEventJson = Json.encodeToString(pixelEvent)

val pixelEventWithEscapes = DefaultPixelEvent(
    version = 1,
    dateTime2 = System.currentTimeMillis().toString(),
    serverName = "some-endp\"oint-qwer",
    domain = "<a href=\"some.domain.com\">",
    method = "POST",
    clientIp = "127.0.0.1",
    queryString = "anxa=CASCative&anxv=13.901.16.34566&anxe=\"FoolbarActive\"&anxt=E7AFBF15-1761-4343-92C1-78167ED19B1C&anxtv=13.901.16.34566&anxp=%5ECQ6%5Expt292%5ES33656%5Eus&anxsi&anxd=2019-10-08T17%3A03%3A57.246Z&f=00400000&anxr=1571945992297&coid=\"66abafd0d49f42e58dc7536109395306\"&userSegment&cwsid=opgkcnbminncdgghighmimmphiooeohh",
    userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.14; rv:70.0) Gecko/20100101 Firefox/70.0",
    contentType = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
    browserLanguage = "\"en\"-\"US\",en;\\q=0.5",
    postData = "-",
    cookies = "_ga=GA1.2.971852807.1546968515"
)
