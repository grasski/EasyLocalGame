package com.dabi.easylocalgame.serverSide.data


data class ServerConfiguration(
    val serverType: ServerType,
    val maximumConnections: Int,
    val serverAsPlayerName: String = "server",
)
enum class ServerType{
    IS_PLAYER,
    IS_TABLE
}


data class ServerState(
    var serverStatus: ServerStatusEnum = ServerStatusEnum.NONE,
    var serverType: ServerType = ServerType.IS_TABLE,

    var connectedClients: List<String> = emptyList()    // endpointID = string
)

enum class ServerStatusEnum{
    NONE,
    ADVERTISING_FAILED,
    ADVERTISING,
    ACTIVE, // After connection is established with all players, we can turn off the advertising
    CLOSED
}