/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.test.*
import kotlin.random.*
import kotlin.test.*

class JsonUnicodeTest : JsonTestBase() {

    @Serializable
    data class UnicodeKeys(
        @SerialName("\uD83E\uDD14") val thinking: String,
        @SerialName("🤔?") val thinking2: String,
        @SerialName("\uD83E\uDD15") val bandage: String,
        @SerialName("\"") val escaped: String
    )

    @Test
    fun testUnicodeKeys() {
        val data = UnicodeKeys("1", "2", "3", "4")
        val s = """{"\uD83E\uDD14":"1","\uD83E\uDD14?":"2","\uD83E\uDD15":"3","\"":"4"}"""
        assertEquals(data, Json.decodeFromString(s))
    }

    @Test
    fun testUnicodeValues() {
        val data = UnicodeKeys(
            "\uD83E\uDD14", "\" \uD83E\uDD14", "\uD83E\uDD14",
            "slow-path-in-\"-the-middle\""
        )
        assertEquals(data, Json.decodeFromString(Json.encodeToString(data)))
    }

    @Serializable
    data class Wrapper(val s: String)

    @Test
    fun testLongEscapeSequence() {
        assertSerializedAndRestored(Wrapper("\"".repeat(100)), Wrapper.serializer())
        // #1456
        assertSerializedAndRestored(
            Wrapper("{\"status\":123,\"message\":\"content\",\"path\":\"/here/beeeeeeeeeeee/dragoons/d63574f-705c-49dd-a6bc-c8d1e524eefd/\"}"),
            Wrapper.serializer()
        )
        // #1460
        assertSerializedAndRestored(
            """{"avatar_url":"https://cdn.discordapp.com/avatars/384333349063491584/8adca1bddf8c5c46c7deed3edbd80d60.png","embeds":[{"color":1741274,"author":{"icon_url":"https://pbs.twimg.com/profile_images/1381321181719109633/4bpPMaer_normal.jpg","name":"Merlijn replied:","url":"https://twitter.com/@PixelHamster/status/1390719238155952129"},"description":"[@shroomizu](https://twitter.com/shroomizu) time for a pro controller","type":"rich","timestamp":"2021-05-07T17:24:39Z"}],"username":"Merijn"}""",
            String.serializer()
        )

        assertSerializedAndRestored(
                Wrapper("null抢\u000e鋽윝䑜厼\uF70A紲ᢨ䣠null⛾䉻嘖緝ᯧnull쎶\u0005null\u0013\uECC9null藜null㴦铰\\bnull\\f똆\u0010蕧⛺null\u0014毣檚牅䈏nullnullnullnullnullￅ\uF7E1쒪null퓈nullnullnullnullnull?null\uF7EA釸팔null蒒\uF840\u0014\u001c\u000f彏\u000fnull㩻ㅃ\u0005\u001fnull풸\u0011\u0FBDnullnullnull\u0006ᡗ撺\u000enullnullnullnull\u2DBF\uED3C굃nullۃnull䨻醙䗰?\uE5EF\uE656null\uE819?\u0017null㈰nullࣝȰ\uF485\u0017null浣䃛搞nullnull?ⴃ뎝null튫nullהꀠnullﲪnull\u000b\u000enull\u001d猶ᄐnullnullnull鎡null⤏null睏뫌ꛖ\\\"nullnull芘䭠\u0006\u0005ቲ慔\uE8A8ٱ琹?nullnullnull\\t\u0EFA\uEDC9괙?\u0007⠍\u001c\uF5D9랴nullnull\\t묽null甄\uED64䥀null\u0007婍\\b\u0002\\\\null臍nullnullnullnull巓\u000f剛\u0004滴\u0010nullnullnullnullnull烌\u0006nullnull풜null\uEF0F\u001b\u0005null鱽劝nullnull닙nullnullnullnullnullnull⧝\u000bnullnull↥\\t杽鵀nullnull嘡nullnullꑁ\u0007뀨nullnullꑜ䇓亶ｖ鶣\uE4C8\\bnull퇾nullnullޕnull⻗፷null\u001dnullnullnullnull\\b⢅놫賗ᯱ\uF44A\u0016\uF8B7扈\\fnullnull妯nullnullพnull쨆null⤥슅\u0006㥹\u0016null滀null㼻\u0007null搽뿙툛單賎猘nullnull\u0005춎null兀nullnull诃䅤nullnull鵺쒢ꦀnull諟\u001c훕nullጐnull\u000bƒ볤ː老\u001enull抅ǩౄ켎null吿null\uFDCA\u000b૧奵nullﶼ㣘null\u001c\uF085nullꉫ腈null뛅⚼갩nullnullnullꕂ䦾돶\u001e\u2EFF\u0010\uE67Cnull\u0000\u0005null\\\\null䣉\u001b\u0017\u0000\u001f萵\u0007Ꞃ驿\u0005ḁnull臅\u0007nullnullnull\u0003킍Շ矙\uEB60複null\uF5E1null햗灶null\\r\u0002\uE76Fꢬnullϗ뺤⫷nullnull⾿null苵nullnull퉰\u000f\\b摮null\u0019利\\b䷈ꡎ蒏null븘\uF07E\u0015\u0017nullnull䔐\u000f\u001a\\t꣪nullླྀ?\\bnullnullnull묣null鎱熅null\\n鳜鱽黦ꘜ뱈\u0EDAnullnullnullnullnullnull뫾바\u0007꩕푊\u001cnullnull套\u0017null\u0004샂null饘null\uF2EB툛nullnullnull옉\uE2B7null\uED6Fnull\u0004罟null\u0011˟\\f荲null팩\uFD4Anull⥴\uEA9Enullnull⡎搻狯nullnullӋnull翅ભ\u0007䏟null\uF043\u0011null跩nullnull倿\u09BB賘null\uE514⾙nullꭜѲం\\\"⎾null땈\uF36E\u0007\uE6D9null婣㭁null\uE570nullnull傢nullnull\uF6DB慞嘼null\u187B㺯⍹ᘃ㝛年쇡null讬枏䠜昅᾽null檳\u0010\u0007\\t֊壑ﵐ詇nullnull\u001e\uF3D7\\r妗뙇null퐁null\u0003㖣뢠ᮉ䏁null蘜\u000e\u0006null졎nullnull?庪null䞺黩null\u000e뎤null\u0013null\u001d윸\u20F6ﾯ\uF57B\\\"猬null\u001bnullnullnullnull抙\uEAC3ꢨ\u001b\uF0C6\u0002\uF41Enull\u0014\uF3C5訇깤吀匭?퐺\u000eꩦnull\u0004\u0013恧null眜null\u0015飥nullసnullⅆnullnull\u0002nullnull\uE442\uF2A0闛null渜null㊄\u0001≧긷null螥\\\\nullቹčnull\u0018null箱nullnull端\uF7B5⋒䝂\u001c饆抋\u001eᐳ\\r공nullnull\u1739null쨒ꭇnull\\f\u0003\u0018null햖㈙\u001enullꃿnullꃦ\u0000null᧡뚦\uAACEnullnullnull\u0019null\u061C㞮췦\u000e൝null\u0014\u0015null揰null\u0018null禟斛♷\uEAB3nullnull\uE82D罁ꆟnull\\fnullnullլ癱㗋䢵?\u0015\u0005㪙췗null\u0006帡\u0013倫ﴚnull⫣\u0000鲉null\u001dnull\u0010쓸릌null\u0005⨓null疰\u000b偒nullnull\uEAA4ꕸnull塯䩡쀍null?nullnullnull●null\u0000\\f\uE3EB悔榴촓䉁椙null䓖null\u0005null鉌nullnullnull⦙null\uEA21null\u0011\u001enull\u001enull偲\u001cnullnull좩nullᚫ꛱null원꒒null䨉㚁⋎null\u0002镅\uEF30댵\\fnull촴뺴됶肎null鯑詺\uF618nullnull녽null眴nullnull郱ᘘ斮궡nullnull뛋⋎榩?딡nullnull?\u0014蘉\uF2E1null\\bnullnull추null\u0005㉁ꤙꇈ姱null㪹\u0002㍈nullnullnull\u001f㇇\u0017蒷墛nullɩ\\\"null\u000f⬆襤nullnull┭ഀ溜ࢳ❧亠null컈\u0019\\tnullnullﳎnull\u0007null\\t⧘\u0014nullﲘnull끣nullᯜ㧭푬쓉null\u001anull〪䣩䃂ﺤ찅null恗⏗䅳null⭮nullnull\u0016nullnullnull\uE825郞㬃嶘null\\n꼻\uF08Enull\uE4D7⒙䑮null\uE0E2nullnull䢱null\u0003\uEBFB苚釳Ǧ\u0012\u000b\u0011nullnull\\bnullഏ?\u000fﵕ鋨짰\u0001null憞\u001b๘null祆\u001cnull॒꺰\u0010ѻnull?\u000b˻갇null㍿\uE63C㖝\u0016匃\\n\u0001null\\rnullnullᅋnull\uE365↾抬ꠁnull㾁ᕚ헕챱\u0002\uEDB5\u0010null凵\u1ADA༽Ꙋ긚nullᵪnull\\bnullﾳ퍶null얄팼ަᔄ\uF120nullᬍ\\r⍭null퀉蟸焋\\fnull⮏nullnull?\u0012\u0017갞?Ꮵnull┌\\\"\uE803nullnullnullɂ财⎱null엏null娮nullnullnull꛰null吣\u0019ヷ맾ᣝ\uE766ﰟnullnullnullnull祐null\u0000null\u0C84뽉툑null 폍윲\u001d愼ᱳnullꁮnull㕃null가null㗱nullצnull\u0001null춊\u001fힾ\u001a\u0016┒옎璻电絒\u0015\u001bၑnull\u0006\u0006\u0002㳟nullnullԈnull\u1F5Anullnullnull\u000fnullnullnullЧ曗კ\uF5A8null錦Ӣnull\u000b"),
                Wrapper.serializer()
        )
    }

    @Test
    fun testRandomEscapeSequences() = noJs { // Too slow on JS
        repeat(10_000) {
            val s = generateRandomUnicodeString(Random.nextInt(1, 2047))
            try {
                assertSerializedAndRestored(s, String.serializer())
            } catch (e: Throwable) {
                // Not assertion error to preserve cause
                throw IllegalStateException("Unexpectedly failed test, cause string: $s", e)
            }
        }
    }
}
