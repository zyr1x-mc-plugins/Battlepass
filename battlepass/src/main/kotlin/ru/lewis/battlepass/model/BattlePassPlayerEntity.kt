package ru.lewis.battlepass.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "battlepass_players")
class BattlePassPlayerEntity(
    @Id
    @Column(name = "uuid", length = 36)
    var uuid: String = "",

    @Column(name = "xp")
    var xp: Long = 0,

    @Column(name = "is_premium")
    var isPremium: Boolean = false,

    @Column(name = "last_daily_reset")
    var lastDailyReset: Long = 0,
) {
    constructor() : this("")

    fun toModel(): BattlePassPlayer = BattlePassPlayer(
        uuid = UUID.fromString(uuid),
        xp = xp,
        isPremium = isPremium,
        lastDailyReset = lastDailyReset,
    )

    companion object {
        fun fromModel(player: BattlePassPlayer): BattlePassPlayerEntity = BattlePassPlayerEntity(
            uuid = player.uuid.toString(),
            xp = player.xp,
            isPremium = player.isPremium,
            lastDailyReset = player.lastDailyReset,
        )
    }
}
