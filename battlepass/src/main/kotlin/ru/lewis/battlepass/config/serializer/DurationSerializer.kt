package ru.lewis.battlepass.config.serializer

import com.google.inject.Inject
import io.github.blackbaroness.durationserializer.DurationFormats
import io.github.blackbaroness.durationserializer.DurationSerializer
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.set
import org.spongepowered.configurate.serialize.TypeSerializer
import ru.lewis.battlepass.extensions.format
import java.lang.reflect.Type
import java.time.Duration

class DurationSerializer @Inject constructor() : TypeSerializer<Duration> {

    override fun deserialize(type: Type, node: ConfigurationNode): Duration? {
        return node.string?.let { DurationSerializer.deserialize(it) }
    }

    override fun serialize(type: Type, obj: Duration?, node: ConfigurationNode) {
        node.set(String::class, obj?.format(DurationFormats.mediumLengthRussian()))
    }
}
