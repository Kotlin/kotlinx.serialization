# Kotlin serialization in 1.3

## Disclaimer

Library is experimental and under heavy development. We are moving towards the [KEEP](https://github.com/Kotlin/KEEP/blob/serialization/proposals/extensions/serialization.md)
, so some features described there can be not implemented yet.
While library is stable and has successfully been used in various scenarios, there is no compatibility and API guarantees between versions.

## Gradle plugin and setup

Starting from 1.3, compiler plugin is bundled with main distribution.
This means new maven coordinates and versioning scheme:

```gradle
buildscript {
    ext.kotlin_version = '1.3.0-rc-51'
    repositories { jcenter() } // no need in kotlinx repository

    dependencies {
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version"
        // kotlin package, and no "gradle-plugin" in name
        // version matches Kotlin version
        classpath "org.jetbrains.kotlin:kotlin-serialization:$kotlin_version"
    }
}
```

However, this applies only to plugin, runtime library **has old kotlinx coordinates**
and its own versioning scheme:

```gradle
repositories {
    jcenter()
    maven { url "https://kotlin.bintray.com/kotlinx" }
}

dependencies {
    compile "org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version"
    compile "org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serialization_version"
}
``` 

Current `serialization_version` is `0.8.0-rc13`. Sources can be found in the `eap13` branch.

Don't forget to apply the plugin:

```gradle
apply plugin: 'kotlin'
apply plugin: 'kotlinx-serialization'
```

### IntelliJ IDEA

Works out of the box, if you have 1.3 Kotlin plugin installed.
In case of problems, force project re-import from Gradle or/and delegate build to Gradle
(`Settings - Build, Execution, Deployment - Build Tools - Gradle - Runner -` tick `Delegate IDE build/run actions to gradle`)

### JS and common

Use `kotlinx-serialization-runtime-js` and `kotlinx-serialization-runtime-common` artifacts.

### Native

You can apply the plugin to `kotlin-platform-native` or `kotlin-multiplatform` projects.
`konan` plugin is not supported and deprecated.

Use `kotlinx-serialization-runtime-native` artifact. Don't forget to `enableFeaturePreview('GRADLE_METADATA')`
in yours `settings.gradle`. You must have Gradle 4.7, because higher versions have unsupported format of metadata.

Serialization compiler plugin for Native is still in active development, and is not as feature-full as JVM/JS plugins.

What **works**: 

* `@Serializable` classes with primitive or `@Serializable` properties
* Standard collections
* `@Optional` and `@SerialName` annotations

What **does not work**:

* `@Serializable` classes with generics (except standard collections)
* Enums and arrays (`Array<T>, ByteArray, etc`)
* `@Transient` initializers and `init` blocks
* `@SerialInfo`-based annotations

Compatible Kotlin/Native versions are `0.9.2` and `0.9.2-dev-4008`.

### Json parser

Separate `jsonparser` and `jsonparser-native` are deprecated and will be removed in future.

## Migration guide

* Make sure you have updated maven coordinates and version of the compiler plugin.
* Recompile or update any dependent libraries you have.

If you haven't written any custom serializers our touched internal machinery, you're done. Otherwise,

* Read the [KEEP](https://github.com/Kotlin/KEEP/blob/serialization/proposals/extensions/serialization.md) about new design.
* Rename superclasses: `KInput` -> `Decoder/CompositeDecoder`, `KOutput` -> `Encoder/CompositeEncoder`, `KSerialClassDesc` -> `SerialDescriptor`.
* Update all method names.

### On Context serializer

KEEP assumes that context and polymorphic serializers would not be applied implicitly.
Current plugin implementation still does this, but this functionality will be disabled in future without further notice.
To migrate your code which has been using context serializers, apply `@ContextualSerialization` to each property or type (e.g. `List<@ContextualSerialization MyDate>`), or use
file-level form of it: `@ContextualSerialization(MyDate::class)`.
To migrate your code which has been using polymorphic serializers, apply `@Polymorphic` to each property or type.
