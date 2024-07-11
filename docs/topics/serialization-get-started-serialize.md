[//]: # (title: Serialize an object)

<microformat>
   <p>This is the first part of the <strong>Getting started with Kotlin serialization</strong> tutorial:</p>
   <p><img src="icon-1-done.svg" width="20" alt="First step"/> Add Kotlin serialization plugins and dependencies<br/>
      <img src="icon-2.svg" width="20" alt="Second step"/> <strong>Serialize an object</strong><br/>
      <img src="icon-3-todo.svg" width="20" alt="Third step"/> Add dependencies to a Kotlin Notebook<br/>      
      <img src="icon-4-todo.svg" width="20" alt="Fourth step"/> Share a Kotlin Notebook<br/>
  </p>
</microformat>

1. 

2. Make a class serializable by annotating it with `@Serializable`.

    ```kotlin
    import kotlinx.serialization.Serializable
    
    @Serializable
    data class Data(val a: Int, val b: String)
    ```

3. Serialize an instance of this class by calling `Json.encodeToString()`.

    ```kotlin
    import kotlinx.serialization.Serializable
    import kotlinx.serialization.json.Json
    import kotlinx.serialization.encodeToString
    
    @Serializable
    data class Data(val a: Int, val b: String)
    
    fun main() {
       val json = Json.encodeToString(Data(42, "str"))
    }
    ```

    As a result, you get a string containing the state of this object in the JSON format: `{"a": 42, "b": "str"}`
    
    > You can also serialize object collections, such as lists, in a single call:
    >
    > ```kotlin
    > val dataList = listOf(Data(42, "str"), Data(12, "test"))
    > val jsonList = Json.encodeToString(dataList)
    > ```
    >
    {type="note"}

4. 