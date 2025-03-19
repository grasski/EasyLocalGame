package com.dabi.easylocalgame.serverSide

import android.util.Log
import androidx.lifecycle.ViewModel
import com.dabi.easylocalgame.payloadUtils.data.ClientPayloadType
import com.dabi.easylocalgame.payloadUtils.data.ServerPayloadType
import com.dabi.easylocalgame.payloadUtils.fromPayload
import com.dabi.easylocalgame.payloadUtils.toServerPayload
import com.dabi.easylocalgame.serverSide.data.ServerConfiguration
import com.dabi.easylocalgame.serverSide.data.ServerState
import com.dabi.easylocalgame.serverSide.data.ServerStatusEnum
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update


abstract class ServerViewmodelTemplate(
    private val connectionsClient: ConnectionsClient,
): ViewModel() {
    /**
     * Use this function to react on [ClientAction]s in your gameState.
     *
     * [ClientAction.PayloadAction] is mainly for your own [ClientPayloadType] events,
     *  but you can use it however you want.
     */
    abstract fun clientAction(clientAction: ClientAction)
    val serverManager: ServerManager by lazy {
        ServerManager(connectionsClient, this::clientAction)
    }
}

/**
 * Actions used by client to communicate with server.
 */
sealed class ClientAction{
    data class EstablishConnection(val endpointID: String, val payload: Payload): ClientAction()
    data class Disconnect(val endpointID: String): ClientAction()
    data class PayloadAction(val endpointID: String, val payload: Payload): ClientAction()
}


class ServerManager(
    val connectionsClient: ConnectionsClient,

    private val clientAction: (ClientAction) -> Unit,
) {
    private val _serverState = MutableStateFlow(ServerState())
    val serverState = _serverState.asStateFlow()

    private lateinit var serverConfiguration: ServerConfiguration
    fun startServer(packageName: String, serverConfiguration: ServerConfiguration){
        if (_serverState.value.serverStatus == ServerStatusEnum.ADVERTISING || _serverState.value.serverStatus == ServerStatusEnum.ACTIVE){
            return
        }

        this.serverConfiguration = serverConfiguration
        _serverState.update { it.copy(
            serverType = this.serverConfiguration.serverType
        ) }
        startAdvertising(packageName)
    }
    fun closeServer(){
        connectionsClient.stopAllEndpoints()
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()

        _serverState.update { ServerState(serverStatus = ServerStatusEnum.CLOSED) }
    }
    fun stopAdvertising(){
        connectionsClient.stopAdvertising()
        _serverState.update { it.copy(
                serverStatus = ServerStatusEnum.ACTIVE
            )
        }
    }


    private fun clientConnected(endpointID: String){
        if (_serverState.value.connectedClients.size >= serverConfiguration.maximumConnections){
            Log.i("ServerManager.kt", "Reached maximum connections!")

            val payload = toServerPayload(ServerPayloadType.ROOM_IS_FULL, null)
            sendPayload(endpointID, payload)

            connectionsClient.disconnectFromEndpoint(endpointID)
            return
        }

        _serverState.update { it.copy(
            connectedClients = it.connectedClients.plus(endpointID)
        )}

        val payload = toServerPayload(ServerPayloadType.CLIENT_CONNECTED, _serverState.value.serverType)
        sendPayload(endpointID, payload)
    }
    private fun clientDisconnected(endpointID: String){
        _serverState.update { it.copy(
            connectedClients = it.connectedClients.filter { clients -> clients != endpointID }
        ) }

        clientAction(ClientAction.Disconnect(endpointID))
    }


    fun sendPayload(clientID: String, serverPayload: Payload){
        if (_serverState.value.connectedClients.contains(clientID)){
            connectionsClient.sendPayload(
                clientID,
                serverPayload
            )
        }
    }
    fun sendPayload(serverPayload: Payload){
        if (_serverState.value.connectedClients.isNotEmpty()){
            connectionsClient.sendPayload(_serverState.value.connectedClients, serverPayload)
        }
    }

    private fun startAdvertising(packageName: String) {
        val advertisingOptions: AdvertisingOptions = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_STAR).build()
        connectionsClient.stopAdvertising()
        connectionsClient.stopAllEndpoints()

        connectionsClient.startAdvertising(serverConfiguration.serverAsPlayerName, packageName, connectionLifecycleCallback, advertisingOptions)
            .addOnSuccessListener {
                Log.i("ServerManager.kt", "SERVER ADVERTISING READY")
                _serverState.update { it.copy(
                    serverStatus = ServerStatusEnum.ADVERTISING
                ) }
            }
            .addOnFailureListener {
                Log.e("ServerManager.kt", "SERVER ADVERTISING FAILED")
                _serverState.update { it.copy(
                    serverStatus = ServerStatusEnum.ADVERTISING_FAILED
                ) }
            }
    }

    private val connectionLifecycleCallback: ConnectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            if (_serverState.value.connectedClients.size >= serverConfiguration.maximumConnections){
                connectionsClient.rejectConnection(endpointId)
            } else{
                connectionsClient.acceptConnection(endpointId, payloadCallback)
            }
        }

        override fun onConnectionResult(endpointId: String, resolution: ConnectionResolution) {
            when (resolution.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    clientConnected(endpointId)
                }
                else -> {
                    _serverState.update { it.copy(
                        serverStatus = ServerStatusEnum.ADVERTISING_FAILED
                    ) }
                    Log.e("ServerManager.kt", "SERVER status code ${resolution.status.statusCode}: ${resolution.status.statusMessage}")
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            clientDisconnected(endpointId)
            Log.i("ServerManager.kt", "SERVER: $endpointId disconnected")
        }
    }

    private val payloadCallback: PayloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            try {
                val result = fromPayload<ClientPayloadType, Any>(payload, null)

                val clientPayloadType = result.first
                when(clientPayloadType){
                    ClientPayloadType.ESTABLISH_CONNECTION -> {
                        clientAction(ClientAction.EstablishConnection(endpointId, payload))
                    }
                    ClientPayloadType.ACTION_DISCONNECTED -> {
                        clientDisconnected(endpointId)
                    }
                }
            } catch (_: Exception){
                clientAction(ClientAction.PayloadAction(endpointId, payload))
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) { }
    }
}