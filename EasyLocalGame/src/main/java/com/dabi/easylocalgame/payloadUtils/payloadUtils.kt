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
* If you have your own TypeAdapter, register it to this object by using <b>.registerTypeAdapter()</b>.
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


fun <T> toServerPayload(payloadType: ServerPayloadType, data: T): Payload {
    val gson = gsonBuilder.create()

    val json = gson.toJson(Pair(payloadType, data))
    return Payload.fromBytes(json.toByteArray(Charsets.UTF_8))
}
fun <T> toServerPayload(payloadType: String, data: T): Payload {
    val gson = gsonBuilder.create()

    val json = gson.toJson(Pair(payloadType, data))
    return Payload.fromBytes(json.toByteArray(Charsets.UTF_8))
}
/**
 * Converts a `ServerPayload` to a [Pair] of [ServerPayloadType] and [T].
 *
 * If you set [T] to [Any], you can set the [typeAdapters] parameter to `null`:
 * ```kotlin
 * val result: Pair<ServerPayloadType, Any?> = fromServerPayload(serverPayload, null)
 * ```
 *
 * If you set [T] to a specific class that you want to deserialize directly, you **may** set [typeAdapters] to `null`
 * only if the class or its members do not require a `TypeAdapter`. If a `TypeAdapter` is required,
 * register it with [gsonBuilder] first or add it to the [typeAdapters] parameter:
 * ```kotlin
 * val result: Pair<ServerPayloadType, DontNeedAdapter?> = fromServerPayload(serverPayload, null)
 * val result2: Pair<ServerPayloadType, NeedAdapter?> = fromServerPayload(
 *     payload = serverPayload,
 *     typeAdapters = mapOf(NeedAdapter::class.java to NeedAdapterTypeAdapter())
 * )
 * // or
 * gsonBuilder.registerTypeAdapter(NeedAdapter::class.java, NeedAdapterTypeAdapter())
 * val result3: Pair<ServerPayloadType, NeedAdapter?> = fromServerPayload(serverPayload, null)
 * ```
 */
inline fun <reified R, T> fromServerPayload(payload: Payload, typeAdapters: Map<Type, Any>?): Pair<R, T?> {
    val gson = gsonBuilder.apply {
        typeAdapters?.forEach { (t, a) ->
            this.registerTypeAdapter(t, a)
        }
    }.create()

    val rawData = String(payload.asBytes()!!, Charsets.UTF_8)
    return gson.fromJson(rawData, object : TypeToken<Pair<R, T?>>() {}.type)
}



fun <T> toClientPayload(payloadType: ClientPayloadType, data: T?): Payload {
    val gson = gsonBuilder.create()

    val json = gson.toJson(Pair(payloadType, data))
    return Payload.fromBytes(json.toByteArray(Charsets.UTF_8))
}
fun <T> toClientPayload(payloadType: String, data: T?): Payload {
    val gson = gsonBuilder.create()

    val json = gson.toJson(Pair(payloadType, data))
    return Payload.fromBytes(json.toByteArray(Charsets.UTF_8))
}
/**
 * Converts a `ClientPayload` to a [Pair] of [ClientPayloadType] and [T].
 *
 * If you set [T] to [Any], you can set the [typeAdapters] parameter to `null`:
 * ```kotlin
 * val result: Pair<ClientPayloadType, Any?> = fromClientPayload(clientPayload, null)
 * ```
 *
 * If you set [T] to a specific class that you want to deserialize directly, you **may** set [typeAdapters] to `null`
 * only if the class or its members do not require a `TypeAdapter`. If a `TypeAdapter` is required,
 * register it with [gsonBuilder] first or add it to the [typeAdapters] parameter:
 * ```kotlin
 * val result: Pair<ClientPayloadType, DontNeedAdapter?> = fromClientPayload(clientPayload, null)
 * val result2: Pair<ClientPayloadType, NeedAdapter?> = fromClientPayload(
 *     payload = clientPayload,
 *     typeAdapters = mapOf(NeedAdapter::class.java to NeedAdapterTypeAdapter())
 * )
 * // or
 * gsonBuilder.registerTypeAdapter(NeedAdapter::class.java, NeedAdapterTypeAdapter())
 * val result3: Pair<ClientPayloadType, NeedAdapter?> = fromClientPayload(clientPayload, null)
 * ```
 */
inline fun <reified R, T> fromClientPayload(payload: Payload, typeAdapters: Map<Type, Any>?): Pair<R, T?> {
    val gson = gsonBuilder.apply {
        typeAdapters?.forEach { (t, a) ->
            this.registerTypeAdapter(t, a)
        }
    }.create()

    val rawData = String(payload.asBytes()!!, Charsets.UTF_8)
    return gson.fromJson(rawData, object : TypeToken<Pair<R, T?>>() {}.type)
}
