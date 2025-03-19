package com.dabi.easylocalgame.payloadUtils

import com.dabi.easylocalgame.payloadUtils.data.ClientPayloadType
import com.dabi.easylocalgame.payloadUtils.data.ServerPayloadType
import com.dabi.easylocalgame.textUtils.UiTexts
import com.dabi.easylocalgame.textUtils.UiTextsAdapter
import com.google.android.gms.nearby.connection.Payload
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type


/**
* Use this object for serializing and deserializing data.
*
* If you have your own `TypeAdapter`, register it to this object by using `gsonBuilder.registerTypeAdapter()`.
*/
val gsonBuilder: GsonBuilder = GsonBuilder()
    .registerTypeAdapter(UiTexts::class.java, UiTextsAdapter())

/**
 * You can use this function to convert [Any] type from the result.second of [fromServerPayload] or [fromClientPayload] functions to your specified type.
 *
 * If your specified type needs TypeAdapter, register it to [gsonBuilder] first.
 */
inline fun <reified T> Any.convertFromJsonToType(classOf: Class<T>): T{
    val gson = gsonBuilder.create()
    return gson.fromJson(gson.toJson(this), classOf)
}


/**
 * Converts a `payloadType` and `data` to a `Payload` object for sending.
 *
 * ```kotlin
 * enum class MyPayloadType {
 *     ACTION_READY,
 *     ACTION_CALL,
 * }
 * val serverPayload = toPayload(MyPayloadType.ACTION_READY.toString(), null)
 * ```
 *
 * This method is mainly used with general [String] type for your own [Enum].toString().
 *
 * Work with the `typeAdapters` is same as in the [fromPayload] function.
 */
fun <T> toPayload(payloadType: String, data: T?, typeAdapters: Map<Type, Any>?=null): Payload {
    val gson = gsonBuilder.apply {
        typeAdapters?.forEach { (t, a) ->
            this.registerTypeAdapter(t, a)
        }
    }.create()

    val json = gson.toJson(Pair(payloadType, data))
    return Payload.fromBytes(json.toByteArray(Charsets.UTF_8))
}

/**
 * Converts a `Payload` to a [Pair] of [R] and [T].
 *
 * If you set [T] to [Any], you can set the [typeAdapters] parameter to `null`:
 * ```kotlin
 * // val serverPayload = toServerPayload(ServerPayloadType.ROOM_IS_FULL, null)
 * val result: Pair<ServerPayloadType, Any?> = fromPayload(serverPayload, null)
 *
 * // Or general payload type:
 * // val myOwnPayload = toPayload(MyPayloadType.NULL_DATA.toString(), null)
 * val result: Pair<String or MyPayloadType, Any?> = fromPayload(myOwnPayload, null)
 * ```
 *
 * If you set [T] to a specific class that you want to deserialize directly, you **may** set [typeAdapters] to `null`
 * only if the class or its members do not require a `TypeAdapter`. If a `TypeAdapter` is required,
 * register it with [gsonBuilder] first or add it to the [typeAdapters] parameter:
 * ```kotlin
 * val result: Pair<ClientPayloadType, DontNeedAdapter?> = fromPayload(clientPayload, null)
 * val result2: Pair<ServerPayloadType, NeedAdapter?> = fromPayload(
 *     payload = serverPayload,
 *     typeAdapters = mapOf(NeedAdapter::class.java to NeedAdapterTypeAdapter())
 * )
 * // or
 * gsonBuilder.registerTypeAdapter(NeedAdapter::class.java, NeedAdapterTypeAdapter())
 * val result3: Pair<String or MyPayloadType, NeedAdapter?> = fromPayload(myPayload, null)
 * ```
 *
 * As you can see, you can use this function for [ServerPayloadType], [ClientPayloadType] or general [String] type for your own [Enum].toString().
 */
inline fun <reified R, reified T> fromPayload(payload: Payload, typeAdapters: Map<Type, Any>?): Pair<R, T?> {
    val gson = gsonBuilder.apply {
        typeAdapters?.forEach { (t, a) ->
            this.registerTypeAdapter(t, a)
        }
    }.create()

    val rawData = String(payload.asBytes()!!, Charsets.UTF_8)
    return gson.fromJson(rawData, object : TypeToken<Pair<R, T?>>() {}.type)
}


/**
 * Converts a `payloadType` of type [ServerPayloadType] and `data` to a `Payload` object for sending.
 */
fun <T> toServerPayload(payloadType: ServerPayloadType, data: T): Payload {
    val gson = gsonBuilder.create()

    val json = gson.toJson(Pair(payloadType, data))
    return Payload.fromBytes(json.toByteArray(Charsets.UTF_8))
}


/**
 * Converts a `payloadType` of type [ClientPayloadType] and `data` to a `Payload` object for sending.
 */
fun <T> toClientPayload(payloadType: ClientPayloadType, data: T?): Payload {
    val gson = gsonBuilder.create()

    val json = gson.toJson(Pair(payloadType, data))
    return Payload.fromBytes(json.toByteArray(Charsets.UTF_8))
}
