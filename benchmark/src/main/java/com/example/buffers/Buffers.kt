package com.example.buffers

import kotlinx.io.core.BytePacketBuilder
import kotlinx.io.core.ByteReadPacket
import kotlinx.serialization.json.BufferEngine
import okio.Buffer
import okio.BufferedSink

class OkioEngine(private val buf: Buffer = Buffer()) : BufferEngine<BufferedSink> {

    override fun append(csq: String) {
        buf.writeUtf8(csq)
    }

    override fun append(csq: String, start: Int, end: Int) {
        buf.writeUtf8(csq, start, end)
    }

    override fun print(v: Char) {
        buf.writeUtf8CodePoint(v.toInt())
    }

    override fun append(obj: Any) {
        buf.writeUtf8(obj.toString())
    }

    override fun print(v: String) {
        buf.writeUtf8(v)
    }

    override fun print(v: Short) {
        buf.writeShort(v.toInt())
    }

    override fun print(v: Int) {
        buf.writeInt(v)
    }

    override fun result(): BufferedSink {
        return buf
    }
}

class KxioEngine(private val builder: BytePacketBuilder = BytePacketBuilder()) : BufferEngine<ByteReadPacket> {

    override fun append(csq: String) {
        builder.append(csq)
    }

    override fun append(csq: String, start: Int, end: Int) {
        builder.append(csq, start, end)
    }

    override fun append(obj: Any) {
        builder.writeStringUtf8(obj.toString())
    }

    override fun print(v: Char) {
        builder.append(v)
    }

    override fun print(v: String) {
        builder.writeStringUtf8(v)
    }

    override fun print(v: Short) {
        builder.writeShort(v)
    }

    override fun print(v: Int) {
        builder.writeInt(v)
    }

    override fun result(): ByteReadPacket {
        return builder.build()
    }
}
