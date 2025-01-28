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

/**
 * List of enum types that represent the possible states when trying to establish advertising with the server device:
 *
 * - [NONE]: No advertising has been initialized.
 * - [ADVERTISING_FAILED]: The advertising failed."
 * - [ADVERTISING]: The advertising is active and clients can connect.
 * - [ACTIVE]: The server is active but not advertising anymore - clients can't connect.
 * - [CLOSED]: The server was closed - you can use it for example to change view from Game to Menu or landing page.
 */
enum class ServerStatusEnum{
    NONE,
    ADVERTISING_FAILED,
    ADVERTISING,
    ACTIVE,
    CLOSED
}