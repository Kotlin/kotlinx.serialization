package kotlinx.serialization

import org.junit.Test
import kotlin.test.assertFailsWith

class IndexTest {
    class MalformedReader: ElementValueInput() {
        override fun readElement(desc: KSerialClassDesc): Int {
            return UNKNOWN_NAME
        }
    }

    @Test
    fun compilerComplainsAboutIncorrectIndex() {
        assertFailsWith(UnknownFieldException::class) {
            MalformedReader().read<OptionalTests.Data>()
        }
    }
}