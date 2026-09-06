package ru.lewis.battlepass.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "battlepass_unlocked_levels")
class PlayerUnlockedLevelEntity(
    @Id
    @Column(name = "id", length = 64)
    var id: String = "",

    @Column(name = "uuid", length = 36)
    var uuid: String = "",

    @Column(name = "level_number")
    var levelNumber: Int = 0,
) {
    constructor() : this("")

    companion object {
        fun fromModel(unlocked: PlayerUnlockedLevel): PlayerUnlockedLevelEntity = PlayerUnlockedLevelEntity(
            id = "${unlocked.uuid}-${unlocked.levelNumber}",
            uuid = unlocked.uuid.toString(),
            levelNumber = unlocked.levelNumber,
        )
    }
}
