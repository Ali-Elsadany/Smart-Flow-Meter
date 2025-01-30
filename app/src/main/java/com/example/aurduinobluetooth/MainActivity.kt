package com.example.bluetoothterminal

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.aurduinobluetooth.R
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import kotlin.math.log

class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var pairedDevices: Set<BluetoothDevice>
    private lateinit var deviceList: ListView
    private lateinit var connectThread: ConnectThread
    private lateinit var connectedThread: ConnectedThread
    private lateinit var tvStatus: TextView
    private lateinit var tvConnectedDevice: TextView
    private lateinit var tvReceivedData: TextView
    private lateinit var tvData: TextView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button
    private lateinit var btnDisconnect: Button
    private lateinit var tvAvailableDevices: TextView

    val handler = Handler(Looper.getMainLooper())
    private var connectedDevice: BluetoothDevice? = null
    private var isConnected = false

    private companion object {
        private const val REQUEST_ENABLE_BT = 1
        private const val REQUEST_LOCATION_PERMISSION = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Initialize UI components
        tvStatus = findViewById(R.id.tvStatus)
        tvConnectedDevice = findViewById(R.id.tvConnectedDevice)
        tvReceivedData = findViewById(R.id.tvReceivedData)
        tvData = findViewById(R.id.tvData)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        btnDisconnect = findViewById(R.id.btnDisconnect)
        tvAvailableDevices = findViewById(R.id.tvAvailableDevices)
        deviceList = findViewById(R.id.lvDevices)

        btnSend.setOnClickListener { sendMessage() }
        btnDisconnect.setOnClickListener { disconnect() }

        // Check and request permissions
        checkPermissions()

        // Enable Bluetooth
        enableBluetooth()
    }

    private fun checkPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        // Check for Location permission
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        // Check for Bluetooth scan permission (required for Android 12+)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
        }

        // Check for Bluetooth connect permission (required for Android 12+)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        // Request permissions if any are missing
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                REQUEST_LOCATION_PERMISSION
            )
        } else {
            // Permissions already granted, proceed with Bluetooth operations
            scanDevices()
        }
    }


    private fun enableBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_LOCATION_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.e("Ali", " done ", )
                    // Permission granted, proceed with Bluetooth operations
                    scanDevices()
                } else {
                    Log.e("Ali", " not done ", )
                    requestLocationPermission()
                    Toast.makeText(
                        this,
                        "Location permission is required for Bluetooth scanning",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    private fun requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Fun Toast message
            Toast.makeText(
                this,
                "We need location permission to discover nearby Bluetooth devices! 📍🔍",
                Toast.LENGTH_LONG
            ).show()

            // Request permission with a bit of flair
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        } else {
            // Permission already granted, no need to ask
            Toast.makeText(this, "Location permission already granted! Let's connect! 🎉", Toast.LENGTH_SHORT).show()
            scanDevices()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_ENABLE_BT -> {
                if (resultCode == RESULT_OK) {
                    // Bluetooth is enabled, proceed with scanning
                    scanDevices()
                } else {
                    Toast.makeText(
                        this,
                        "Bluetooth must be enabled to use this app",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun scanDevices() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        pairedDevices = bluetoothAdapter.bondedDevices
        val list = ArrayList<String>()
        Log.e("list", "scanDevices: $list", )
        val deviceMap = HashMap<String, BluetoothDevice>()

        for (device in pairedDevices) {
            list.add("${device.name}\n${device.address}")
            deviceMap[device.address] = device
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, list)
        deviceList.adapter = adapter

        deviceList.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val info = list[position]
            val address = info.substring(info.length - 17)
            connectedDevice = deviceMap[address]
            connectToDevice()
        }
    }

    private fun connectToDevice() {
        if (connectedDevice == null) return

        connectThread = ConnectThread(connectedDevice!!)
        connectThread.start()
    }

    private fun sendMessage() {
        val message = etMessage.text.toString()
        if (message.isNotEmpty()) {
            connectedThread.write(message.toByteArray())
            etMessage.text.clear()
        }
    }

    private fun disconnect() {
        if (isConnected) {
            connectedThread.cancel()
            isConnected = false
            updateUI(false)
        }
    }

    private fun updateUI(connected: Boolean) {
        if (connected) {
            tvStatus.visibility = View.GONE
            tvConnectedDevice.visibility = View.VISIBLE
            tvReceivedData.visibility = View.VISIBLE
            tvData.visibility = View.VISIBLE
            etMessage.visibility = View.VISIBLE
            btnSend.visibility = View.VISIBLE
            btnDisconnect.visibility = View.VISIBLE
            tvAvailableDevices.visibility = View.GONE
            deviceList.visibility = View.GONE
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            tvConnectedDevice.text = "Connected to: ${connectedDevice?.name}"
        } else {
            tvStatus.visibility = View.VISIBLE
            tvConnectedDevice.visibility = View.GONE
            tvReceivedData.visibility = View.GONE
            tvData.visibility = View.GONE
            etMessage.visibility = View.GONE
            btnSend.visibility = View.GONE
            btnDisconnect.visibility = View.GONE
            tvAvailableDevices.visibility = View.VISIBLE
            deviceList.visibility = View.VISIBLE
            tvStatus.text = "Scanning for devices..."
            tvData.text = ""
        }
    }

    class ConnectedThread(private val mmSocket: BluetoothSocket) : Thread() {
        private val mmInStream: InputStream = mmSocket.inputStream
        private val mmOutStream: OutputStream = mmSocket.outputStream
        private val mmBuffer: ByteArray = ByteArray(1024)
        val handler = Handler(Looper.getMainLooper())




        override fun run() {
            var numBytes: Int

            while (true) {
                try {
                    numBytes = mmInStream.read(mmBuffer)
                    val readMsg = String(mmBuffer, 0, numBytes)
                    handler.post {
                        // myviewModel(readMsg)
                    }
                } catch (e: IOException) {
                    break
                }
            }
        }

        fun write(bytes: ByteArray) {
            try {
                mmOutStream.write(bytes)
            } catch (e: IOException) {
                handler.post {
//                Toast.makeText(this, "Error sending message", Toast.LENGTH_SHORT).show()
                }
            }
        }

        fun cancel() {
            try {
                mmSocket.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

    }
    @SuppressLint("MissingPermission")
    inner class ConnectThread(device: BluetoothDevice) : Thread() {
        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            // Check if BLUETOOTH_CONNECT and BLUETOOTH_SCAN permissions are granted
            if (ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Permission not granted, handle it (e.g., show a message or request permission)
                handler.post {
                    Toast.makeText(
                        this@MainActivity,
                        "Bluetooth connect and scan permissions required",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                 null // Return null if permissions are not granted
            } else {
                // Permission granted, create the socket
                device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
            }
        }

        @RequiresApi(Build.VERSION_CODES.S)
        override fun run() {
            // Check if Bluetooth Scan permission is granted
            if (ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Request the missing permission
                val REQUEST_BLUETOOTH_SCAN_PERMISSION = 1
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(Manifest.permission.BLUETOOTH_SCAN),
                    REQUEST_BLUETOOTH_SCAN_PERMISSION
                )
                return
            }

            // Permission is granted, proceed with Bluetooth connection logic
            bluetoothAdapter.cancelDiscovery()

            mmSocket?.let { socket ->
                try {
                    socket.connect()
                    handler.post {
                        isConnected = true
                        updateUI(true)
                    }
                    connectedThread = ConnectedThread(socket)
                    connectedThread.start()
                } catch (e: IOException) {
                    handler.post {
                        Toast.makeText(this@MainActivity, "Connection failed", Toast.LENGTH_SHORT).show()
                    }
                    try {
                        socket.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            } ?: run {
                // Socket is null (permission not granted), handle it
                handler.post {
                    Toast.makeText(
                        this@MainActivity,
                        "Cannot create Bluetooth socket due to missing permission",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }



    }

}


