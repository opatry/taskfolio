package net.opatry.google.keep.entity

import kotlinx.datetime.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object ProtobufTimestampSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("google.protobuf.Timestamp", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant) {
        // Serialize the Instant as an ISO 8601 string (RFC 3339)
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Instant {
        // Deserialize the ISO 8601 string back to Instant
        return Instant.parse(decoder.decodeString())
    }
}