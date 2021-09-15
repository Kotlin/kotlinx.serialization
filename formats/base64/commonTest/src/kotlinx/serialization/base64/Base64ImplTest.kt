package kotlinx.serialization.base64

import kotlinx.serialization.base64.impl.decode
import kotlinx.serialization.base64.impl.encode
import kotlin.test.Test
import kotlin.test.assertEquals

class Base64ImplTest {

  @Test
  fun encodeSmokeTests() {
    testEncode("123", "MTIz")
    testEncode("abcdef", "YWJjZGVm")

    testEncode("1", "MQ==")
    testEncode("2", "Mg==")
    testEncode("12", "MTI=")

    testEncode("abcd", "YWJjZA==")
    testEncode("abcde", "YWJjZGU=")

    // RFC's testcases
    testEncode("", "")
    testEncode("f", "Zg==")
    testEncode("fo", "Zm8=")
    testEncode("foo", "Zm9v")
    testEncode("foob", "Zm9vYg==")
    testEncode("fooba", "Zm9vYmE=")
    testEncode("foobar", "Zm9vYmFy")
  }

  @Test
  fun decodeSmokeTests() {
    testDecode("123", "MTIz")
    testDecode("abcdef", "YWJjZGVm")

    testDecode("1", "MQ==")
    testDecode("2", "Mg==")
    testDecode("12", "MTI=")

    testDecode("abcd", "YWJjZA==")
    testDecode("abcde", "YWJjZGU=")

    // RFC
    // RFC's testcases
    testDecode("", "")
    testDecode("f", "Zg==")
    testDecode("fo", "Zm8=")
    testDecode("foo", "Zm9v")
    testDecode("foob", "Zm9vYg==")
    testDecode("fooba", "Zm9vYmE=")
    testDecode("foobar", "Zm9vYmFy")
  }

  private fun testEncode(input: String, expected: String) {
    val result = encode(input.encodeToByteArray())
    assertEquals(expected, result)
  }

  private fun testDecode(expected: String, encoded: String) {
    val result = decode(encoded).decodeToString()
    assertEquals(expected, result)
  }
}