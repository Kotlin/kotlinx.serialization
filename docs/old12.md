# Instructions for old versions under Kotlin 1.2 and migration guide

Library API is currently changing on its way to match the corresponding [KEEP](https://github.com/Kotlin/KEEP/blob/serialization/proposals/extensions/serialization.md). 
Migration guide will be kept up-to-date as the new API evolves.

We strongly encourage you to upgrade to Kotlin 1.3, since migration is pretty easy and straightforward. However, if you'll face some issues, you can always setup project with Kotlin 1.2 according to the instructions [at the bottom of the page](#setup-guide-for-kotlin-12).

## Migration guide

* Make sure you have updated maven coordinates and version of the compiler plugin.
* Recompile or update any dependent libraries you have.
* Some versions of `stringify`, `parse`, `dump(s)` and `load(s)` are now extensions, so import of them from `kotlinx.serialization` package may be required.

If you haven't written any custom serializers our touched internal machinery, you're done. Otherwise,

* Read the [KEEP](https://github.com/Kotlin/KEEP/blob/serialization/proposals/extensions/serialization.md) about new design.
* Rename superclasses: `KInput` -> `Decoder/CompositeDecoder`, `KOutput` -> `Encoder/CompositeEncoder`, `KSerialClassDesc` -> `SerialDescriptor`.
* Update all method names.

In case you face any issues, see updated documents about the new API. 

### On Context serializer

KEEP assumes that context and polymorphic serializers would not be applied implicitly.
Current plugin implementation still does this, but this functionality will be disabled in future without further notice.
To migrate your code which has been using context serializers, apply `@ContextualSerialization` to each property or type (e.g. `List<@ContextualSerialization MyDate>`), or use
file-level form of it: `@ContextualSerialization(MyDate::class)`.
To migrate your code which has been using polymorphic serializers, apply `@Polymorphic` to each property or type.

## Setup guide for Kotlin 1.2

Using Kotlin Serialization requires Kotlin compiler `1.1.50` or higher, recommended version is `1.2.71`. See [compatibility table](#compatibility) below.
Also, it's recommended to install [additional IDEA plugin](#working-in-intellij-idea) for better IDE experience. Otherwise,
some valid code will be shown as red and builds will have to be launched from console or build system tasks panel.

### Gradle/JVM

Ensure the proper version of Kotlin and add dependencies on plugin in addition to Kotlin compiler:

```gradle
buildscript {
    ext.kotlin_version = '1.2.71'
    ext.serialization_version = '0.6.2'
    repositories {
        jcenter()
        maven { url "https://kotlin.bintray.com/kotlinx" }
    }

    dependencies {
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version"
        classpath "org.jetbrains.kotlinx:kotlinx-gradle-serialization-plugin:$serialization_version"
    }
}
```

Don't forget to apply the plugin:

```gradle
apply plugin: 'kotlin'
apply plugin: 'kotlinx-serialization'
```

Add serialization runtime library in addition to Kotlin standard library.

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

### Android/JVM

Library should work on Android "as is". If you're using proguard, you need
to add this to your `proguard-rules.pro`:

```proguard
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keep,includedescriptorclasses class com.yourcompany.yourpackage.**$$serializer { *; } # <-- change package name to your app's
-keepclassmembers class com.yourcompany.yourpackage.** { # <-- change package name to your app's
    *** Companion;
}
-keepclasseswithmembers class com.yourcompany.yourpackage.** { # <-- change package name to your app's
    kotlinx.serialization.KSerializer serializer(...);
}
```

You may also want to keep all custom serializers you've defined.

### Maven/JVM

Ensure the proper version of Kotlin and serialization version: 

```xml
<properties>
    <kotlin.version>1.2.71</kotlin.version>
    <serialization.version>0.6.2</serialization.version>
</properties>
```

Include bintray repository for both library and plugin:

```xml
<repositories>
    <repository>
        <id>bintray-kotlin-kotlinx</id>
        <name>bintray</name>
        <url>https://kotlin.bintray.com/kotlinx</url>
    </repository>
</repositories>
<pluginRepositories>
    <pluginRepository>
        <id>bintray-kotlin-kotlinx</id>
        <name>bintray-plugins</name>
        <url>https://kotlin.bintray.com/kotlinx</url>
    </pluginRepository>
</pluginRepositories>
```

Add serialization plugin to Kotlin compiler plugin:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.jetbrains.kotlin</groupId>
            <artifactId>kotlin-maven-plugin</artifactId>
            <version>${kotlin.version}</version>
            <executions>
                <execution>
                    <id>compile</id>
                    <phase>compile</phase>
                    <goals>
                        <goal>compile</goal>
                    </goals>
                </execution>
            </executions>
            <configuration>
                <compilerPlugins>
                    <plugin>kotlinx-serialization</plugin>
                </compilerPlugins>
            </configuration>
            <dependencies>
                <dependency>
                    <groupId>org.jetbrains.kotlinx</groupId>
                    <artifactId>kotlinx-maven-serialization-plugin</artifactId>
                    <version>${serialization.version}</version>
                </dependency>
            </dependencies>
        </plugin>
    </plugins>
</build>
```

Add dependency on serialization runtime library:

```xml
<dependency>
    <groupId>org.jetbrains.kotlinx</groupId>
    <artifactId>kotlinx-serialization-runtime</artifactId>
    <version>${serialization.version}</version>
</dependency>
```

### JavaScript and common

Replace dependency on `kotlinx-serialization-runtime` with `kotlinx-serialization-runtime-js` or `kotlinx-serialization-runtime-common`
to use it in JavaScript and common projects, respectively.

### Native

Full library is not available on native, since there are no plugin API in compiler v 1.2. You can use deprecated `jsonparser-native` artifact.

### Working in IntelliJ IDEA

Instead of using Gradle or Maven, IntelliJ IDEA relies on its own build system when analyzing and running code from within IDE.
Because serialization is still highly experimental, it is shipped as a separate artifact from "big" Kotlin IDEA plugin.
You can download additional IDEA plugin for working with projects that uses serialization from its 
TeamCity build page:

* Latest release (1.2.70): [link](https://teamcity.jetbrains.com/viewLog.html?buildId=lastPinned&buildTypeId=KotlinTools_KotlinxSerialization_KotlinCompilerWithSerializationPlugin&tab=artifacts&guest=1&buildBranch=1.2.70)

* For 1.2.50 and lower (not updated): [link](https://teamcity.jetbrains.com/viewLog.html?buildId=lastPinned&buildTypeId=KotlinTools_KotlinxSerialization_KotlinCompilerWithSerializationPlugin&tab=artifacts&guest=1&buildBranch=1.2.50)
* For 1.2.31 and lower (not updated): [link](https://teamcity.jetbrains.com/viewLog.html?buildId=lastPinned&buildTypeId=KotlinTools_KotlinxSerialization_KotlinCompilerWithSerializationPlugin&tab=artifacts&guest=1&buildBranch=1.2.30)
* For 1.2.40 and higher (not updated): [link](https://teamcity.jetbrains.com/viewLog.html?buildId=lastPinned&buildTypeId=KotlinTools_KotlinxSerialization_KotlinCompilerWithSerializationPlugin&tab=artifacts&guest=1&buildBranch=1.2.40)


In IDEA, open `Settings - Plugins - Install plugin from disk...` and select downloaded .zip or .jar file.
This installation will allow you to run code/tests from IDEA.

In case of issues with IDE, try to use gradle for running builds:
`Settings - Build, Execution, Deployment - Build Tools - Gradle - Runner -` tick `Delegate IDE build/run actions to gradle`; or launch builds from console.

### Compatibility

|Plugin Version|Compiler version|
|--------------|----------------|
| 0.1 – 0.3 | 1.1.50 – 1.2.10|
| 0.4 – 0.4.1 | 1.2.20 – 1.2.21|
| 0.4.2 – 0.5.0 | 1.2.30 – 1.2.41|
| 0.5.1 - 0.6.0 | 1.2.50 - 1.2.51|
| 0.6.1 | 1.2.60 - 1.2.61 |
| 0.6.2 | 1.2.70 - 1.2.71 |

Eap compiler versions are usually supported by snapshot versions (e.g. 1.2.60-eap-* is supported only by 0.6.1-SNAPSHOT)

All ranges in table are inclusive.
