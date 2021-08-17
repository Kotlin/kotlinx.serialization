package kotlinx.serialization.json.internal

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*

internal class JsonPath {
    /*
     * Serial descriptor OR map key.
     * Map key can also implement SD, so it also has '-2' as current index
     */
    private var currentObjectPath = arrayOfNulls<Any?>(8)
    private var currentIndices = IntArray(8)
    private var currentDepth = -1

    fun pushObject(sd: SerialDescriptor) {
        if (++currentDepth == currentObjectPath.size) {
            resize()
        }
        currentObjectPath[currentDepth] = sd
        currentIndices[currentDepth] = -1
    }

    fun updateCurrentMapKey(key: Any?) {
        if (currentIndices[currentDepth] != -2 && ++currentDepth == currentObjectPath.size) {
            resize()
        }
        currentObjectPath[currentDepth] = key
        currentIndices[currentDepth] = -2
    }

    fun updateCurrentIndex(index: Int) {
        currentIndices[currentDepth] = index
    }

    fun pop() {
        // When we are ending map, we pop the last key and the outer field as well
        if (currentIndices[currentDepth] == -2) {
            currentIndices[currentDepth] = -1
            currentDepth--
        }
        // Against top-level maps
        if (currentDepth != -1) currentDepth--
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun currentPath(): String {
        if (currentDepth == -1) return ""
        return buildString {
            append("Root type: ")
            append(currentObjectPath[0])
            append(", leaf type: ")
            append(currentObjectPath[currentDepth])
            append(", \npath: ")
            repeat(currentDepth + 1) {
                val element = currentObjectPath[it]
                if (element is SerialDescriptor) {
                    if (element.kind == StructureKind.LIST) {
                        append("[")
                        append(currentIndices[it])
                        append("]")
                    } else {
                        val idx = currentIndices[it]
                        if (idx >= 0) {
                            append(if (it == 0) "$" else ".")
                            append(element.getElementName(idx))
                        }
                    }
                } else {
                    append(".")
                    append(element)
                }
            }
        }
    }

    private fun resize() {
        val newSize = currentDepth * 2
        currentObjectPath = currentObjectPath.copyOf(newSize)
        currentIndices = currentIndices.copyOf(newSize)
    }
}
