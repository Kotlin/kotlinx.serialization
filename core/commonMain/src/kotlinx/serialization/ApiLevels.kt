/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.descriptors.*

/**
 * Marks declarations that are still **experimental** in kotlinx.serialization, which means that the design of the
 * corresponding declarations has open issues which may (or may not) lead to their changes in the future.
 * Roughly speaking, there is a chance that those declarations will be deprecated in the near future or
 * the semantics of their behavior may change in some way that may break some code.
 *
 * By default, the following categories of API are experimental:
 *
 * * Writing 3rd-party serialization formats
 * * Writing non-trivial custom serializers
 * * Implementing [SerialDescriptor] interfaces
 * * Not-yet-stable serialization formats that require additional polishing
 */
@MustBeDocumented
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION, AnnotationTarget.TYPEALIAS)
@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
public annotation class ExperimentalSerializationApi

/**
 * Public API marked with this annotation is effectively **internal**, which means
 * it should not be used outside of `kotlinx.serialization`.
 * Signature, semantics, source and binary compatibilities are not guaranteed for this API
 * and will be changed without any warnings or migration aids.
 * If you cannot avoid using internal API to solve your problem, please report your use-case to serialization's issue tracker.
 */
@MustBeDocumented
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION, AnnotationTarget.TYPEALIAS)
@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
public annotation class InternalSerializationApi

/**
 * Marks interfaces and non-final classes that can be freely referenced in users' code but should not be
 * implemented or inherited. Such declarations are effectively `sealed` and do not have this modifier purely for technical reasons.
 *
 * kotlinx.serialization library provides compatibility guarantees for existing signatures of such classes;
 * however, new functions or properties can be added to them in any release.
 */
@MustBeDocumented
@Target() // no direct targets, only argument to @SubclassOptInRequired
@RequiresOptIn(message = "This class or interface should not be inherited/implemented outside of kotlinx.serialization library. " +
    "Note it is still permitted to use it directly. Read its documentation about inheritance for details.", level = RequiresOptIn.Level.ERROR)
public annotation class SealedSerializationApi

