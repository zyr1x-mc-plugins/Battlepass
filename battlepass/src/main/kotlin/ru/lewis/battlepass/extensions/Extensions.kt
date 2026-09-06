package ru.lewis.battlepass.extensions

import io.github.blackbaroness.durationserializer.DurationSerializer
import io.github.blackbaroness.durationserializer.format.DurationFormat
import net.kyori.adventure.util.Ticks
import java.time.Duration

val Duration.inTicks: Long
    get() {
        return this.toMillis() / Ticks.SINGLE_TICK_DURATION_MS
    }

fun Duration.format(durationFormat: DurationFormat): String {
    return DurationSerializer.serialize(this.withNanos(0), durationFormat)
}

fun Duration.toMinutesSeconds(): String {
    val seconds = seconds.coerceAtLeast(0)

    val minutesPart = seconds / 60
    val secondsPart = seconds % 60

    return "%02d:%02d".format(
        minutesPart,
        secondsPart
    )
}
