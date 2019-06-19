
v0.11.1 / 2019-06-19
==================

  * Fixed some bugs in compiler plugin for Native (#472, #478) (Kotlin 1.3.40 required)
  * Remove dependency on stdlib-jvm from common source set (Fixes #481)
  * Fix @UseSerializers argument type and clarify some docs
  * Support primitives (ints, strings, JsonLiterals, JsonNull, etc) on a top-level when saving/restoring JSON AST (#451)
  * Migrate to the new (Kotlin 1.3) MPP model
  * Add @SharedImmutable to default json module. Fixes #441 and #446

v0.11.0 / 2019-04-12
====================

#### Plugin:

  * **Semantic change**: Now properties with default values are @Optional by default, and properties without backing fields are @Transient by default.
  * Allow '@Serializable' on a type usage (fixes #367)
  * Auto-applying @Polymorphic for interfaces and serializable abstract classes
  * Do not enable PolymorphicSerializer without special annotation
  * Fix missing optionality of property when generating descriptor in Native
  * Fix impossibility to make @Optional field in a class hierarchy on JS
  * Add synthetic companion with .serializer() getter even if default serializer is overridden. (fixes #228)
  * Ban primitive arrays in JVM codegen too (fixes #260)
  * Don't generate writeSelf/internal constructor if corresponding serialize/deserialize aren't auto-generated
  * Support Serializable class hierarchies on Native and JS
  * Replace @Optional with @Required
  * Support classes with more than 32 serializable properties (fixes #164)
  * Make enums and interfaces not serializable internally. However, they still can be serialized using custom companion object. Fixes #138 and #304

#### Runtime:
  * Introduce JsonBuilder and JsonConfiguration as a better mechanism for configuring and changing configuration of the JSON
  * Implement polymorphic serialization in JSON using class discriminator key
  * Force quoting for map keys (fixes #379)
  * Fix bug with endianness in Native for Longs/Doubles
  * Do not allow to mutate SerialModule in formats
  * Implement multiplatform (JVM, JS and Native) PolymorphicSerializer
  * Remove obsolete and poorly designed global class cache. Remove JVM-only PolymorphicSerializer
  * Replace old SerialModule with new one which: - Can not be installed, should be passed in format constructor - Has polymorphic resolve and contextual resolve - Has DSL for creation - Immutable, but can be combined or overwritten
  * Improve error message for unknown enum constant
  * Deprecate @Optional, introduce @Required
  * Use long instead of int in JsonLiteralSerializer
  * Json and protobuf schemas recording prototype
  * Change JsonObject so it would comply to a Map interface: .get should return null for a missing key Incompatibility with standard Map contract may bring a lot of problems, e.g. broken equals.
  * Make JsonElementSerializer public

0.10.0 / 2019-01-22
==================

  * Migrate to Gradle 4.10 and metadata 0.4
  * Update to 1.3.20
  * Reorder Json parameter for consistency
  * Make JsonElement.toString() consistent with stringify (#325)
  * Reader.read(): Int should return -1 on EOF.
  * Optimize the Writer.write(String) case.
  * Update the docs with new annotations

0.10.0-eap-1 / 2018-12-19
==================

#### Plugin:

  * Support @SerialInfo annotation for Native
  * Remove redundant check for 'all parameters are properties' in a case of fully-customized serializer.
  * Fix unresolved symbol to SerialDescriptor in KSerializer if it was referenced from user custom serializer code (#290)
  * Support for @UseSerializers annotation
  * Restrict auto-implementing serializers methods to certain type of classes
  * Increase priority of overridden serializer on type (#252)
  * Fix instantiation of generic serializers on JS (#244)
  * .shouldEncodeElementDefault for JVM (#58)
  * Support skipping values equals to defaults in output stream for JS and Native backends (#58)
  * Support enums in Native
  * Support reference array and context serializers in Native
  * Fix order of overriding @Serializable(with) on property: check override, than @ContextualSerialization.
  * Support @Transient properties initializers and init blocks in Native
  * Better lookup for `serializer()` function in companion for generic classes because user can define a parameterless shorthand one (#228)
  * Generics serialization in Native
  * .getElementDescriptor for JVM, JS and Native
  * Respect @ContextualSerialization on file
  * Remove auto-applying ContextSerializer. @ContextualSerialization should be used instead.

#### Runtime:

  * Turn around messed endianness names (#308)
  * Update to Kotlin 1.3.20 EAP 2
  * Get rid of protobuf-platform functions since @SerialInfo annotations are supported now. Auto-assign ids starting with 1 because 0 is not a valid protobuf ID.
  * Delegates `equals`, `hashCode` of `JsonObject` and `JsonArray`.
  * Test for fixed #190 in plugin
  * UseSerializers annotation
  * Introduce LongAsStringSerializer
  * Add validation for parsing dynamic to Long Fixes #274
  * Merge pull request #294 from Kotlin/recursive_custom_parsing
  * Fix recursive serialization for JsonOutputs/Inputs
  * Production-ready JSON API
  * Remove ValueTransformer
  * Json improvements
  * @Serializable support for JsonArray
  * @Serializable support for JsonObject
  * @Serializable support for JsonNull and JsonPrimitive
  * Hide JsonTreeParser, provide Json.parseJson as replacement, implement basic JsonElementSerializer.deserialize
  * Migrate the rest of the test on JsonTestBase, implement nullable result in tree json
  * Implement custom serializers support for TreeJsonInput
  * Implement JsonArray serialization
  * Implement strict mode for double in TreeJsonOutput (fixes JsonModesTest)
  * Introduce JsonTestBase in order to ensure streaming and tree json compatibility, transient and strict support in TreeJsonInput
  * Make JsonElement serializable via common machinery
  * Json rework, consolidate different parsing mechanisms, hide implementation details
  * Polymorphic serializer improvements
  * Renamed identifiers to align with Kotlin's coding conventions. https://kotlinlang.org/docs/reference/coding-conventions.html#naming-rules
  * Changed JSON -> Json and CBOR -> Cbor

v0.9.1 / 2018-11-19
==================

  * Update lib to 0.9.1/Kotlin to 1.3.10
  * Make some clarifications about Gradle plugin DSL and serialization plugin distribution
  * Primitive descriptor with overriden name
  * Add missing shorthands for float and char serializers (Fixes #263)
  * Fix bug where primitive non-string values created by hand and created by parser could be inequal due to a redundant type comparison.
  * Don't look at default serializer too early during reflective lookup (Fixes #250)

v0.9.0 / 2018-10-24
==================

  * Fix bug where `.simpleName` was not available for primitives' KClasses.
  * Improve Mapper: it is now a class (with default instance in Companion) which extends AbstractSerialFormat and therefore have context and proper reflectionless API.
  * Introduce @ImplicitReflectionSerializer for API which involves reflection.
  * Add Boolean.Companion.serializer() extension method.
  * Refactor surface API: introduce interfaces for different formats, move some inline functions for serialization start to extensions. As a minor change, now nulls can be serialized at top-level, where it is supported by the format.
  * Add AbstractSerialFormat as a base class to all major formats
  * Update general readme and versions: Library to 0.9, K/N to 1.0 beta
  * Update documentation for the new API
  * Updated info about eap13 releases

v0.8.3-rc13 / 2018-10-19
==================

  * Set default byte order to BigEndian (to be more platform-independent and get rid of posix.BYTE_ORDER dependency)
  * Update Kotlin version to 1.3-RC4
  * Remove Gradle metadata from non-native modules
  * Add missing targets (Fixes #232)
  * Add license, developer and scm information in Maven pom in publication (Fixes #239)
  * Add builder for JsonArray
  * Redesign and unify exceptions from parsers (Fixes #238)
  * Move json parser back to monolith module (drops `jsonparser` artifact)
  * Little improvement of error messages
  > Not working until plugin is updated:
  * Initial support for skipping defaults: JSON
  * Replace choicesNames to Array to be easily instantiated from generated IR

v0.8.2-rc13 / 2018-10-03
========================

  * Update to RC-3
  * Add @SharedImmutable from K/N to some global declarations in JSON parser, so it is now accessible from multiple workers (Fixes #225)
  > Not working until plugin is updated:
  * Tests for generic descriptors
  * Generated serializer and stuff for providing descriptors from plugin
  * Tests on @ContextualSerialization on file

v0.8.1-rc13 / 2018-09-24
========================

  * Upgrade Kotlin/Native version

v0.8.0-rc13 / 2018-09-19
========================

  * Add (currently) no-op annotations to the kibrary for smoother migration
  * Update migration guide and versions to RCs.
  * Support WildcardType in serializerByTypeToken (#212)
  > Not working until plugin is updated:
  * Added experimental support of reference arrays for Native

v0.7.3-eap-13 / 2018-09-18
==========================

  * New enum serializing model
  * New context: SerialModules draft. Renaming and mutable/immutable hierarchy
  * Remove untyped encoding
  * Improve serializers resolving by adding primitive serializers. Also add some helper methods to JSON to serialize lists without pain
  * Fix protobuf by adapting MapLikeSerializer to HashSetSerializer(MapEntrySerializer). Elements' serializers in collection serializers are now accessible for such adaptions.
  * Prohibit NaN and infinite values in JSON strict mode
  * Cleanup JSON, reflect opt-in strict mode in naming
  * Get rid of StructureKind.SET and StructureKind.ENTRY
  * Remove SIZE_INDEX
  * Remove inheritance from Encoder and CompositeEncoder
  * Working over primitive kinds and enums
  * Reworked SerialDescriptor and kinds
  * Renaming of ElementValue* and Tagged*
  * Renaming: KOutput -> Encoder/CompositeEncoder KInput -> Decoder/CompositeDecoder
  * Renaming: KSerialClassDesc -> SerialDescriptor SerialSaver, SerialLoader -> *Strategy
  > Not working until plugin is updated:
  * Provide limited `equals` on collections' descriptors
  * Support for `isElementOptional`

v0.6.2 / 2018-09-12
===================

  * Updated Kotlin to 1.2.70 and Kotlin/Native to 0.9

v0.6.1 / 2018-08-06
===================

  * Compatibility release for 1.2.60
  * Don't throw NoSuchElement if key is missing in the map in `Mapper.readNotNullMark`,
  because tag can be only prefix for nested object. Fixes #182
  * Update ios sample with latest http client

v0.6.0 / 2018-07-13
===================

  Plugin:

  * Allow @SerialName and @SerialInfo on classes
  * Fix resolving serializers for classes from other modules (#153 and #166)

  Runtime:

  * Use new 0.8 K/N DSL
  * Simplify JSON AST API, Provide JSON builder, provide useful extensions, add documentation, update K/N
  * Get rid of JsonString to align json primitives with each other. Provide JSON AST pojo parser which exposes current design issues
  * [JSON-AST] Introduce non-nullable methods throwing exceptions for getting json elements
  * [JSON-AST] Add ability to parse JSONInput element as tree. Symmetric functionality for JsonOutput + JsonTree
  * [JSON-AST] Docs writeup
  * [JSON-AST] Publishing native artifact on bintray
  * [JSON-AST] Saving AST back to JSON
  * [JSON-AST] JsonAstMapper to serializable classes
  * Remove annoying "for class class" message in not found serializer exception
  * Introduce module for benchmarks
  * Add notes about snapshot versions
  * Tests for bugs fixed in latest published plugin (#118 and #125)
  * Auto-assign proto ids using field index

v0.5.1 / 2018-06-13
===================

  Plugin:

  * Fix 1.2.50 compatibility
  * Workaround for recursive resolve on @Serializable(with) and @Serializer(for) pair annotations
  * Don't generate additional constructor if @SerialInfo has no properties
  * Fix order of resolving serializers: user-overriden should go before polymorphic and default
  * While creating descriptors, add type arguments not from serializable class definition but from actual KSerializer implementation. This provides better support for user-defined or external generic serializers
  * Don't generate constructor for passing generic serializers if user already defined proper one.
  * Respect `@Serializable(with)` on properties on JS too.
  * Fix for Kotlin/kotlinx.serialization/136
  * Fix for Kotlin/kotlinx.serialization/125
  * Fix for Kotlin/kotlinx.serialization/118
  * Fix for Kotlin/kotlinx.serialization/123: resolve annotation parameters in-place

  Runtime:

  * Added some shorthands for standard serializers
  * Fix for bug #141 that uses an extra boolean to determine whether to write a separating comma rather than assuming that the element with the index 0 is written first(or at all) in all cases.
  * Move mode cache to output class to make .stringify stateless and thread-safe (#139)
  * Bugfix #95: Can't locate default serializer for classes with named co… (#130)
  * Updated versions in docs and examples Add changelog

v0.5.0 / 2018-04-26
===================

  * Improve buildscript and bumped kotlin version to 1.2.40
  * Remove code warnings
  * Add note about different IDEA plugin versions
  * Add null check to Companion when looking up serializer.
  * Improved performance of JSON.stringify
  * Improved performance of JSON.parse
  * Added compatibility note
  * Fix #107 and #112. #76 awaits next compiler release.

v0.4.2 / 2018-03-07
===================

  * Update runtime library version to match plugin version. Update examples to use latest version of compiler, plugin and runtime. Update Gradle to run on build agents with Java 9.
  * Fix ProGuard rules docs for serialization of classes with generic types
  * Fix ProGuard rules docs for serialization 0.4.1 version
  * Add support for @Serializable classes that are private and live out of kotlinx.serialization package. In such case the Companion field is not visible and must be set accessible before use.
  * update jvm-example to latest versions
