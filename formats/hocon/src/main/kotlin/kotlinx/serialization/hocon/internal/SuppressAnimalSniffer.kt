package kotlinx.serialization.hocon.internal

/**
 * Suppresses Animal Sniffer plugin errors for certain methods.
 * Such methods include references to Java 8 methods that are not
 * available in Android API, but can be desugared by R8.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
internal annotation class SuppressAnimalSniffer
