package com.mycompany

import com.mycompany.model.MyData
import kotlinx.serialization.json.JSON
import org.junit.Assert.assertEquals
import org.junit.Test

class Tests {
    @Test
    fun testModel() {
        val data = MyData(42)
        val json = JSON.stringify(MyData.serializer, data)
        assertEquals("""{"x":42,"y":"foo","intList":[1,2,3]}""", json)
    }
}
