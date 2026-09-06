package ru.lewis.battlepass.model

import java.util.UUID

data class PlayerQuestProgress(
    val uuid: UUID,
    val questId: String,
    val questType: String,
    val required: Int,
    var current: Int = 0,
    var completed: Boolean = false,
    // Index into BattlePassConfigService.questConfigs at the moment this
    // quest was rolled. `questType` alone (e.g. "BREAK_BLOCK") is not a
    // unique identifier when several quest variants share the same type
    // (different materials/entities/required/xp) - looking things up by
    // type only picks whichever variant happens to be first in the config
    // list, which used to cause the displayed quest (name/amount/xp) to
    // mismatch the variant actually rolled for the player. -1 means
    // "unknown", used as a fallback for progress saved before this field
    // existed.
    val configIndex: Int = -1,
)
