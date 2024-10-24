package com.example.machilink.ui.transfer

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.machilink.R
import com.example.machilink.data.model.PointTransferData
import com.example.machilink.data.model.TransferMethod
import com.example.machilink.data.model.TransferType
import com.example.machilink.databinding.ActivityPointTransferBinding
import com.example.machilink.manager.BluetoothManager
import com.example.machilink.manager.NearbyConnectionsManager
import com.example.machilink.manager.PointBalanceManager
import com.example.machilink.manager.SecurityManager
import com.example.machilink.manager.TransferHistoryManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class PointTransferActivity : AppCompatActivity() {
    private var _binding: ActivityPointTransferBinding? = null
    private val binding get() = _binding!!
    private lateinit var nearbyManager: NearbyConnectionsManager
    private lateinit var bluetoothManager: BluetoothManager
    private var hasShownPermissionDialog = false

    private val viewModel: PointTransferViewModel by viewModels {
        PointTransferViewModelFactory(
            TransferHistoryManager(),
            PointBalanceManager(),
            SecurityManager()
        )
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            hasShownPermissionDialog = false
            initializeAfterPermissions()
        } else {
            val shouldShowRationale = permissions.keys.any { permission ->
                shouldShowRequestPermissionRationale(permission)
            }

            if (!shouldShowRationale && !hasShownPermissionDialog) {
                hasShownPermissionDialog = true
                showPermissionDeniedDialog()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityPointTransferBinding.inflate(layoutInflater)
        setContentView(binding.root)

        try {
            checkAndRequestPermissions()
            initializeUserPoints() // 初期ポイントの設定
        } catch (e: Exception) {
            Log.e("PointTransferActivity", "Error in onCreate", e)
            showError("Failed to initialize: ${e.message}")
        }
    }

    private fun initializeUserPoints() {
        lifecycleScope.launch {
            try {
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId != null) {
                    val pointBalanceManager = PointBalanceManager()
                    pointBalanceManager.initializeUserPoints(userId)
                }
            } catch (e: Exception) {
                Log.e("PointTransferActivity", "Error initializing points", e)
            }
        }
    }

    private fun initializeAfterPermissions() {
        try {
            initializeManagers()
            setupUI()
            observeViewModel()
        } catch (e: Exception) {
            Log.e("PointTransferActivity", "Error in initializeAfterPermissions", e)
            showError("Failed to initialize: ${e.message}")
        }
    }

    private fun getRequiredPermissions(): List<String> {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.addAll(
                listOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            )
        } else {
            permissions.addAll(
                listOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN
                )
            )
        }

        permissions.addAll(
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )

        return permissions
    }

    private fun checkPermissions(): Boolean {
        val permissions = getRequiredPermissions()
        val permissionResults = permissions.associateWith { permission ->
            val isGranted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
            Log.d("PermissionCheck", "Permission $permission: ${if (isGranted) "GRANTED" else "DENIED"}")
            isGranted
        }

        val allGranted = permissionResults.all { it.value }
        Log.d("PermissionCheck", "All permissions granted: $allGranted")
        return allGranted
    }

    private fun logMissingPermissions(): List<String> {
        return getRequiredPermissions().filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }
    }

    private fun checkAndRequestPermissions() {
        if (hasShownPermissionDialog) {
            return
        }

        val permissions = getRequiredPermissions()
        val notGrantedPermissions = permissions.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }

        when {
            notGrantedPermissions.isEmpty() -> {
                initializeAfterPermissions()
            }
            notGrantedPermissions.any { permission ->
                shouldShowRequestPermissionRationale(permission)
            } -> {
                showPermissionExplanationDialog(notGrantedPermissions.toTypedArray())
            }
            else -> {
                requestPermissionLauncher.launch(notGrantedPermissions.toTypedArray())
            }
        }
    }

    private fun showPermissionExplanationDialog(permissions: Array<String>) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.permission_required))
            .setMessage(getString(R.string.permission_explanation_detail))
            .setPositiveButton(getString(R.string.grant_permission)) { _, _ ->
                requestPermissionLauncher.launch(permissions)
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.permission_required))
            .setMessage(getString(R.string.permission_denied_explanation))
            .setPositiveButton(getString(R.string.open_settings)) { _, _ ->
                openAppSettings()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun openAppSettings() {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            startActivity(this)
        }
    }

    private fun initializeManagers() {
        nearbyManager = NearbyConnectionsManager(this)
        bluetoothManager = BluetoothManager(this)
    }

    private fun setupUI() {
        binding.apply {
            nfcTransferButton.setOnClickListener {
                startNearbyTransfer()
            }

            bluetoothTransferButton.setOnClickListener {
                startBluetoothTransfer()
            }

            transferButton.setOnClickListener {
                initiateTransfer()
            }

            receiveButton.setOnClickListener {
                startReceiveMode()
            }
        }
    }

    private fun startReceiveMode() {
        try {
            val missingPermissions = logMissingPermissions()
            if (missingPermissions.isNotEmpty()) {
                showError(getString(R.string.permissions_not_granted))
                checkAndRequestPermissions()
                return
            }

            binding.progressBar.visibility = View.VISIBLE
            binding.statusText.text = getString(R.string.waiting_for_sender)

            nearbyManager.startDiscovery { success, error ->
                runOnUiThread {
                    if (success) {
                        Toast.makeText(
                            this,
                            getString(R.string.discovery_started),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        showError(error ?: getString(R.string.discovery_failed))
                        binding.progressBar.visibility = View.GONE
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting receive mode", e)
            showError(getString(R.string.receive_mode_failed))
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            try {
                viewModel.transferState.collect { state ->
                    updateUIState(state)
                }
            } catch (e: Exception) {
                Log.e("PointTransferActivity", "Error collecting transferState", e)
                showError("Error updating state: ${e.message}")
            }
        }

        lifecycleScope.launch {
            try {
                viewModel.balance.collect { balance ->
                    binding.balanceTextView.text = getString(R.string.current_balance_format, balance)
                }
            } catch (e: Exception) {
                Log.e("PointTransferActivity", "Error collecting balance", e)
                showError("Error updating balance: ${e.message}")
            }
        }
    }

    private fun updateUIState(state: TransferState) {
        when (state) {
            is TransferState.Initial -> {
                binding.progressBar.visibility = View.GONE
                enableButtons(true)
            }
            is TransferState.Preparing -> {
                binding.progressBar.visibility = View.VISIBLE
                enableButtons(false)
            }
            is TransferState.TransferComplete -> {
                binding.progressBar.visibility = View.GONE
                enableButtons(true)
                showTransferComplete(state.amount)
                clearInputs()
            }
            is TransferState.Error -> {
                binding.progressBar.visibility = View.GONE
                enableButtons(true)
                showError(state.message)
            }
        }
    }

    private fun enableButtons(enable: Boolean) {
        binding.apply {
            nfcTransferButton.isEnabled = enable
            bluetoothTransferButton.isEnabled = enable
            transferButton.isEnabled = enable
        }
    }

    private fun clearInputs() {
        binding.apply {
            amountEditText.text?.clear()
            recipientEditText.text?.clear()
        }
    }

    private fun showTransferComplete(amount: Int) {
        Toast.makeText(
            this@PointTransferActivity,
            getString(R.string.transfer_success, amount),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun showError(message: String) {
        Toast.makeText(this@PointTransferActivity, message, Toast.LENGTH_SHORT).show()
    }

    private fun showDeviceSelectionDialog(devices: List<BluetoothDevice>) {
        val deviceNames = devices.map { it.name ?: "Unknown Device" }.toTypedArray()

        AlertDialog.Builder(this@PointTransferActivity)
            .setTitle(getString(R.string.select_device))
            .setItems(deviceNames) { _: DialogInterface, which: Int ->
                val selectedDevice = devices[which]
                // 選択されたデバイスとの接続処理
                handleSelectedDevice(selectedDevice)
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
            }
            .show()
    }

    private fun handleSelectedDevice(device: BluetoothDevice) {
        // 選択されたデバイスとの接続処理を実装
        val amount = binding.amountEditText.text.toString().toIntOrNull()
        if (amount != null) {
            viewModel.initiateTransfer(device.address, amount, TransferMethod.BLUETOOTH)
        } else {
            showError(getString(R.string.invalid_amount))
        }
    }

    private fun startNearbyTransfer() {
        try {
            val missingPermissions = logMissingPermissions()
            if (missingPermissions.isNotEmpty()) {
                Log.d("PermissionCheck", "Missing permissions: $missingPermissions")
                showError(getString(R.string.permissions_not_granted))
                checkAndRequestPermissions()
                return
            }

            binding.progressBar.visibility = View.VISIBLE
            enableButtons(false)

            val amount = binding.amountEditText.text.toString().toIntOrNull()
                ?: throw IllegalArgumentException("Invalid amount")

            val userId = FirebaseAuth.getInstance().currentUser?.uid
                ?: throw IllegalStateException("User not authenticated")

            val transferData = PointTransferData(
                senderId = userId,
                amount = amount,
                timestamp = System.currentTimeMillis(),
                signature = "",
                type = TransferType.NORMAL
            )

            nearbyManager.startAdvertising { success, error ->
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    enableButtons(true)

                    if (success) {
                        viewModel.initiateTransfer(receiverId = "", amount = amount, method = TransferMethod.NFC)
                        Toast.makeText(
                            this@PointTransferActivity,
                            getString(R.string.nearby_transfer_started),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        showError(error ?: getString(R.string.transfer_failed))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PointTransferActivity", "Error in startNearbyTransfer", e)
            showError(getString(R.string.transfer_failed_with_message, e.message))
            binding.progressBar.visibility = View.GONE
            enableButtons(true)
        }
    }

    private fun startBluetoothTransfer() {
        try {
            val missingPermissions = logMissingPermissions()
            if (missingPermissions.isNotEmpty()) {
                Log.d("PermissionCheck", "Missing permissions: $missingPermissions")
                showError(getString(R.string.permissions_not_granted))
                checkAndRequestPermissions()
                return
            }

            bluetoothManager.startDiscovery { devices ->
                runOnUiThread {
                    showDeviceSelectionDialog(devices)
                }
            }
        } catch (e: Exception) {
            Log.e("PointTransferActivity", "Error in startBluetoothTransfer", e)
            showError(getString(R.string.transfer_failed_with_message, e.message))
        }
    }

    private fun initiateTransfer() {
        if (!checkPermissions()) {
            showError(getString(R.string.permissions_not_granted))
            return
        }

        val amount = binding.amountEditText.text.toString().toIntOrNull()
        if (amount == null) {
            showError(getString(R.string.invalid_amount))
            return
        }

        val recipient = binding.recipientEditText.text.toString()
        if (recipient.isEmpty()) {
            showError(getString(R.string.invalid_recipient))
            return
        }

        try {
            viewModel.initiateTransfer(recipient, amount, TransferMethod.MANUAL)
        } catch (e: Exception) {
            Log.e("PointTransferActivity", "Error in initiateTransfer", e)
            showError(getString(R.string.transfer_failed_with_message, e.message))
        }
    }

    override fun onPause() {
        super.onPause()
        nearbyManager.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (!hasShownPermissionDialog && !checkPermissions()) {
            checkAndRequestPermissions()
        }
        nearbyManager.onResume()
    }

    override fun onDestroy() {
        try {
            nearbyManager.stopAllEndpoints()
            _binding = null
            super.onDestroy()
        } catch (e: Exception) {
            Log.e("PointTransferActivity", "Error in onDestroy", e)
        }
    }

    companion object {
        private const val TAG = "PointTransferActivity"
    }
}