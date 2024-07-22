[//]: # (title: Serialize polymorphic classes)

Polymorphism allows objects of different types to be treated as objects of a common supertype.
In Kotlin, polymorphism enables you to work with various subclasses through a shared interface or superclass.
This is useful for designing flexible and reusable code, especially when the exact type of objects isn't known at compile time.
In the context of serialization, polymorphism helps manage data structures where the runtime type of data can vary.

## Closed polymorphism

In serialization, closed polymorphism ensures that all possible subclasses are known and handled, providing a clear and
safe way to serialize and deserialize polymorphic data structures.

### Static types

Kotlin Serialization is fully static with respect to types by default. The structure of encoded objects is determined
by *compile-time* types of objects. Let's examine this aspect in more detail and learn how
to serialize polymorphic data structures, where the type of data is determined at runtime.

To show the static nature of Kotlin Serialization let us make the following setup. An `open class Project`
has just the `name` property, while its derived `class OwnedProject` adds an `owner` property.
In the below example, we serialize `data` variable with a static type of
`Project` that is initialized with an instance of `OwnedProject` at runtime.

## Open polymorphism