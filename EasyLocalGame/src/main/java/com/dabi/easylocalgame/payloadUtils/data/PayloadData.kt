package com.dabi.easylocalgame.payloadUtils.data


enum class ClientPayloadType {
    ESTABLISH_CONNECTION,
    ACTION_DISCONNECTED,
}


enum class ServerPayloadType {
    CLIENT_CONNECTED,
    ROOM_IS_FULL,

    UPDATE_PLAYER_STATE,
    UPDATE_GAME_STATE
}