# Kotlin serialization in 1.3

## Disclaimer

Library is experimental and under heavy development. We are moving towards the [KEEP](https://github.com/Kotlin/KEEP/blob/serialization/proposals/extensions/serialization.md), so some features described there can be not implemented yet.
While library is stable and has successfully been used in various scenarios, there is no compatibility and API guarantees between versions.

## Gradle plugin and setup

Starting from 1.3, compiler plugin is bundled with main distribution.
This means new maven coordinates and versioning scheme:

```gradle
buildscript {
    ext.kotlin_version = '1.3.0-rc-146'
    
    // no need in kotlinx repository
    repositories { 
        jcenter() 
        maven { url "https://dl.bintray.com/kotlin/kotlin-eap" } // while you are on 1.3-RC
    } 
    

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

Current `serialization_version` is `0.8.2-rc13`. Sources can be found in the `eap13` branch.

Don't forget to apply the plugin:

```gradle
apply plugin: 'kotlin' // or 'kotlin-platform-***' or 'kotlin-multiplatform'
apply plugin: 'kotlinx-serialization'
```

### IntelliJ IDEA/Android Studio

Works out of the box in 2018.2, 2018.3, Android Studio 3.2 and 3.3 — if you have 1.3 Kotlin plugin installed.
Please note that projects with old version of kotlinx.serialization **will not be correctly imported**.
In case of problems, force project re-import from Gradle. You still need to delegate build to Gradle:
(`Settings - Build, Execution, Deployment - Build Tools - Gradle - Runner -` tick `Delegate IDE build/run actions to gradle`)

### JS and common

Use `kotlinx-serialization-runtime-js` and `kotlinx-serialization-runtime-common` artifacts.

### Native

You can apply the plugin to `kotlin-platform-native` or `kotlin-multiplatform` projects.
`konan` plugin is not supported and deprecated.

**Important note**: for `kotlin-multiplatform` project, apply usual `kotlinx-serailization` plugin.
For `kotlin-platform-native` module, apply `kotlinx-serialization-native` plugin,
since platform-native from K/N 0.9.3 uses infrastructure in which compiler plugins [are shaded](https://github.com/JetBrains/kotlin-native/issues/2210#issuecomment-429753168).


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

Compatible Kotlin/Native version is `1.3.0-rc-146` (which is 0.9.3 under the hood).

### Json parser

Separate `jsonparser` and `jsonparser-native` artifacts are deprecated and will be removed in future.

## Migration guide

* Make sure you have updated maven coordinates and version of the compiler plugin.
* Recompile or update any dependent libraries you have.

If you haven't written any custom/context serializers our touched internal machinery, you're done. Otherwise,

* Read the [KEEP](https://github.com/Kotlin/KEEP/blob/serialization/proposals/extensions/serialization.md) about new design.
* Rename superclasses: `KInput` -> `Decoder/CompositeDecoder`, `KOutput` -> `Encoder/CompositeEncoder`, `KSerialClassDesc` -> `SerialDescriptor`.
* Update all method names.

### On Context serializer

KEEP assumes that context and polymorphic serializers would not be applied implicitly.
Current plugin implementation still does this, but this functionality will be disabled in future without further notice.
To migrate your code which has been using context serializers, apply `@ContextualSerialization` to each property or type (e.g. `List<@ContextualSerialization MyDate>`), or use
file-level form of it: `@ContextualSerialization(MyDate::class)`.
To migrate your code which has been using polymorphic serializers, apply `@Polymorphic` to each property or type.
