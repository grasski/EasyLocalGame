package com.dabi.easylocalgame.serverSide.data

import com.dabi.easylocalgame.clientSide.data.IPlayerState

interface IGameState{
    val players: Map<String, IPlayerState>  // String = endpointID
}

interface IGameEvents{
    data object StartGame: IGameEvents
    data object CloseGame: IGameEvents
    data object StopAdvertising: IGameEvents
}