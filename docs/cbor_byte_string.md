Per [RFC 7049] [2.1 Major Types], CBOR supports the following data types:

- Major type 0: an unsigned integer
- Major type 1: a negative integer
- **Major type 2: a byte string**
- Major type 3: a text string
- **Major type 4: an array of data items**
- Major type 5: a map of pairs of data items
- Major type 6: optional semantic tagging of other major types
- Major type 7: floating-point numbers and simple data types that need no content, as well as the "break" stop code

By default, `ByteArray`s are encoded/decoded as **major type 4**.
When **major type 2** is desired, then the `@ByteString` annotation can be used.

For example:

```kotlin
@Serializable
data class Data(
    @ByteString
    val a: ByteArray, // CBOR Major type 2

    val b: ByteArray  // CBOR Major type 4
)
```


[RFC 7049]: https://tools.ietf.org/html/rfc7049
[2.1 Major Types]: https://tools.ietf.org/html/rfc7049#section-2.1
