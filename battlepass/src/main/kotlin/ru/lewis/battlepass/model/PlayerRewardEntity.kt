package ru.lewis.battlepass.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "battlepass_claimed_rewards")
class PlayerRewardEntity(
    @Id
    @Column(name = "id")
    var id: String = "",

    @Column(name = "uuid", length = 36)
    var uuid: String = "",

    @Column(name = "reward_number")
    var rewardNumber: Int = 0,

    @Column(name = "is_premium")
    var isPremium: Boolean = false,
) {
    constructor() : this("")

    fun toModel(): PlayerReward = PlayerReward(
        uuid = UUID.fromString(uuid),
        rewardNumber = rewardNumber,
        isPremium = isPremium,
    )

    companion object {
        fun fromModel(reward: PlayerReward): PlayerRewardEntity = PlayerRewardEntity(
            id = "${reward.uuid}-${reward.rewardNumber}-${reward.isPremium}",
            uuid = reward.uuid.toString(),
            rewardNumber = reward.rewardNumber,
            isPremium = reward.isPremium,
        )
    }
}
