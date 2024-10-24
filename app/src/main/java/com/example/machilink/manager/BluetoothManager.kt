package com.example.machilink.manager

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

class BluetoothManager(private val context: Context) {
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var discoveryCallback: ((List<BluetoothDevice>) -> Unit)? = null
    private val discoveredDevices = mutableListOf<BluetoothDevice>()

    init {
        initializeBluetooth()
    }

    private fun initializeBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        registerReceivers()
    }

    private fun registerReceivers() {
        context.registerReceiver(
            discoveryReceiver,
            IntentFilter(BluetoothDevice.ACTION_FOUND)
        )
    }

    fun startDiscovery(callback: (List<BluetoothDevice>) -> Unit) {
        discoveryCallback = callback
        discoveredDevices.clear()
        bluetoothAdapter.startDiscovery()
    }

    private val discoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when(intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(
                        BluetoothDevice.EXTRA_DEVICE
                    )
                    device?.let {
                        discoveredDevices.add(it)
                        discoveryCallback?.invoke(discoveredDevices.toList())
                    }
                }
            }
        }
    }
}