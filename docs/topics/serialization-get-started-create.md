[//]: # (title: Add Kotlin serialization plugins and dependencies)

<microformat>
   <p>This is the first part of the <strong>Getting started with Kotlin serialization</strong> tutorial:</p>
   <p><img src="icon-1.svg" width="20" alt="First step"/> <strong>Add Kotlin serialization plugins and dependencies</strong><br/>
      <img src="icon-2-todo.svg" width="20" alt="Second step"/> Serialize an object<br/>
      <img src="icon-3-todo.svg" width="20" alt="Third step"/> Add dependencies to a Kotlin Notebook<br/>      
      <img src="icon-4-todo.svg" width="20" alt="Fourth step"/> Share a Kotlin Notebook<br/>
  </p>
</microformat>

In this section of the tutorial, you will learn how to add the necessary serialization dependencies using IntelliJ IDEA.

To get started, first download and install the latest version of [IntelliJ IDEA](https://www.jetbrains.com/idea/download/).

## Create a project

1. In IntelliJ IDEA, select **File | New | Project**.
2. In the panel on the left, select **New Project**.
3. Name the new project and change its location if necessary.

   > Select the **Create Git repository** checkbox to place the new project under version control.
   > You will be able to do it later at any time.
   >
   {type="tip"}

4. From the **Language** list, select **Kotlin**.
5. Select the **IntelliJ** build system.
6. From the **JDK list**, select the [JDK](https://www.oracle.com/java/technologies/downloads/) that you want to use in your project.
7. Enable the **Add sample code** option to create a file with a sample `"Hello World!"` application.

   > You can also enable the **Generate code with onboarding tips** option to add some additional useful comments to your sample code.
   >
   {type="tip"}

8. Click **Create**.

## Add plugins and dependencies for Kotlin serialization

Before starting, you must configure your build script to use Kotlin serialization tools in your project.
The instructions below cover how to apply the Kotlin serialization plugin and add the necessary dependencies using Gradle
(with Kotlin DSL and Groovy DSL) and Maven.

<tabs>
<tab id="kotlin" title="Kotlin DSL">

1. Add the Kotlin serialization Gradle plugin `kotlin("plugin.serialization")` to your `build.gradle.kts` file:

    ```kotlin
    plugins {
        kotlin("jvm") version "%kotlinVersion%"
        kotlin("plugin.serialization") version "%kotlinVersion%"
    }
    ```

2. Add the JSON serialization library dependency `org.jetbrains.kotlinx:kotlinx-serialization-json:%serializationVersion%` to your `build.gradle.kts` file:

    ```kotlin
    dependencies { 
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:%serializationVersion%")
    } 
    ```

</tab>

<tab id="groovy" title="Groovy DSL">

1. Add the Kotlin serialization Gradle plugin `org.jetbrains.kotlin.plugin.serialization` to your `build.gradle` file:

    ```groovy
    plugins {
        id 'org.jetbrains.kotlin.jvm' version '%kotlinVersion%'
        id 'org.jetbrains.kotlin.plugin.serialization' version '%kotlinVersion%'  
    }
    ```

2. Add the JSON serialization library dependency `org.jetbrains.kotlinx:kotlinx-serialization-json:%serializationVersion%` to your `build.gradle` file:

    ```groovy
    dependencies {
        implementation 'org.jetbrains.kotlinx:kotlinx-serialization-json:%serializationVersion%'
    } 
    ```

</tab>

<tab id="maven" title="Maven">

1. Specify the Kotlin and serialization versions by adding the following properties to your `pom.xml` file:

    ```xml
    <properties>
    <kotlin.version>%kotlinVersion%</kotlin.version>
    <serialization.version>%serializationVersion%</serialization.version>
    </properties>
   ```

2. Add the Kotlin serialization Maven plugin to the `<build>` section of your `pom.xml` file:

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
                       <groupId>org.jetbrains.kotlin</groupId>
                       <artifactId>kotlin-maven-serialization</artifactId>
                       <version>${kotlin.version}</version>
                   </dependency>
               </dependencies>
           </plugin>
       </plugins>
   </build>
   ```

3. Add the JSON serialization library dependency to the `<dependencies>` section of your `pom.xml` file:

   ```xml
   <dependencies>
       <dependency>
           <groupId>org.jetbrains.kotlinx</groupId>
           <artifactId>kotlinx-serialization-json</artifactId>
           <version>${serialization.version}</version>
       </dependency>
   </dependencies>
   ```

</tab>

<tab id="bazel" title="Bazel">

To set up the Kotlin compiler plugin for Bazel, see the example provided in the [rules_kotlin repository](https://github.com/bazelbuild/rules_kotlin/tree/master/examples/plugin/src/serialization).

</tab>

</tabs>

### Add Kotlin serialization dependencies for multiplatform projects

You can use Kotlin serialization in multiplatform projects, including Kotlin/JS and Kotlin/Native.
To do so, you must add the JSON serialization library dependency to your common source set:

```kotlin
commonMain {
   dependencies {
      implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:%serializationVersion%")
   }
}
```

### Optimizing Kotlin Serialization for Android with ProGuard and R8

THIS SHOULD BE PLACED IN AN ADVANCED SECTION FOR ANDROID