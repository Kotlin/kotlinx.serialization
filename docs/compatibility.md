# Compatibility policy

This document describes the compatibility policy of kotlinx.serialization library since version 1.0.0 and Kotlin 1.4.0.

Note that content of this document can be applied only to JVM platform,
since Kotlin/Native and Kotlin/JS are experimental themselves and currently do not impose any backward-compatibility guarantees.

- [Runtime library compatibility](#runtime-library-compatibility)
  * [General (Stable) API](#general-stable-api)
  * [Experimental API](#experimental-api)
  * [Internal API](#internal-api)
- [Compatibility with Kotlin compiler plugin](#compatibility-with-kotlin-compiler-plugin)

## Runtime library compatibility

Runtime library provides three kinds of API: general (stable), experimental, and internal.
They all have different compatibility policies.
Experimental and internal APIs are marked accordingly and require opt-in (see sections below), all other API
is considered to be stable.
To learn how to use declarations that require opt-in, see [corresponding documentation page](https://kotlinlang.org/docs/reference/opt-in-requirements.html#non-propagating-use).

### General (Stable) API

This API is considered stable. This means the following:

* It cannot change its semantics expressed in the documentation.

* It is binary backward-compatible: if one updates kotlinx.serialization version, your compiled code will continue to work.
For example, if you're writing a library that depends on kotlinx.serialization and using only stable API,
clients of your library can freely increase kotlinx.serialization version.

* It is source backward compatible with deprecations:
API can only be deleted in rare scenarios (such as unfixable design flaws) with proper deprecation cycle.
Such API is going to be deprecated in two steps:
first, a declaration is marked as deprecated with `DeprecationLevel.WARNING` and proper replacement.
Then, in the next major release, these declarations are either deleted (`DeprecationLevel.HIDDEN`)
or marked as an error (`DeprecationLevel.ERROR`).
This means that you can freely update to any minor release and migrate to new declarations as a part of your routine maintenance.

### Experimental API

This API marked as `@ExperimentalSerializationApi` because its design is unfinished and may be changed in the future based on users' feedback.
We do not provide any binary compatibility guarantees for it.
However, we'll try to provide best-effort source compatibility — such declarations won't be deleted instantly,
they will go through deprecation cycle if this is possible. Notice that such deprecation cycle may be faster than usual,
and experimental API deprecated with a warning may be deleted in the next minor release.

* You _may_ use it in your applications if you accept maintenance cost, and you understand that you
may have to perform migrations every time you update kotlinx.serialization runtime library.

* You _may_ use it as a dependency in your **experimental** libraries (for example, a custom serialization format).
In such cases, your clients need to be aware that they may not be able to update kotlinx.serialization library independently of your library.

* You _should not_ use it as a dependency in your **stable** libraries or their stable parts.
Due to the lack of binary backward compatibility, your clients may experience bugs
or runtime crashes when an unexpected version of kotlinx.serialization gets included in the runtime classpath.

### Internal API

This API is marked with `@InternalSerializationApi` or located in `kotlinx.serialization.internal` package.
It does not have any binary or source compatibility guarantees and can be deprecated or deleted without replacement at any time.

Typically, you do not have to use them. However,
if you have a rare use-case that can be solved only via internal API, it is possible to use it.
In such a case, please create an issue on GitHub so proper public API can be introduced.

## Compatibility with Kotlin compiler plugin

As you may know, kotlinx.serialization also has the compiler plugin, which generates code that contains calls to the runtime library.
Therefore, the compiler plugin should be compatible with the runtime library to work.
Kotlin & kotlinx.serialization plugin 1.4.0 are compatible with 1.0.0 runtime library.
For further updates, we have the following rules:

* New Kotlin compiler plugins should be backward compatible with runtime.
It means that you can freely update Kotlin version in your project without changing the code
and without the need to update kotlinx.serialization runtime.
In other words, 1.0.0 runtime can be used with any of Kotlin 1.4.x versions.

* New Kotlin compiler plugin features may require new kotlinx.serialization runtime.
For example, if Kotlin 1.4.n gets serialization of unsigned integers,
it would require a corresponding runtime version higher than 1.0.0.
This would be indicated by a compiler error specific to a particular feature.

* New runtime library versions may or may not require Kotlin compiler plugin update,
depending on a particular release.
We'll try to avoid this situation; however, in case of some unexpected design issues, this may be necessary.
So you may face the situation where upgrading from serialization runtime 1.n to 1.m requires an update of Kotlin version from 1.4.0 to 1.4.x.
The compiler can detect such problems and will inform you if its version is too low to work with a
particular release.