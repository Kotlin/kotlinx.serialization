package kotlinx.serialization.json.internal

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.internal.*

/**
 * Internal representation of the current JSON path.
 * It is stored as the array of serial descriptors (for regular classes)
 * and `Any?` in case of Map keys.
 *
 * Example of the state when decoding the list
 * ```
 * class Foo(val a: Int, val l: List<String>)
 *
 * // {"l": ["a", "b", "c"] }
 *
 * Current path when decoding array elements:
 * Foo.descriptor, List(String).descriptor
 * 1 (index of the 'l'), 2 (index of currently being decoded "c")
 * ```
 */
internal class JsonPath {

    // Tombstone indicates that we are within a map, but the map key is currently being decoded.
    // It is also used to overwrite a previous map key to avoid memory leaks and misattribution.
    object Tombstone

    /*
     * Serial descriptor, map key or the tombstone for map key
     */
    private var currentObjectPath = arrayOfNulls<Any?>(8)
    /*
     * Index is a small state-machine used to determine the state of the path:
     * >=0 -> index of the element being decoded with the outer class currentObjectPath[currentDepth]
     * -1 -> nested elements are not yet decoded
     * -2 -> the map is being decoded and both its descriptor AND the last key were added to the path.
     *
     * -2 is effectively required to specify that two slots has been claimed and both should be
     * cleaned up when the decoding is done.
     * The cleanup is essential in order to avoid memory leaks for huge strings and structured keys.
     */
    private var indicies = IntArray(8) { -1 }
    private var currentDepth = -1

    // Invoked when class is started being decoded
    fun pushDescriptor(sd: SerialDescriptor) {
        val depth = ++currentDepth
        if (depth == currentObjectPath.size) {
            resize()
        }
        currentObjectPath[depth] = sd
    }

    // Invoked when index-th element of the current descriptor is being decoded
    fun updateDescriptorIndex(index: Int) {
        indicies[currentDepth] = index
    }

    /*
     * For maps we cannot use indicies and should use the key as an element of the path instead.
     * The key can be even an object (e.g. in a case of 'allowStructuredMapKeys') where
     * 'toString' is way too heavy or have side-effects.
     * For that we are storing the key instead.
     */
    fun updateCurrentMapKey(key: Any?) {
        // idx != -2 -> this is the very first key being added
        if (indicies[currentDepth] != -2 && ++currentDepth == currentObjectPath.size) {
            resize()
        }
        currentObjectPath[currentDepth] = key
        indicies[currentDepth] = -2
    }

    /** Used to indicate that we are in the process of decoding the key itself and can't specify it in path */
    fun resetCurrentMapKey() {
        if (indicies[currentDepth] == -2) {
            currentObjectPath[currentDepth] = Tombstone
        }
    }

    fun popDescriptor() {
        // When we are ending map, we pop the last key and the outer field as well
        val depth = currentDepth
        if (indicies[depth] == -2) {
            indicies[depth] = -1
            currentDepth--
        }
        // Guard against top-level maps
        if (currentDepth != -1) {
            // No need to clean idx up as it was already cleaned by updateDescriptorIndex(DECODE_DONE)
            currentDepth--
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun getPath(): String {
        return buildString {
            append("$")
            repeat(currentDepth + 1) {
                val element = currentObjectPath[it]
                if (element is SerialDescriptor) {
                    if (element.kind == StructureKind.LIST) {
                        if (indicies[it] != -1) {
                            append("[")
                            append(indicies[it])
                            append("]")
                        }
                    } else {
                        val idx = indicies[it]
                        // If an actual element is being decoded
                        if (idx >= 0) {
                            append(".")
                            append(element.getElementName(idx))
                        }
                    }
                } else if (element !== Tombstone) {
                    append("[")
                    // All non-indicies should be properly quoted by JsonPath convention
                    append("'")
                    // Else -- map key
                    append(element)
                    append("'")
                    append("]")
                }
            }
        }
    }


    @OptIn(ExperimentalSerializationApi::class)
    private fun prettyString(it: Any?) = (it as? SerialDescriptor)?.serialName ?: it.toString()

    private fun resize() {
        val newSize = currentDepth * 2
        currentObjectPath = currentObjectPath.copyOf(newSize)
        indicies = indicies.copyOf(newSize)
    }

    override fun toString(): String = getPath()
}
