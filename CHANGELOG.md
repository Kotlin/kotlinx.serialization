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
