package com.dabi.easylocalgame.clientSide.data

import com.dabi.easylocalgame.serverSide.data.ServerType


data class ClientState(
    var connectionStatus: ConnectionStatusEnum = ConnectionStatusEnum.NONE,

    var serverID: String = "",
    var serverType: ServerType = ServerType.IS_TABLE
)

/**
 * List of enum types that represent the possible states when trying to establish a connection with the server:
 *
 * - [NONE]: No connection has been initialized.
 * - [CONNECTING]: The user clicked on "connect."
 * - [CONNECTING_FAILED]: The connection attempt failed.
 * - [CONNECTING_REJECTED]: The connection was rejected, usually because the room is full.
 * - [ENDPOINT_LOST]: The client lost the connection to the server.
 * - [ROOM_IS_FULL]: The room is full.
 * - [CONNECTED]: The connection was successful.
 * - [CONNECTION_ESTABLISHED]: The connection was established after [CONNECTED]. The client sent their information from [IPlayerConnectionState] to the server.
 * - [DISCONNECTED]: The connection was closed - you can use it for example to change view from Game to Menu or landing page.
 */
enum class ConnectionStatusEnum{
    NONE,
    CONNECTING,
    CONNECTING_FAILED,
    CONNECTING_REJECTED,
    ENDPOINT_LOST,
    ROOM_IS_FULL,
    CONNECTED,
    CONNECTION_ESTABLISHED,
    DISCONNECTED
}
