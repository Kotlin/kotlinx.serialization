package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.json.internal.*

// Public visibility to allow casting in user-code to call [readAsTree]
@Suppress("RedundantVisibilityModifier")
public class JsonInput internal constructor(private val json: Json, private val mode: WriteMode,
                                            private val parser: JsonParser) : ElementValueDecoder() {
    private var curIndex = -1
    private var entryIndex = 0

    init {
        context = json.context
    }

    fun readAsTree(): JsonElement = JsonTreeParser(parser).read()

    override val updateMode: UpdateMode
        get() = json.updateMode

    override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder {
        val newMode = switchMode(desc, typeParams)
        if (newMode.begin != INVALID) {
            parser.requireTokenClass(newMode.beginTc) { "Expected '${newMode.begin}, kind: ${desc.kind}'" }
            parser.nextToken()
        }
        return when (newMode) {
            WriteMode.LIST, WriteMode.MAP, WriteMode.POLY -> JsonInput(
                json,
                newMode,
                parser
            ) // need fresh cur index
            else -> if (mode == newMode) this else
                JsonInput(json, newMode, parser) // todo: reuse instance per mode
        }
    }

    override fun endStructure(desc: SerialDescriptor) {
        if (mode.end != INVALID) {
            parser.requireTokenClass(mode.endTc) { "Expected '${mode.end}'" }
            parser.nextToken()
        }
    }

    override fun decodeNotNullMark(): Boolean {
        return parser.tokenClass != TC_NULL
    }

    override fun decodeNull(): Nothing? {
        parser.requireTokenClass(TC_NULL) { "Expected 'null' literal" }
        parser.nextToken()
        return null
    }

    override fun decodeElementIndex(desc: SerialDescriptor): Int {
        while (true) {
            if (parser.tokenClass == TC_COMMA) parser.nextToken()
            when (mode) {
                WriteMode.LIST -> {
                    return if (!parser.canBeginValue) CompositeDecoder.READ_DONE else ++curIndex
                }
                WriteMode.MAP -> {
                    if (curIndex % 2 == 0 && parser.tokenClass == TC_COLON) parser.nextToken()
                    return if (!parser.canBeginValue) CompositeDecoder.READ_DONE else ++curIndex
                }
                WriteMode.POLY -> {
                    return when (entryIndex++) {
                        0 -> 0
                        1 -> 1
                        else -> {
                            entryIndex = 0
                            CompositeDecoder.READ_DONE
                        }
                    }
                }
                WriteMode.ENTRY -> {
                    return when (entryIndex++) {
                        0 -> 0
                        1 -> {
                            parser.requireTokenClass(TC_COLON) { "Expected ':'" }
                            parser.nextToken()
                            1
                        }
                        else -> {
                            entryIndex = 0
                            CompositeDecoder.READ_DONE
                        }
                    }
                }
                else -> {
                    if (!parser.canBeginValue) return CompositeDecoder.READ_DONE
                    val key = parser.takeString()
                    parser.requireTokenClass(TC_COLON) { "Expected ':'" }
                    parser.nextToken()
                    val ind = desc.getElementIndex(key)
                    if (ind != CompositeDecoder.UNKNOWN_NAME)
                        return ind
                    if (json.strictMode)
                        throw JsonUnknownKeyException(key)
                    else
                        parser.skipElement()
                }
            }
        }
    }

    override fun decodeBoolean(): Boolean = parser.takeString().run { if (json.strictMode) toBooleanStrict() else toBoolean() }
    override fun decodeByte(): Byte = parser.takeString().toByte()
    override fun decodeShort(): Short = parser.takeString().toShort()
    override fun decodeInt(): Int = parser.takeString().toInt()
    override fun decodeLong(): Long = parser.takeString().toLong()
    override fun decodeFloat(): Float = parser.takeString().toFloat()
    override fun decodeDouble(): Double = parser.takeString().toDouble()
    override fun decodeChar(): Char = parser.takeString().single()
    override fun decodeString(): String = parser.takeString()
    override fun decodeEnum(enumDescription: EnumDescriptor): Int = enumDescription.getElementIndex(parser.takeString())
}
