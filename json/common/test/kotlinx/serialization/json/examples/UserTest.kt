package kotlinx.serialization.json.examples

import kotlin.test.*

class UserTest {

    @Test
    fun testUser() {
        val user = User(1, "Decard",
            UserAddress("USA", "S.F", 1234, listOf("sheep", "dream")),
            UserProperties(true, 1, 2.0, false))
        testParse(user)
    }

    @Test
    fun testUserWithNulls() {
        testParse(User(1, "Nameless",
            UserAddress("???", null, null, listOf("One")),
            UserProperties(true, 3, 55.0, null)))
    }

    @Test
    fun testUserWithNullObject() {
        testParse(User(1, "Coming up with test data is really hard", null,
            UserProperties(true, 3, 55.0, null)))
    }

    @Test
    fun testUserEmptyArray() {
        testParse(User(1, "", UserAddress("", "", null, emptyList()),
            UserProperties(true, 3, 55.0, true)))
    }

    private fun testParse(user: User) {
        val json = UserParser.write(user).toString()
        val deserialized = UserParser.read(json)
        assertEquals(user, deserialized)
    }
}
