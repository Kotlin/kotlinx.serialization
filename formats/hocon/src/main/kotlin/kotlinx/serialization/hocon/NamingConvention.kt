package kotlinx.serialization.hocon

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*

private val NAMING_CONVENTION_REGEX by lazy { "[A-Z]".toRegex() }

@OptIn(ExperimentalSerializationApi::class)
internal fun SerialDescriptor.getConventionElementName(index: Int, useConfigNamingConvention: Boolean): String {
    val originalName = getElementName(index)
    return if (!useConfigNamingConvention) originalName
    else originalName.replace(NAMING_CONVENTION_REGEX) { "-${it.value.lowercase()}" }
}
