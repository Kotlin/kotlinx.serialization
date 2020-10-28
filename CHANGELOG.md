1.0.1 / 2020-10-28
==================

This patch release contains several feature improvements as well as bugfixes and performance improvements.

### Features
  * Add object-based serialization and deserialization of polymorphic types for `dynamic` conversions on JS platform  (#1122)
  * Add support for object polymorphism in HOCON decoder (#1136)
  * Add support of decoding map in the root of HOCON config (#1106)
  
### Bugfixes
  * Properly cache generated serializers in PluginGeneratedSerialDescriptor (#1159)
  * Add Pair and Triple to serializer resolving from Java type token (#1160)
  * Fix deserialization of half-precision, float and double types in CBOR  (#1112)
  * Fix ByteString annotation detection when ByteArray is nullable (#1139)
  
1.0.0 / 2020-10-08
==================

The first public stable release, yay!
The definitions of stability and backwards compatibility guarantees are located in the [corresponding document](docs/compatibility.md).
We now also have a GitHub Pages site with [full API reference](https://kotlin.github.io/kotlinx.serialization/).

Compared to RC2, no new features apart from #947 were added and all previously deprecated declarations and migrations were deleted. 
If you are using RC/RC2 along with deprecated declarations, please, migrate before updating to 1.0.0.
In case you are using pre-1.0 versions (e.g. 0.20.0), please refer to our [migration guide](docs/migration.md).
  
### Bugfixes and improvements

  * Support nullable types at top-level for JsonElement decoding (#1117)
  * Add CBOR ignoreUnknownKeys option (#947) (thanks to [Travis Wyatt](https://github.com/twyatt))
  * Fix incorrect documentation of `encodeDefaults` (#1108) (thanks to [Anders Carling](https://github.com/anderscarling))
  
1.0.0-RC2 / 2020-09-21
==================

Second release candidate for 1.0.0 version. This RC contains tweaks and changes based on users feedback after 1.0.0-RC.

### Major changes

* JSON format is now located in different artifact (#994)

In 1.0.0-RC, the `kotlinx-serialization-core` artifact contained core serialization entities as well as `Json` serial format.
We've decided to change that and to make `core` format-agnostic.
It would make the life easier for those who use other serial formats and also make possible to write your own implementation of JSON
or another format without unnecessary dependency on the default one.

In 1.0.0-RC2, `Json` class and related entities are located in `kotlinx-serialization-json` artifact.
To migrate, simply replace `kotlinx-serialization-core` dependency with `-json`. Core library then will be included automatically
as the transitive dependency.

For most use-cases, you should use new `kotlinx-serialization-json` artifact. Use `kotlinx-serialization-core` if you are
writing a library that depends on kotlinx.serialization in a format-agnostic way of provides its own serial format.

* `encodeDefaults` flag is now set to `false` in the default configuration for JSON, CBOR and Protocol Buffers.

The change is motivated by the fact that in most real-life scenarios, this flag is set to `false` anyway,
because such configuration reduces visual clutter and saves amount of data being serialized. 
Other libraries, like GSON and Moshi, also have this behavior by default.

This may change how your serialized data looks like, if you have not set value for `encodeDefaults` flag explicitly.
We anticipate that most users already had done this, so no migration is required.
In case you need to return to the old behavior, simply add `encodeDefaults = true` to your configuration while creating `Json/Cbor/ProtoBuf` object.

* Move `Json.encodeToDynamic/Json.decodeFromDynamic` functions to json package

Since these functions are no longer exposed via `DynamicObjectParser/Serializer` and they are now `Json` class extensions,
they should be moved to `kotlinx.serialization.json` package.
To migrate, simply add `import kotlinx.serialization.json.*` to your files.


### Bugfixes and improvements

  * Do not provide default implementation for serializersModule in AbstractEncoder/Decoder (#1089)
  * Support JsonElement hierarchy in `dynamic` encoding/decoding (#1080)
  * Support top-level primitives and primitive map keys in `dynamic` encoding/decoding
  * Change core annotations retention (#1083)
  * Fix 'Duplicate class ... found in modules' on Gradle != 6.1.1 (#996)
  * Various documentation clarifications
  * Support deserialization of top-level nullable types (#1038)
  * Make most serialization exceptions eligible for coroutines exception recovery (#1054)
  * Get rid of methods that do not present in Android API<24 (#1013, #1040)
  * Throw JsonDecodingException on empty string literal at the end of the input (#1011)
  * Remove new lines in deprecation warnings that caused errors in ObjC interop (#990)

1.0.0-RC / 2020-08-17
==================

Release candidate for 1.0.0 version. The goal of RC release is to collect feedback from users
and provide 1.0.0 release with bug fixes and improvements based on that feedback.

While working on 1.0.0 version, we carefully examined every public API declaration of the library and 
split it to stable API, that we promise to be source and binary-compatible, 
and experimental API, that may be changed in the future.
Experimental API is annotated with `@ExperimentalSerializationApi` annotation, which requires opt-in.
For a more detailed description of the guarantees, please refer to the [compatibility guide](docs/compatibility.md).

The id of the core artifact with `@Serializable` annotation and `Json` format was changed
from `kotlinx-serialization-runtime` to `kotlinx-serialization-core` to be more clear and aligned with other kotlinx libraries.

A significant part of the public API was renamed or extracted to a separate package.
To migrate from the previous versions of the library, please refer to the [migration guide](docs/migration.md).

### API changes

#### Json

* Core API changes
    * `stringify` and `parse` are renamed to `encodeToString` and `decodeFromString`
    * `parseJson` and `fromJson` are renamed to `parseToJsonElement` and `decodeFromJsonElement`
    * Reified versions of methods are extracted to extensions

* `Json` constructor is replaced with `Json {}` builder function, `JsonConfiguration` is deprecated in favor
of `Json {}` builder
    * All default `Json` implementations are removed
   * `Json` companion object extends `Json`

* Json configuration
    * `prettyPrintIndent` allows only whitespaces
    * `serializeSpecialFloatingPointValues` is renamed to `allowSpecialFloatingPointValues`. It now affects both serialization and deserialization behaviour
    * `unquoted` JSON flag is deprecated for removal
    * New `coerceInputValues` option for null-defaults and unknown enums (#90, #246)

* Simplification of `JsonElement` API
    * Redundant members of `JsonElement` API are deprecated or extracted to extensions
    * Potential error-prone API is removed
    * `JsonLiteral` is deprecated in favor of `JsonPrimitive` constructors with nullable parameter
    
* `JsonElement` builders rework to be aligned with stdlib collection builders (#418, #627)
    * Deprecated infix `to` and unaryPlus in JSON DSL in favor of `put`/`add` functions
    * `jsonObject {}` and `json {}` builders are renamed to `buildJsonObject {}` and `buildJsonArray {}`
    * Make all builders `inline` (#703)

* JavaScript support
    * `DynamicObjectParser` is deprecated in the favor of `Json.decodeFromDynamic` extension functions
    * `Json.encodeToDynamic` extension is added as a counterpart to `Json.decodeFromDynamic` (former `DynamicObjectParser`) (#116)

* Other API changes:
    * `JsonInput` and `JsonOutput` are renamed to `JsonDecoder` and `JsonEncoder`
    * Methods in `JsonTransformingSerializer` are renamed to `transformSerialize` and `transformDeserialize`
    * `JsonParametricSerializer` is renamed to `JsonContentPolymorphicSerializer`
    * `JsonEncodingException` and `JsonDecodingException` are made internal
    
* Bug fixes
    * `IllegalStateException` when `null` occurs in JSON input in the place of an expected non-null object (#816)
    * java.util.NoSuchElementException when deserializing twice from the same JsonElement (#807)

#### Core API for format authoring 

* The new naming scheme for `SerialFormats`
   *  Core functions in `StringFormat` and `BinaryFormat` are renamed and now follow the same naming scheme
   * `stringify`/`parse` are renamed to `encodeToString`/`decodeFromString`
   * `encodeToByteArray`/`encodeToHexString`/`decodeFromByteArray`/`decodeFromHexString` in `BinaryFormat` are introduced instead of `dump`/`dumps`/`load`/`loads`

* New format instances building convention
   * Constructors replaced with builder-function with the same name to have the ability to add new configuration parameters, 
   while preserving both source and binary compatibility
   * Format's companion objects now extend format class and can be used interchangeably

* SerialDescriptor-related API
    * `SerialDescriptor` and `SerialKind` are moved to a separate `kotlinx.serialization.descriptors` package
    * `ENUM` and `CONTEXTUAL` kinds now extend `SerialKind` directly
    * `PrimitiveDescriptor` is renamed to `PrimitiveSerialDescriptor`
    * Provide specific `buildClassSerialDescriptor` to use with classes' custom serializers, creating other kinds is considered experimental for now
    * Replace extensions that returned lists (e.g. `elementDescriptors`) with properties that return iterable as an optimization
    * `IndexOutOfBoundsException` in `descriptor.getElementDescriptor(index)` for `List` after upgrade to 0.20.0 is fixed (#739)

* SerializersModule-related API
    * `SerialModule` is renamed to `SerializersModule`
    * `SerialModuleCollector` is renamed to `SerializersModuleCollector`
    * All builders renamed to be aligned with a single naming scheme (e.g. `SerializersModule {}` DSL)
    * Deprecate infix `with` in polymorphic builder in favor of subclass()
    * Helper-like API is extracted to extension functions where possible.
    * `polymorphicDefault` API for cases when type discriminator is not registered or absent (#902)

* Contextual serialization
    * `@ContextualSerialization` is split into two annotations: `@Contextual` to use on properties and `@UseContextualSerialization` to use on file
    *  New `SerialDescriptor.capturedKClass` API to introspect SerializersModule-based contextual and polymorphic kinds (#515, #595)
    
* Encoding-related API
    * Encoding-related classes (`Encoder`, `Decoder`, `AbstractEncoder`, `AbstractDecoder`) are moved to a separate `kotlinx.serialization.encoding` package
    * Deprecated `typeParameters` argument in `beginStructure`/`beginCollectio`n methods
    * Deprecated `updateSerializableValue` and similar methods and `UpdateMode` enum
    * Renamed `READ_DONE` to `DECODE_DONE` 
    * Make extensions `inline` where applicable
    * `kotlinx.io` mockery (`InputStream`, `ByteArrayInput`, etc) is removed
   
* Serializer-related API
    * `UnitSerializer` is replaced with `Unit.serializer()`
    * All methods for serializers retrieval are renamed to `serializer`
    * Context is used as a fallback in `serializer` by KType/Java's Reflect Type functions (#902, #903)
    * Deprecated all exceptions except `SerializationException`.
    * `@ImplicitReflectionSerializer` is deprecated
    * Support of custom serializers for nullable types is added (#824)

#### ProtoBuf
    
* `ProtoBuf` constructor is replaced with `ProtoBuf {}` builder function
* `ProtoBuf` companion object now extends `ProtoBuf`
* `ProtoId` is renamed to `ProtoNumber`, `ProtoNumberType` to `ProtoIntegerType` to be consistent with ProtoBuf specification
* ProtoBuf performance is significantly (from 2 to 10 times) improved (#216)
* Top-level primitives, classes and objects are supported in ProtoBuf as length-prefixed tagless messages (#93)
* `SerializationException` is thrown instead of `IllegalStateException` on incorrect input (#870)
* `ProtobufDecodingException` is made internal

#### Other formats
   * All format constructors are migrated to builder scheme
   * Properties serialize and deserialize enums as strings (#818)
   * CBOR major type 2 (byte string) support (#842) 
   * `ConfigParser` is renamed to `Hocon`, `kotlinx-serialization-runtime-configparser` artifact is renamed to `kotlinx-serialization-hocon`
   * Do not write/read size of collection into Properties' map (#743)

0.20.0 / 2020-03-04
==================

### Release notes

0.20.0 release is focused on giving a library its final and stable API shape. 

We have carefully evaluated every `public` declaration and
decided whether it should be publicly available. As a result, some declarations were deprecated with an intention of removing
them from public API because they are going to be replaced with others, more valuable and useful for users. 

Deprecated symbols include: 
 - Pre-defined JSON instances like `nonStrict` — `strictMode` was split to 3 separate, more granular, flags.
Users are encouraged to create their own configuration; 
 - Top-level serializers like `IntSerializer` and `ArrayListSerializer`.
They were replaced with constructor-like factory functions.
 - `SerialClassDescImpl` creation class replaced with `SerialDescriptor` 
builder function to ease writing of custom serializers and maintain `SerialDescriptor` contract.
 - Internal utilities, like HexConverter and ByteBuffer, were deprecated as not relevant to serialization public API.
 - Add-on formats like Protobuf, CBOR and Properties (formerly Mapper)
are now extracted to [separate artifacts](formats/README.md#protobuf) to keep the core API lightweight.

We have spent a lot of effort into the quality,
documenting most of the core interfaces, establishing their contracts,
fixing numerous of bugs, and even introducing new features that may be useful for those of you who
write custom serializers — see [JsonTransformingSerializer](docs/json_transformations.md).  

Such API changes, of course, may be not backwards-compatible in some places, in particular, between compiler plugin and runtime.
Given that the library is still is in the experimental phase, we took the liberty to introduce breaking changes in order to give users
the better, more convenient API. Therefore, this release has number `0.20.0` instead of `0.15.0`;
Kotlin 1.3.70 is compatible _only_ with this release.

To migrate: 
1. Replace `import kotlinx.serialization.internal.*` with `import kotlinx.serialization.builtins.*`.
This action is sufficient for most of the cases, except primitive serializers — instead of using `IntSerializer`, use `Int.serializer()`.
For other object-like declarations, you may need to transform it to function call: `ByteArraySerializer` => `ByteArraySerializer()`.
 
2. Pay attention to the changed `JsonConfiguration` constructor arguments: instead of `strictMode`,
now three different flags are available: `ignoreUnknownKeys`, `isLenient`, and `serializeSpecialFloatingPointValues`.

3. If you used formats other than JSON, make sure you've included the corresponding artifact as dependency,
because now they're located outside of core module. See [formats list](formats/README.md) for particular artifact coordinates.

4. Other corresponding deprecation replacements are available via standard `@Deprecated(replaceWith=..)` mechanism.
(use Alt+Enter for quickfix replacing).

### Full changelog (by commit):
  
  * This release is compatible with Kotlin 1.3.70 
  * Rework polymorphic descriptors: polymorphic and sealed descriptor elements are now aligned with an actual serialization process (#731)
  * Hide internal collection and map serializers
  * Introduce factories for ArraySerializers as well, deprecate top-level array serializers
  * Extract ElementValue encoder and decoder to builtins and rename it to AbstractEncoder and AbstractDecoder respectively
  * Hide as much internal API as possible for collections. Now ListSerializer(), etc factories should be used
  * Replace top-level primitive serializers with corresponding companion functions from builtins
  * Move Tagged.kt to internal package
  * Hide tuple serializers from the public usages and replace them with factory methods in builtins package
  * Deprecate top-level format instances, leave only companion objects
  * Document contracts for JsonInput/JsonOutput (#715)
  * Ensure that serialization exception is thrown from JSON parser on invalid inputs (#704)
  * Do best-effort input/output attach to exceptions to simplify debugging
  * JSON configuration rework: strictMode is splitted into three flags.
  * Make strictMode even more restrictive, prohibit unquoted keys and values by default, always use strict boolean parser (#498, #467)
  * Preserve quotation information during JsonLiteral parsing (#536, #537)
  * Change MapEntrySerializer.descriptor to be truly map-like. Otherwise, it cannot be properly serialized by TaggedDecoder (-> to JsonObject)
  * Cleanup ConfigParser: move to proper package to be consistent with other formats
  * Support primitive and reference arrays in serializer(KType)
  * Add option to use HOCON naming convention
  * Allow DynamicObjectParser to handle polymorphic types (array-mode polymorphism only)
  * Get rid of PrimitiveKind.UNIT and corresponding encoder methods. Now UNIT encoded as regular object.
  * JsonParametricSerializer and JsonTransformingSerializer implementation
  * Remove AbstractSerialFormat superclass since it is useless
  * Deprecate most of the functions intended for internal use
  * Document core kotlinx.serialization.* package
  * Introduce UnionKind.CONTEXTUAL to cover Polymorphic/Contextual serializers, get rid of elementsCount in builders
  * SerialDescriptor for enums rework: now each enum member has object kind
  * Introduce DSL for creating user-defined serial descriptors
  * Update README with Gradle Kotlin DSL (#638)
  * Fix infinite recursion in EnumDescriptor.hashCode() (#666)
  * Allow duplicating serializers during SerialModule concatenation if they are equal (#616)
  * Rework sealed class discriminator check to reduce the footprint of the check when no JSON is used
  * Detect collisions with class discriminator and for equal serial names within the same sealed class hierarchy (#457)
  * Detect name conflicts in polymorphic serialization during setup phase (#461, #457, #589)
  * Extract all mutable state in modules package to SerialModuleBuilder to have a single mutable point and to ensure that SerialModule can never be modified
  * Omit nulls in Properties.store instead of throwing an exception
  * Add optionals handling to Properties reader (#460, #79)
  * Support StructureKind.MAP in Properties correctly (#406)
  * Move Mapper to separate 'properties' module and rename it to Properties
  * Reified extensions for registering serializers in SerialModule (#671, #669)
  * Promote KSerializer.nullable to public API
  * Object serializer support in KType and Type based serializer lookups on JVM (#656)
  * Deprecate HexConverter
  * Supply correct child descriptors for Pair and Triple
  * Rename SerialId to ProtoId to better reflect its semantics
  * Support of custom generic classes in typeOf()/serializer() API (except JS)
  * Allow setting `ProtoBuf.shouldEncodeElementDefault` to false (#397, #71)
  * Add Linux ARM 32 and 64 bit targets
  * Reduce number of internal dependencies: deprecate IOException, mark IS/OS as internal serialization API (so it can be removed in the future release)
  * Reduce number of internal dependencies and use bitwise operations in ProtoBuf/Cbor instead of ByteBuffer. Deprecate ByteBuffer for removal
  * Extract ProtoBuf & CBOR format to the separate module
  * READ_ALL rework (#600)
  * SerialDescriptor API standartization (#626, #361, #410)
  * Support polymorphism in CBOR correctly (fixes #620)
  * Add forgotten during migration WASM32 target (#625)
  * Fix exception messages & typos in JsonElement (#621)

v0.14.0 / 2019-11-19
==================

  * Bump version to 0.14.0 @ Kotlin 1.3.60
  * Add empty javadoc artifact to linking with Maven Central
  * Mark more things as @InternalSerializationApi.
  * Support @SerialId on enum members in protobuf encoding
  * Move Polymorphic and sealed kinds from UnionKind to special PolymorphicKind
  * Sealed classes serialization & generated serializers for enum classes (@SerialInfo support)
  * Objects serialization
  * Don't use deprecated UTF8<>ByteArray conversions in Native
  * Improve error message when static non-generic serializer can't be found
  * Support optional values for typesafe config format

v0.13.0 / 2019-09-12
==================

  * Add mingwX86 target (#556)
  * Replace KClass.simpleName with artificial expect/actual with java.lang.Class.simpleName on JVM to overcome requirement for kotlin-reflect.jar (#549)
  * Update Gradle to 5.6.1 (therefore Gradle metadata to 1.0)
  * Fix incorrect index supply during map deserialization when READ_ALL was used (#526)
  * Serializers for primitive arrays (ByteArray etc)
  * Hide NullableSerializer, introduce '.nullable' extension instead
  * Fix the library to not create a stack overflow exception when creating a MissingDescriptorException. (#545)

v0.12.0 / 2019-08-23
==================

  * Set up linuxArm32Hfp target (#535)
  * wasm32 is added as a build target (#518)
  * MPP (JVM & Native) serializer resolving from KType (via typeOf()/serializer() function)
  * Support maps and objects decoding when map size present in stream (fix #517)
  * Add proper SerialClassDescImpl.toString
  * Make JSON parser much more stricter; e.g. Prohibit all excessive separators in objects and maps
  * Robust JsonArray parsing
  * Improve json exceptions, add more contextual information, get rid of obsolete exception types
  * Prohibit trailing commas in JSON parser
  * Make the baseclass of the polymorphic serializer public to allow formats (#520)
  * Fix decoding for ProtoBuf when there are missing properties in the model. (#506)
  * Rework JsonException and related subclasses
  * Fix #480 (deserialization of complex map keys). Add tests for structured map keys in conjuction with polymorphism
  * Implement 'allowStructuredMapKeys' flag. Now this flag is required for serializing into JSON maps which keys are not primitive.

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
