package com.dabi.easylocalgame.clientSide.data


interface IPlayerConnectionState{
    var nickname: String
    val avatarId: Int?  // @RawRes
}
data class PlayerConnectionState(
    override var nickname: String,
    override val avatarId: Int?
): IPlayerConnectionState


interface IPlayerState{
    val nickname: String
    val id: String
    val isServer: Boolean

    val avatarId: Int?  // @RawRes
}
