# Compatibility policy

This document describes the compatibility policy of kotlinx.serialization library since version 1.0.0 and Kotlin 1.4.0.

Note that content of this document is applicable only for **stable** Kotlin platforms (currently Kotlin/JVM and classic Kotlin/JS),
since other experimental platforms currently do not impose any backward-compatibility guarantees.
You can check out what platforms are considered to be stable on [this page](https://kotlinlang.org/docs/reference/evolution/components-stability.html).

- [Core library compatibility](#core-library-compatibility)
  * [General (Stable) API](#stable-api)
  * [Experimental API](#experimental-api)
  * [Internal API](#internal-api)
- [Compatibility with Kotlin compiler plugin](#compatibility-with-kotlin-compiler-plugin)

## Core library compatibility

Core library public API comes in three flavours: general (stable), experimental, and internal.
All public API except stable is marked with the corresponding annotation.
To learn how to use declarations that require opt-in, please refer to [corresponding documentation page](https://kotlinlang.org/docs/reference/opt-in-requirements.html#non-propagating-use).

### Stable API

Stable API is guaranteed to preserve its ABI and documented semantics:

* It cannot change its semantics expressed in its documentation.
* It is binary backwards-compatible: during update of `kotlinx.serialization` version, previously compiled code will continue to work.
    For example, for a library that depends only on `kotlinx.serialization` stable API,
    clients of the library can easily depend on a next `kotlinx.serialization` version and expect everything to work.
* It is source backwards compatible modulo major deprecation. Most of the API is here to stay forever,
unless an unfixable security or design flaw is exposed. Minor releases never add source-incompatible changes to the stable API.

#### Deprecation cycle

When API is deprecated, it goes through multiple stages and there is at least one major release between each stages.

1. Feature is deprecated with compilation warning. Most of the time, proper replacement (and corresponding `replaceWith` declaration) is provided to automatically migrate deprecated usages with a help of IntelliJ IDEA.
2. Deprecation level is increased to error or hidden. It is no longer possible to compile new code against deprecated API, though it is still present in the ABI.
3. API is completely removed. While we give our best efforts not to do so and have no plans of removing any API, we still are leaving this option in case of unforeseen problems such as security issues. 


### Experimental API

This API marked as `@ExperimentalSerializationApi`. API is marked experimental when its design has potential open questions which may eventually lead to either semantics changes of the API or its deprecation.
By default, most of the new API is marked as experimental and becomes stable in one of the next releases if no new issues arise. Otherwise, either semantics is fixed without changes in ABI or API goes through deprecation cycle.

However, we'll try to provide best-effort compatibility — such declarations won't be deleted or changed instantly,
they will go through deprecation cycle if this is possible. However, this deprecation cycle may be faster than usual.

Usage notes:

* Experimental API can be used in your applications if maintenance cost is clear: 
additional migrations may have to be performed during `kotlinx.serialization` update.

* Experimental API can be used in other **experimental** API (for example, a custom serialization format).
In such cases, clients of the API have to be aware about experimentality.

* It's not recommended to use it as a dependency in your **stable** API, even as an implementation detail.
Due to the lack of binary backward compatibility, your clients may experience behavioural changes
or runtime exceptions when an unexpected version of `kotlinx.serialization` gets included in the runtime classpath.

### Internal API

This API is marked with `@InternalSerializationApi` or located in `kotlinx.serialization.internal` package.
It does not have any binary or source compatibility guarantees and can be deprecated or deleted without replacement at any time.

It is not recommended to use it. 
However, if you have a rare use-case that can be solved only with internal API, it is possible to use it.
In such a case, please create an issue on GitHub in order for us to understand a use-case and to provide stable alternative.

## Compatibility with Kotlin compiler plugin

`kotlinx.serialization` also has the compiler plugin, that generates code depending on the core library.
Therefore, the compiler plugin should be compatible with the runtime library to work.
Kotlin & `kotlinx.serialization` plugin 1.4.0/1.4.10 are compatible with 1.0.0 runtime library.

For further updates, we have the following policy:

* New Kotlin compiler plugins should be backward compatible with core library.
It means that it is possible to freely update Kotlin version in a project without changing the code
and without the need to update `kotlinx.serialization` runtime.
In other words, `1.0.0` runtime can be used with any of Kotlin `1.4.x` versions.

* New Kotlin compiler plugin features may require new `kotlinx.serialization` library.
For example, if Kotlin `1.4.x` gets serialization of unsigned integers,
it would require a corresponding runtime version higher than `1.0.0`.
This would be indicated by a compiler error specific to a particular feature.

* New core library versions may or may not require Kotlin compiler plugin update,
depending on a particular release.
We'll try to avoid these situations; however, in case of some unexpected issues, it may be necessary.
So it is possible to have a situation where upgrading serialization runtime from `1.x` to `1.y` requires an update of Kotlin version from `1.4.0` to `1.4.x`.
The compiler can detect such problems and will inform you if its version is incompatible with a current version of core library.
