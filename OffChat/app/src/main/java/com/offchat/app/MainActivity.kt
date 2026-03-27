package com.offchat.app

import android.Manifest
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.offchat.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var manager: WifiP2pManager
    private lateinit var channel: WifiP2pManager.Channel
    private lateinit var receiver: WiFiDirectBroadcastReceiver

    var isWifiP2pEnabled = false
    private val peers = mutableListOf<WifiP2pDevice>()
    private lateinit var deviceAdapter: DeviceAdapter

    private val intentFilter = IntentFilter().apply {
        addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }

    val peerListListener = WifiP2pManager.PeerListListener { deviceList: WifiP2pDeviceList ->
        val refreshedPeers = deviceList.deviceList
        peers.clear()
        peers.addAll(refreshedPeers)
        deviceAdapter.notifyDataSetChanged()
        if (peers.isEmpty()) {
            binding.statusText.text = "No devices found. Make sure both phones have app open."
        } else {
            binding.statusText.text = "${peers.size} device(s) found — tap one to connect"
        }
    }

    val connectionInfoListener = WifiP2pManager.ConnectionInfoListener { info: WifiP2pInfo ->
        if (info.groupFormed) {
            val intent = android.content.Intent(this, ChatActivity::class.java).apply {
                putExtra("isGroupOwner", info.isGroupOwner)
                putExtra("groupOwnerAddress", info.groupOwnerAddress?.hostAddress ?: "")
            }
            startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = manager.initialize(this, mainLooper, null)
        receiver = WiFiDirectBroadcastReceiver(manager, channel, this)

        deviceAdapter = DeviceAdapter(peers) { device -> connectToDevice(device) }
        binding.devicesRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.devicesRecyclerView.adapter = deviceAdapter

        binding.scanButton.setOnClickListener { checkPermissionsAndDiscover() }

        requestPermissions()
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, notGranted.toTypedArray(), 100)
        }
    }

    private fun checkPermissionsAndDiscover() {
        val hasLocation = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasLocation) discoverPeers() else requestPermissions()
    }

    private fun discoverPeers() {
        if (!isWifiP2pEnabled) {
            Toast.makeText(this, "Please enable Wi-Fi", Toast.LENGTH_SHORT).show()
            return
        }
        binding.statusText.text = "Scanning..."
        manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                binding.statusText.text = "Scanning... keep both phones open"
            }
            override fun onFailure(reason: Int) {
                binding.statusText.text = "Scan failed (code $reason). Try again."
            }
        })
    }

    private fun connectToDevice(device: WifiP2pDevice) {
        val config = WifiP2pConfig().apply { deviceAddress = device.deviceAddress }
        manager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                binding.statusText.text = "Connecting to ${device.deviceName}..."
            }
            override fun onFailure(reason: Int) {
                Toast.makeText(this@MainActivity, "Connection failed", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(receiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
    }
}
