# Kotlin Serialization Guide

Kotlin Serialization is a cross-platform and multi-format framework for data serialization&mdash;converting 
trees of objects to strings, byte arrays, or other _serial_ representations and back.
Kotlin Serialization fully supports and enforces the Kotlin type system, making sure only valid 
objects can be deserialized. 
 
Kotlin Serialization is not just a library. It is a compiler plugin that is bundled with the Kotlin
compiler distribution itself. Build configuration is explained in [README.md](../README.md#setup). 
Once the project is set up, we can start serializing some classes.  

## Table of contents

**Chapter 1.** [Basic Serialization](basic-serialization.md) (**start reading here**)
<!--- TOC_REF basic-serialization.md -->
* <a name='basics'></a>[Basics](basic-serialization.md#basics)
  * <a name='json-encoding'></a>[JSON encoding](basic-serialization.md#json-encoding)
  * <a name='json-decoding'></a>[JSON decoding](basic-serialization.md#json-decoding)
* <a name='serializable-classes'></a>[Serializable classes](basic-serialization.md#serializable-classes)
  * <a name='backing-fields-are-serialized'></a>[Backing fields are serialized](basic-serialization.md#backing-fields-are-serialized)
  * <a name='constructor-properties-requirement'></a>[Constructor properties requirement](basic-serialization.md#constructor-properties-requirement)
  * <a name='data-validation'></a>[Data validation](basic-serialization.md#data-validation)
  * <a name='optional-properties'></a>[Optional properties](basic-serialization.md#optional-properties)
  * <a name='optional-property-initializer-call'></a>[Optional property initializer call](basic-serialization.md#optional-property-initializer-call)
  * <a name='required-properties'></a>[Required properties](basic-serialization.md#required-properties)
  * <a name='transient-properties'></a>[Transient properties](basic-serialization.md#transient-properties)
  * <a name='defaults-are-not-encoded-by-default'></a>[Defaults are not encoded by default](basic-serialization.md#defaults-are-not-encoded-by-default)
  * <a name='nullable-properties'></a>[Nullable properties](basic-serialization.md#nullable-properties)
  * <a name='type-safety-is-enforced'></a>[Type safety is enforced](basic-serialization.md#type-safety-is-enforced)
  * <a name='referenced-objects'></a>[Referenced objects](basic-serialization.md#referenced-objects)
  * <a name='no-compression-of-repeated-references'></a>[No compression of repeated references](basic-serialization.md#no-compression-of-repeated-references)
  * <a name='generic-classes'></a>[Generic classes](basic-serialization.md#generic-classes)
  * <a name='serial-field-names'></a>[Serial field names](basic-serialization.md#serial-field-names)
<!--- END -->

**Chapter 2.** [Builtin Classes](builtin-classes.md)

<!--- TOC_REF builtin-classes.md -->
* <a name='primitives'></a>[Primitives](builtin-classes.md#primitives)
  * <a name='numbers'></a>[Numbers](builtin-classes.md#numbers)
  * <a name='long-numbers'></a>[Long numbers](builtin-classes.md#long-numbers)
  * <a name='long-numbers-as-strings'></a>[Long numbers as strings](builtin-classes.md#long-numbers-as-strings)
  * <a name='enum-classes'></a>[Enum classes](builtin-classes.md#enum-classes)
  * <a name='serial-names-of-enum-entries'></a>[Serial names of enum entries](builtin-classes.md#serial-names-of-enum-entries)
* <a name='composites'></a>[Composites](builtin-classes.md#composites)
  * <a name='pair-and-triple'></a>[Pair and triple](builtin-classes.md#pair-and-triple)
  * <a name='lists'></a>[Lists](builtin-classes.md#lists)
  * <a name='sets-and-other-collections'></a>[Sets and other collections](builtin-classes.md#sets-and-other-collections)
  * <a name='deserializing-collections'></a>[Deserializing collections](builtin-classes.md#deserializing-collections)
  * <a name='maps'></a>[Maps](builtin-classes.md#maps)
  * <a name='unit-and-singleton-objects'></a>[Unit and singleton objects](builtin-classes.md#unit-and-singleton-objects)
<!--- END -->

**Chapter 3.** [Serializers](serializers.md)

<!--- TOC_REF serializers.md -->
* <a name='introduction-to-serializers'></a>[Introduction to serializers](serializers.md#introduction-to-serializers)
  * <a name='plugin-generated-serializer'></a>[Plugin-generated serializer](serializers.md#plugin-generated-serializer)
  * <a name='plugin-generated-generic-serializer'></a>[Plugin-generated generic serializer](serializers.md#plugin-generated-generic-serializer)
  * <a name='builtin-primitive-serializers'></a>[Builtin primitive serializers](serializers.md#builtin-primitive-serializers)
  * <a name='constructing-collection-serializers'></a>[Constructing collection serializers](serializers.md#constructing-collection-serializers)
  * <a name='using-top-level-serializer-function'></a>[Using top-level serializer function](serializers.md#using-top-level-serializer-function)
* <a name='custom-serializers'></a>[Custom serializers](serializers.md#custom-serializers)
  * <a name='primitive-serializer'></a>[Primitive serializer](serializers.md#primitive-serializer)
  * <a name='delegating-serializers'></a>[Delegating serializers](serializers.md#delegating-serializers)
  * <a name='composite-serializer-via-surrogate'></a>[Composite serializer via surrogate](serializers.md#composite-serializer-via-surrogate)
  * <a name='hand-written-composite-serializer'></a>[Hand-written composite serializer](serializers.md#hand-written-composite-serializer)
  * <a name='sequential-decoding-protocol-experimental'></a>[Sequential decoding protocol (experimental)](serializers.md#sequential-decoding-protocol-experimental)
  * <a name='serializing-3rd-party-classes'></a>[Serializing 3rd party classes](serializers.md#serializing-3rd-party-classes)
  * <a name='passing-a-serializer-manually'></a>[Passing a serializer manually](serializers.md#passing-a-serializer-manually)
  * <a name='specifying-serializer-on-a-property'></a>[Specifying serializer on a property](serializers.md#specifying-serializer-on-a-property)
  * <a name='specifying-serializers-for-a-file'></a>[Specifying serializers for a file](serializers.md#specifying-serializers-for-a-file)
  * <a name='custom-serializers-for-a-generic-type'></a>[Custom serializers for a generic type](serializers.md#custom-serializers-for-a-generic-type)
  * <a name='format-specific-serializers'></a>[Format-specific serializers](serializers.md#format-specific-serializers)
* <a name='contextual-serialization'></a>[Contextual serialization](serializers.md#contextual-serialization)
  * <a name='serializers-module'></a>[Serializers module](serializers.md#serializers-module)
  * <a name='contextual-serialization-and-generic-classes'></a>[Contextual serialization and generic classes](serializers.md#contextual-serialization-and-generic-classes)
* <a name='deriving-external-serializer-for-another-kotlin-class-experimental'></a>[Deriving external serializer for another Kotlin class (experimental)](serializers.md#deriving-external-serializer-for-another-kotlin-class-experimental)
  * <a name='external-serialization-uses-properties'></a>[External serialization uses properties](serializers.md#external-serialization-uses-properties)
<!--- END -->

**Chapter 4.** [Polymorphism](polymorphism.md)

<!--- TOC_REF polymorphism.md -->
* <a name='closed-polymorphism'></a>[Closed polymorphism](polymorphism.md#closed-polymorphism)
  * <a name='static-types'></a>[Static types](polymorphism.md#static-types)
  * <a name='designing-serializable-hierarchy'></a>[Designing serializable hierarchy](polymorphism.md#designing-serializable-hierarchy)
  * <a name='sealed-classes'></a>[Sealed classes](polymorphism.md#sealed-classes)
  * <a name='custom-subclass-serial-name'></a>[Custom subclass serial name](polymorphism.md#custom-subclass-serial-name)
  * <a name='concrete-properties-in-a-base-class'></a>[Concrete properties in a base class](polymorphism.md#concrete-properties-in-a-base-class)
  * <a name='objects'></a>[Objects](polymorphism.md#objects)
* <a name='open-polymorphism'></a>[Open polymorphism](polymorphism.md#open-polymorphism)
  * <a name='registered-subclasses'></a>[Registered subclasses](polymorphism.md#registered-subclasses)
  * <a name='serializing-interfaces'></a>[Serializing interfaces](polymorphism.md#serializing-interfaces)
  * <a name='property-of-an-interface-type'></a>[Property of an interface type](polymorphism.md#property-of-an-interface-type)
  * <a name='static-parent-type-lookup-for-polymorphism'></a>[Static parent type lookup for polymorphism](polymorphism.md#static-parent-type-lookup-for-polymorphism)
  * <a name='explicitly-marking-polymorphic-class-properties'></a>[Explicitly marking polymorphic class properties](polymorphism.md#explicitly-marking-polymorphic-class-properties)
  * <a name='registering-multiple-superclasses'></a>[Registering multiple superclasses](polymorphism.md#registering-multiple-superclasses)
  * <a name='polymorphism-and-generic-classes'></a>[Polymorphism and generic classes](polymorphism.md#polymorphism-and-generic-classes)
  * <a name='merging-library-serializers-modules'></a>[Merging library serializers modules](polymorphism.md#merging-library-serializers-modules)
  * <a name='default-polymorphic-type-handler-for-deserialization'></a>[Default polymorphic type handler for deserialization](polymorphism.md#default-polymorphic-type-handler-for-deserialization)
  * <a name='default-polymorphic-type-handler-for-serialization'></a>[Default polymorphic type handler for serialization](polymorphism.md#default-polymorphic-type-handler-for-serialization)
<!--- END -->

**Chapter 5.** [JSON Features](json.md)

<!--- TOC_REF json.md -->
* <a name='json-configuration'></a>[Json configuration](json.md#json-configuration)
  * <a name='pretty-printing'></a>[Pretty printing](json.md#pretty-printing)
  * <a name='lenient-parsing'></a>[Lenient parsing](json.md#lenient-parsing)
  * <a name='ignoring-unknown-keys'></a>[Ignoring unknown keys](json.md#ignoring-unknown-keys)
  * <a name='alternative-json-names'></a>[Alternative Json names](json.md#alternative-json-names)
  * <a name='coercing-input-values'></a>[Coercing input values](json.md#coercing-input-values)
  * <a name='encoding-defaults'></a>[Encoding defaults](json.md#encoding-defaults)
  * <a name='explicit-nulls'></a>[Explicit nulls](json.md#explicit-nulls)
  * <a name='allowing-structured-map-keys'></a>[Allowing structured map keys](json.md#allowing-structured-map-keys)
  * <a name='allowing-special-floating-point-values'></a>[Allowing special floating-point values](json.md#allowing-special-floating-point-values)
  * <a name='class-discriminator-for-polymorphism'></a>[Class discriminator for polymorphism](json.md#class-discriminator-for-polymorphism)
* <a name='json-elements'></a>[Json elements](json.md#json-elements)
  * <a name='parsing-to-json-element'></a>[Parsing to Json element](json.md#parsing-to-json-element)
  * <a name='types-of-json-elements'></a>[Types of Json elements](json.md#types-of-json-elements)
  * <a name='json-element-builders'></a>[Json element builders](json.md#json-element-builders)
  * <a name='decoding-json-elements'></a>[Decoding Json elements](json.md#decoding-json-elements)
* <a name='json-transformations'></a>[Json transformations](json.md#json-transformations)
  * <a name='array-wrapping'></a>[Array wrapping](json.md#array-wrapping)
  * <a name='array-unwrapping'></a>[Array unwrapping](json.md#array-unwrapping)
  * <a name='manipulating-default-values'></a>[Manipulating default values](json.md#manipulating-default-values)
  * <a name='content-based-polymorphic-deserialization'></a>[Content-based polymorphic deserialization](json.md#content-based-polymorphic-deserialization)
  * <a name='under-the-hood-experimental'></a>[Under the hood (experimental)](json.md#under-the-hood-experimental)
  * <a name='maintaining-custom-json-attributes'></a>[Maintaining custom JSON attributes](json.md#maintaining-custom-json-attributes)
<!--- END -->

**Chapter 6.** [Alternative and custom formats (experimental)](formats.md)

<!--- TOC_REF formats.md -->
* <a name='cbor-experimental'></a>[CBOR (experimental)](formats.md#cbor-experimental)
  * <a name='ignoring-unknown-keys'></a>[Ignoring unknown keys](formats.md#ignoring-unknown-keys)
  * <a name='byte-arrays-and-cbor-data-types'></a>[Byte arrays and CBOR data types](formats.md#byte-arrays-and-cbor-data-types)
* <a name='protobuf-experimental'></a>[ProtoBuf (experimental)](formats.md#protobuf-experimental)
  * <a name='field-numbers'></a>[Field numbers](formats.md#field-numbers)
  * <a name='integer-types'></a>[Integer types](formats.md#integer-types)
  * <a name='lists-as-repeated-fields'></a>[Lists as repeated fields](formats.md#lists-as-repeated-fields)
  * <a name='packed-fields'></a>[Packed fields](formats.md#packed-fields)
  * <a name='protobuf-schema-generator-experimental'></a>[ProtoBuf schema generator (experimental)](formats.md#protobuf-schema-generator-experimental)
* <a name='properties-experimental'></a>[Properties (experimental)](formats.md#properties-experimental)
* <a name='custom-formats-experimental'></a>[Custom formats (experimental)](formats.md#custom-formats-experimental)
  * <a name='basic-encoder'></a>[Basic encoder](formats.md#basic-encoder)
  * <a name='basic-decoder'></a>[Basic decoder](formats.md#basic-decoder)
  * <a name='sequential-decoding'></a>[Sequential decoding](formats.md#sequential-decoding)
  * <a name='adding-collection-support'></a>[Adding collection support](formats.md#adding-collection-support)
  * <a name='adding-null-support'></a>[Adding null support](formats.md#adding-null-support)
  * <a name='efficient-binary-format'></a>[Efficient binary format](formats.md#efficient-binary-format)
  * <a name='format-specific-types'></a>[Format-specific types](formats.md#format-specific-types)
<!--- END -->

**Appendix A.** [Serialization and inline classes (experimental, IR-specific)](inline-classes.md)

<!--- TOC_REF inline-classes.md -->
* <a name='serializable-inline-classes'></a>[Serializable inline classes](inline-classes.md#serializable-inline-classes)
* <a name='unsigned-types-support-json-only'></a>[Unsigned types support (JSON only)](inline-classes.md#unsigned-types-support-json-only)
* <a name='using-inline-classes-in-your-custom-serializers'></a>[Using inline classes in your custom serializers](inline-classes.md#using-inline-classes-in-your-custom-serializers)
<!--- END -->
