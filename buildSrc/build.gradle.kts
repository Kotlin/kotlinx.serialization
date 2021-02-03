import org.jetbrains.kotlin.gradle.plugin.*
import java.util.*

plugins {
    `kotlin-dsl`
}

repositories {
    jcenter()
}

kotlinDslPluginOptions {
    experimentalWarning.set(false)
}
