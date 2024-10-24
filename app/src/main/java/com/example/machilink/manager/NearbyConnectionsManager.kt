package com.example.machilink.manager

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.util.Log

class NearbyConnectionsManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val strategy = Strategy.P2P_POINT_TO_POINT
    private var isAdvertising = false

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            Log.d(TAG, "Connection initiated with endpoint: $endpointId")
            Nearby.getConnectionsClient(context).acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    Log.d(TAG, "Connection successful with endpoint: $endpointId")
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    Log.d(TAG, "Connection rejected by endpoint: $endpointId")
                }
                else -> {
                    Log.d(TAG, "Connection failed with endpoint: $endpointId. Status code: ${result.status.statusCode}")
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.d(TAG, "Disconnected from endpoint: $endpointId")
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            when (payload.type) {
                Payload.Type.BYTES -> {
                    val data = payload.asBytes()?.let { String(it) }
                    Log.d(TAG, "Received payload: $data from endpoint: $endpointId")
                }
                else -> Log.d(TAG, "Received unknown payload type from endpoint: $endpointId")
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            when (update.status) {
                PayloadTransferUpdate.Status.SUCCESS -> {
                    Log.d(TAG, "Payload transfer successful to endpoint: $endpointId")
                }
                PayloadTransferUpdate.Status.FAILURE -> {
                    Log.d(TAG, "Payload transfer failed to endpoint: $endpointId")
                }
            }
        }
    }

    fun startAdvertising(callback: (Boolean, String?) -> Unit) {
        if (isAdvertising) {
            Log.d(TAG, "Already advertising, stopping previous advertisement")
            stopAdvertising()
        }

        val advertisingOptions = AdvertisingOptions.Builder().setStrategy(strategy).build()

        Nearby.getConnectionsClient(context)
            .startAdvertising(
                "Device Name",
                "com.example.machilink",
                connectionLifecycleCallback,
                advertisingOptions
            )
            .addOnSuccessListener {
                Log.d(TAG, "Started advertising successfully")
                isAdvertising = true
                scope.launch { callback(true, null) }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to start advertising", e)
                isAdvertising = false
                scope.launch { callback(false, e.message) }
            }
    }

    fun stopAdvertising() {
        try {
            if (isAdvertising) {
                Nearby.getConnectionsClient(context).stopAdvertising()
                Log.d(TAG, "Stopped advertising")
                isAdvertising = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping advertising", e)
        }
    }

    fun stopAllEndpoints() {
        try {
            stopAdvertising()  // 広告を停止
            Nearby.getConnectionsClient(context).stopAllEndpoints()
            Log.d(TAG, "Stopped all endpoints")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping endpoints", e)
        }
    }

    // アクティビティのライフサイクルに合わせて呼び出すメソッドを追加
    fun onPause() {
        stopAdvertising()
    }

    fun onResume() {
        // 必要に応じて広告を再開
    }

    companion object {
        private const val TAG = "NearbyConnectionsManager"
    }
}