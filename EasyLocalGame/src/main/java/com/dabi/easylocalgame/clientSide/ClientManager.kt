package com.dabi.easylocalgame.clientSide

import android.util.Log
import androidx.lifecycle.ViewModel
import com.dabi.easylocalgame.clientSide.data.ClientState
import com.dabi.easylocalgame.clientSide.data.ConnectionStatusEnum
import com.dabi.easylocalgame.clientSide.data.IPlayerConnectionState
import com.dabi.easylocalgame.payloadUtils.data.ClientPayloadType
import com.dabi.easylocalgame.payloadUtils.data.ServerPayloadType
import com.dabi.easylocalgame.payloadUtils.fromPayload
import com.dabi.easylocalgame.payloadUtils.toClientPayload
import com.dabi.easylocalgame.serverSide.data.ServerType
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update


abstract class PlayerViewmodelTemplate(
    private val connectionsClient: ConnectionsClient,
): ViewModel(){
    /**
     * Use this function to react on [ServerAction]s in your gameState.
     *
     * [ServerAction.PayloadAction] is mainly for your own [ServerPayloadType] events,
     *  but you can use it however you want.
     */
    abstract fun serverAction(serverAction: ServerAction)
    val clientManager: ClientManager by lazy {
        ClientManager(connectionsClient, this::serverAction)
    }
}


/**
 * Actions used by server to communicate with client.
 */
sealed class ServerAction{
    data class UpdateGameState(val payload: Payload): ServerAction()
    data class UpdatePlayerState(val payload: Payload): ServerAction()
    data class PayloadAction(val payload: Payload): ServerAction()
}


class ClientManager(
    private val connectionsClient: ConnectionsClient,
    private val serverAction: (ServerAction) -> Unit,
) {
    private val _clientState = MutableStateFlow(ClientState())
    val clientState = _clientState.asStateFlow()

    private lateinit var playerConnectionState: IPlayerConnectionState
    fun connect(packageName: String, playerConnectionState: IPlayerConnectionState){
        if (
            _clientState.value.connectionStatus == ConnectionStatusEnum.CONNECTING ||
            _clientState.value.connectionStatus == ConnectionStatusEnum.CONNECTED ||
            _clientState.value.connectionStatus == ConnectionStatusEnum.CONNECTION_ESTABLISHED
        ) {
            return
        }

        this.playerConnectionState = playerConnectionState
        startDiscovery(packageName, playerConnectionState.nickname)
    }
    private fun establishConnection(){
        val clientPayload = toClientPayload(ClientPayloadType.ESTABLISH_CONNECTION, playerConnectionState)
        sendPayload(clientPayload)

        _clientState.update { it.copy(
            connectionStatus = ConnectionStatusEnum.CONNECTION_ESTABLISHED
        ) }

        connectionsClient.stopDiscovery()
    }

    fun disconnect(){
        _clientState.update { it.copy(connectionStatus = ConnectionStatusEnum.DISCONNECTED) }

        connectionsClient.disconnectFromEndpoint(clientState.value.serverID)
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
    }

    fun sendPayload(clientPayload: Payload){
        connectionsClient.sendPayload(
            _clientState.value.serverID,
            clientPayload
        )
    }

    private fun startDiscovery(packageName: String, nickname: String) {
        val discoveryOptions = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_STAR).build()
        connectionsClient.stopDiscovery()

        connectionsClient.startDiscovery(packageName, endpointDiscoveryCallback(nickname), discoveryOptions)
            .addOnSuccessListener {
                Log.i("ClientManager.kt", "CLIENT DISCOVERY READY")
                _clientState.update { it.copy(
                    connectionStatus = ConnectionStatusEnum.CONNECTING
                ) }
            }
            .addOnFailureListener {
                Log.e("ClientManager.kt", "CLIENT DISCOVERY FAILURE " + it.message)
                _clientState.update { state -> state.copy(
                    connectionStatus = ConnectionStatusEnum.CONNECTING_FAILED
                ) }
            }
    }

    private val endpointDiscoveryCallback: (String) -> EndpointDiscoveryCallback = { nickname ->
        object : EndpointDiscoveryCallback() {
            override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                Log.i("ClientManager.kt", "CLIENT $nickname is requesting connection on: ${info.endpointName} + $endpointId")

                connectionsClient.requestConnection(
                    nickname,
                    endpointId,
                    connectionLifecycleCallback
                ).addOnSuccessListener {
                    Log.i("ClientManager.kt", "CLIENT Successfully requested a connection")
                }.addOnFailureListener {
                    Log.e("ClientManager.kt", "CLIENT Failed to request the connection")
                    _clientState.update { it.copy(
                        connectionStatus = ConnectionStatusEnum.CONNECTING_FAILED
                    ) }
                }
            }

            override fun onEndpointLost(endpointId: String) {
                _clientState.update { it.copy(
                    connectionStatus = ConnectionStatusEnum.ENDPOINT_LOST
                ) }
                Log.i("ClientManager.kt", "CLIENT onEndpointLost $endpointId")
            }
        }
    }

    private var connectionLifecycleCallback: ConnectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, resolution: ConnectionResolution) {
            when (resolution.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    _clientState.update { it.copy(
                        serverID = endpointId,
                        connectionStatus = ConnectionStatusEnum.CONNECTED
                    ) }

                    establishConnection()
                }
                else -> {
                    if (resolution.status.statusCode == ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED){
                        _clientState.update { it.copy(
                            connectionStatus = ConnectionStatusEnum.CONNECTING_REJECTED
                        ) }
                    } else{
                        _clientState.update { it.copy(
                            connectionStatus = ConnectionStatusEnum.CONNECTING_FAILED
                        ) }
                    }
                    Log.e("ClientManager.kt", "CLIENT status code ${resolution.status.statusCode}: ${resolution.status.statusMessage}")
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            _clientState.update { it.copy(
                connectionStatus = ConnectionStatusEnum.DISCONNECTED
            ) }
        }
    }

    private val payloadCallback: PayloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            try {
                val result: Pair<ServerPayloadType, Any?> = fromPayload(payload, null)

                val serverPayloadType = result.first
                val data = result.second
                when(serverPayloadType){
                    ServerPayloadType.CLIENT_CONNECTED -> {
                        _clientState.update { it.copy(
                            serverType = ServerType.valueOf(data.toString()),
                        ) }
                    }
                    ServerPayloadType.ROOM_IS_FULL -> {
                        _clientState.update { it.copy(
                            connectionStatus = ConnectionStatusEnum.ROOM_IS_FULL
                        ) }
                    }

                    ServerPayloadType.UPDATE_PLAYER_STATE -> {
                        serverAction(ServerAction.UpdatePlayerState(payload))
                    }
                    ServerPayloadType.UPDATE_GAME_STATE -> {
                        serverAction(ServerAction.UpdateGameState(payload))
                    }
                }
            } catch (_: Exception){
                serverAction(ServerAction.PayloadAction(payload))
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) { }
    }
}