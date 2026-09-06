package ru.lewis.battlepass.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "battlepass_quest_progress")
class PlayerQuestProgressEntity(
    @Id
    @Column(name = "id")
    var id: String = "",

    @Column(name = "uuid", length = 36)
    var uuid: String = "",

    @Column(name = "quest_id")
    var questId: String = "",

    @Column(name = "quest_type")
    var questType: String = "",

    @Column(name = "required")
    var required: Int = 0,

    @Column(name = "current")
    var current: Int = 0,

    @Column(name = "completed")
    var completed: Boolean = false,

    @Column(name = "config_index")
    var configIndex: Int = -1,
) {
    constructor() : this("")

    fun toModel(): PlayerQuestProgress = PlayerQuestProgress(
        uuid = UUID.fromString(uuid),
        questId = questId,
        questType = questType,
        required = required,
        current = current,
        completed = completed,
        configIndex = configIndex,
    )

    companion object {
        fun fromModel(progress: PlayerQuestProgress): PlayerQuestProgressEntity = PlayerQuestProgressEntity(
            id = "${progress.uuid}-${progress.questId}",
            uuid = progress.uuid.toString(),
            questId = progress.questId,
            questType = progress.questType,
            required = progress.required,
            current = progress.current,
            completed = progress.completed,
            configIndex = progress.configIndex,
        )
    }
}
