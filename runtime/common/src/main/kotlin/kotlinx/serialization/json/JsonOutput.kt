package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.json.internal.*
import kotlin.jvm.*

// Public visibility to allow casting in user-code to call [writeTree]
@Suppress("RedundantVisibilityModifier")
public class JsonOutput private constructor(
    private val composer: Composer, private val json: Json, private val mode: WriteMode,
    private val modeReuseCache: Array<JsonOutput?>
) : ElementValueEncoder() {

    internal constructor(
        output: StringBuilder, json: Json, mode: WriteMode,
        modeReuseCache: Array<JsonOutput?>
    ) : this(Composer(output, json), json, mode, modeReuseCache)

    // Forces serializer to wrap all values into quotes
    private var forceQuoting: Boolean = false

    init {
        context = json.context
        val i = mode.ordinal
        if (modeReuseCache[i] !== null || modeReuseCache[i] !== this)
            modeReuseCache[i] = this
    }

    /**
     * Doesn't respect indentation or quoting settings
     */
    fun writeTree(tree: JsonElement) {
        composer.sb.append(tree.toString())
    }

    override fun shouldEncodeElementDefault(desc: SerialDescriptor, index: Int): Boolean {
        return json.encodeDefaults
    }

    override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeEncoder {
        val newMode = switchMode(desc, typeParams)
        if (newMode.begin != INVALID) { // entry
            composer.print(newMode.begin)
            composer.indent()
        }

        if (mode == newMode) return this

        val cached = modeReuseCache[newMode.ordinal]
        if (cached != null) {
            return cached
        }

        return JsonOutput(composer, json, newMode, modeReuseCache)
    }

    override fun endStructure(desc: SerialDescriptor) {
        if (mode.end != INVALID) {
            composer.unIndent()
            composer.nextItem()
            composer.print(mode.end)
        }
    }

    override fun encodeElement(desc: SerialDescriptor, index: Int): Boolean {
        when (mode) {
            WriteMode.LIST -> {
                if (!composer.writingFirst)
                    composer.print(COMMA)
                composer.nextItem()
            }
            WriteMode.MAP -> {
                if (!composer.writingFirst) {
                    if (index % 2 == 0) composer.print(COMMA) else composer.print(
                        COLON
                    )
                }
                composer.nextItem()
            }
            WriteMode.ENTRY -> throw IllegalStateException("Entry is deprecated")
            WriteMode.POLY -> {
                if (index == 0)
                    forceQuoting = true
                if (index == 1) {
                    composer.print(if (mode == WriteMode.ENTRY) COLON else COMMA)
                    composer.space()
                    forceQuoting = false
                }
            }
            else -> {
                if (!composer.writingFirst)
                    composer.print(COMMA)
                composer.nextItem()
                encodeString(desc.getElementName(index))
                composer.print(COLON)
                composer.space()
            }
        }
        return true
    }

    override fun encodeNull() {
        composer.print(NULL)
    }

    override fun encodeBoolean(value: Boolean) {
        if (forceQuoting) encodeString(value.toString()) else composer.print(value)
    }

    override fun encodeByte(value: Byte) {
        if (forceQuoting) encodeString(value.toString()) else composer.print(value)
    }

    override fun encodeShort(value: Short) {
        if (forceQuoting) encodeString(value.toString()) else composer.print(value)
    }

    override fun encodeInt(value: Int) {
        if (forceQuoting) encodeString(value.toString()) else composer.print(value)
    }

    override fun encodeLong(value: Long) {
        if (forceQuoting) encodeString(value.toString()) else composer.print(value)
    }

    override fun encodeFloat(value: Float) {
        if (json.strictMode && !value.isFinite()) {
            throw JsonInvalidValueInStrictModeException(value)
        }

        if (forceQuoting) encodeString(value.toString()) else composer.print(value)
    }

    override fun encodeDouble(value: Double) {
        if (json.strictMode && !value.isFinite()) {
            throw JsonInvalidValueInStrictModeException(value)
        }

        if (forceQuoting) encodeString(value.toString()) else composer.print(value)
    }

    override fun encodeChar(value: Char) {
        encodeString(value.toString())
    }

    override fun encodeString(value: String) {
        if (json.unquoted && !shouldBeQuoted(value)) {
            composer.print(value)
        } else {
            composer.printQuoted(value)
        }
    }

    override fun encodeEnum(enumDescription: EnumDescriptor, ordinal: Int) {
        encodeString(enumDescription.getElementName(ordinal))
    }

    override fun encodeValue(value: Any) {
        if (json.strictMode) super.encodeValue(value) else
            encodeString(value.toString())
    }

    internal class Composer(@JvmField internal val sb: StringBuilder, private val json: Json) {
        private var level = 0
        var writingFirst = true
            private set

        fun indent() {
            writingFirst = true; level++
        }

        fun unIndent() {
            level--
        }

        fun nextItem() {
            writingFirst = false
            if (json.indented) {
                print("\n")
                repeat(level) { print(json.indent) }
            }
        }

        fun space() {
            if (json.indented)
                print(' ')
        }

        fun print(v: Char) = sb.append(v)
        fun print(v: String) = sb.append(v)
        fun print(v: Float) = sb.append(v)
        fun print(v: Double) = sb.append(v)
        fun print(v: Byte) = sb.append(v)
        fun print(v: Short) = sb.append(v)
        fun print(v: Int) = sb.append(v)
        fun print(v: Long) = sb.append(v)
        fun print(v: Boolean) = sb.append(v)
        fun printQuoted(value: String): Unit = sb.printQuoted(value)
    }
}
