# Alternative approach to generate serialization code

To make code more readable and debuggable it's possible to
use generated sources for serializers in Kotlin instead of target platform code.
This approach also unlock possibilities to modify runtime interfaces and architecture easily and fast;
moreover, Kotlin/Native (which currently does not support compiler plugins) will be available soon.

`kotlinx-serialization-runtime-sourcegen` provides DSL based on [`kotlinpoet`](https://github.com/square/kotlinpoet) 
to generate Kotlin classes with its serializers and does not rely on any compiler magic or reflection.
This DSL can be used in buildscripts or custom Gradle Plugins to generate additional sources for your project.

Example of such setup can be found in [`example-sourcegen`](../example-sourcegen) folder.
It uses Gradle-specific `buildSrc` folder to create custom gradle task.
