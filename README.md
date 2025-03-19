# EasyLocalGame

**EasyLocalGame** is a **Kotlin** library designed to simplify the development of **local multiplayer games** using **Nearby Connections API**. It is optimized for **Jetpack Compose** and is structured to be used within a **ViewModel** for seamless integration.

## Features

- ✅ **Easy integration** – Designed to be used within `ViewModel`, making it perfect for Jetpack Compose applications.
- ✅ **Nearby Connections API support** – Enables peer-to-peer communication between devices without an internet connection.
- ✅ **Modular architecture** – Allows developers to customize client-server interactions based on their game logic.

## Installation

To use this library in your project, add the following to your **settings.gradle.kts**:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

Then add the dependency to your app module **build.gradle.kts**:

```kotlin
dependencies {
    implementation("com.github.grasski:EasyLocalGame:latest-version")
}
```

Replace `latest-version` with the latest release from [![](https://jitpack.io/v/grasski/EasyLocalGame.svg)](https://jitpack.io/#grasski/EasyLocalGame).

## How It Works

The library provides **Server** and **Client** templates that extend `ViewModel`, enabling efficient game state management.

### 🔹 Server

The server utilizes `ServerManager` to handle connected clients and process game actions.

```kotlin
abstract class ServerViewmodelTemplate(
    private val connectionsClient: ConnectionsClient,
) : ViewModel() {
    /**
     * Use this function to handle [ClientAction]s in your game state.
     *
     * [ClientAction.PayloadAction] is primarily used for custom [ClientPayloadType] events,
     * but can be utilized as needed.
     */
    abstract fun clientAction(clientAction: ClientAction)

    val serverManager: ServerManager by lazy {
        ServerManager(connectionsClient, this::clientAction)
    }
}
```

#### Handling Client Actions

The `clientAction` function on the server receives messages from clients and updates the game state accordingly.
It processes different types of client actions, such as:

##### **ClientAction.PayloadAction**
- **ACTION\_READY** – Toggles the player's "ready" state.
- **ACTION\_CHECK / ACTION\_CALL / ACTION\_RAISE** – Handles turn-based game actions like checking, calling, or raising in a poker-style game.
- **ACTION\_FOLD** – Allows the player to fold.

##### **Library Events**
- **EstablishConnection** – Registers a new player when they connect. This is a library event, which you can use to update your game state to match your own game logic.
- **Disconnect** – Removes a player when they leave. This is a library event, which you can use to update your game state to match your own game logic.

Each developer can **override** this function and customize it to fit their own game logic.

```kotlin
override fun clientAction(clientAction: ClientAction) {
    when(clientAction) {
        is ClientAction.PayloadAction -> {
            val clientID = clientAction.endpointID
            val result = fromClientPayload<MyClientPayloadType, Any>(clientAction.payload, null)
            val clientPayloadType = result.first
            val data = result.second
            
            when(clientPayloadType) {
                MyClientPayloadType.ACTION_READY -> togglePlayerReady(clientID)
                MyClientPayloadType.ACTION_CHECK -> handleCheckAction(clientID)
                MyClientPayloadType.ACTION_CALL -> handleCallAction(clientID)
                MyClientPayloadType.ACTION_RAISE -> handleRaiseAction(clientID, data)
                MyClientPayloadType.ACTION_FOLD -> handleFoldAction(clientID)
            }
        }
        is ClientAction.EstablishConnection -> registerNewPlayer(clientAction)
        is ClientAction.Disconnect -> removePlayer(clientAction)
    }
}
```

**Custom Payload Actions**

For `ClientAction.PayloadAction`, developers can define their own action types specific to their game. For example, in a poker game, actions like `ACTION_CHECK`, `ACTION_CALL`, and `ACTION_RAISE` are used, but other games can define their own logic.

Similarly, developers can define their own custom server-side actions using `ServerAction.PayloadAction` and their own `OwnServerPayloadType`. This allows for flexibility in defining game-specific events on both the client and server.

Example PayloadTypes:

```kotlin
@Keep
enum class MyClientPayloadType{
    ACTION_READY,
    ACTION_CHECK,
    ACTION_CALL,
    ACTION_RAISE,
    ACTION_FOLD
}
```

Library PayloadTypes:

```kotlin
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
```

Additionally, sending and receiving messages is facilitated by helper functions in `payloadUtils.kt`. The functions `toClientPayload` and `toServerPayload` are default for Library PayloadTypes and function `toPayload` can be used for own data types converted to String type. Function `fromPayload` help deserialize data to wanted data types of both Pair values: payloadType and message data.

```kotlin
// Client side
val clientPayload = toClientPayload(ClientPayloadType.ESTABLISH_CONNECTION, data=playerConnectionState)
clientManager.sendPayload(clientPayload)

// Server side
val result: Pair<ClientPayloadType, PlayerConnectionState?> = fromPayload(payload=payloadData, typeAdapters=null)
```
or 
```kotlin
// Server side
val serverPayloadType = toServerPayload(ServerPayloadType.UPDATE_PLAYER_STATE, playerState)
serverManager.sendPayload(playerEndpointId, serverPayloadType)

// Client side
val result: Pair<ServerPayloadType, PlayerState?> = fromPayload(payload=payloadData, typeAdapters=null)
```
or general payloadType
```kotlin
// For example on Client side
val payloadData = toPayload(MyOwnClientPayloadType.ACTION_READY.toString(), data=null)
clientManager.sendPayload(payloadData)

// Server side
val result: Pair<String or MyOwnClientPayloadType if you know the type, Any?> = fromPayload(payload=payloadData, typeAdapters=null)
```
If a custom type requires a `TypeAdapter`, it should be registered with `gsonBuilder` before serialization or deserialization.

### 🔹 Client

The client communicates with the server through `ClientManager` and reacts to server actions.

```kotlin
abstract class PlayerViewmodelTemplate(
    private val connectionsClient: ConnectionsClient,
) : ViewModel() {
    /**
     * Use this function to handle [ServerAction]s in your game state.
     *
     * [ServerAction.PayloadAction] is primarily used for custom [ServerPayloadType] events,
     * but can be utilized as needed.
     */
    abstract fun serverAction(serverAction: ServerAction)

    val clientManager: ClientManager by lazy {
        ClientManager(connectionsClient, this::serverAction)
    }
}
```

### Dependency Injection

Both `PlayerViewmodelTemplate` and `ServerViewmodelTemplate` use:

```kotlin
private val connectionsClient: ConnectionsClient
```

It is important to instantiate these ViewModels using **Hilt** or another dependency injection framework to ensure that `ConnectionsClient` is a singleton throughout the application's lifecycle. This prevents issues with multiple instances interfering with each other.

### Usage in Jetpack Compose with Hilt
In Jetpack Compose view, a developer can use these ViewModels with `Hilt` like this:

```kotlin
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val connectionsClient: ConnectionsClient
): PlayerViewmodelTemplate(connectionsClient), PlayerCoreInterface {
    private val _playerState: MutableStateFlow<PlayerState> = MutableStateFlow(PlayerState())
    val playerState = _playerState.asStateFlow()

    private val _gameState: MutableStateFlow<GameState> = MutableStateFlow(GameState())
    val gameState = _gameState.asStateFlow()

    private val _playerActionsState: MutableStateFlow<PlayerActionsState> = MutableStateFlow(PlayerActionsState())
    val playerActionsState = _playerActionsState.asStateFlow()

    // Your ViewModel logic.
}
```
As a client, you can connect to the server like this:
```kotlin
val playerViewModel: PlayerViewModel = hiltViewModel()
val playerState by playerViewModel.playerState.collectAsStateWithLifecycle()
val playerActionsState by playerViewModel.playerActionsState.collectAsStateWithLifecycle()
val clientState by playerViewModel.clientManager.clientState.collectAsStateWithLifecycle()

val context = LocalContext.current
LaunchedEffect(Unit) {
    playerViewModel.clientManager.connect(context.packageName, PlayerConnectionState(nickname, avatarId))
}

// Your view logic.
```

Or start a server like this:
```kotlin
val serverViewModel = if (serverScreen.serverType == ServerType.IS_TABLE.toString()){
    hiltViewModel<ServerOwnerViewModel>()
} else {
    hiltViewModel<ServerPlayerViewModel>()
}
val serverState by serverViewModel.serverManager.serverState.collectAsStateWithLifecycle()
val context = LocalContext.current
LaunchedEffect(Unit) {
    serverViewModel.serverManager.startServer(context.packageName, ServerConfiguration(serverType=ServerType.valueOf(serverScreen.serverType), maximumConnections=10))
}

// Your view logic.
```

This ensures that all components properly react to the ViewModel states.