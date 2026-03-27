package com.offchat.app

import android.net.wifi.p2p.WifiP2pDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DeviceAdapter(
    private val devices: List<WifiP2pDevice>,
    private val onClick: (WifiP2pDevice) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.deviceName)
        val statusText: TextView = view.findViewById(R.id.deviceStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_device, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device = devices[position]
        holder.nameText.text = device.deviceName.ifEmpty { "Unknown Device" }
        holder.statusText.text = when (device.status) {
            WifiP2pDevice.CONNECTED -> "Connected"
            WifiP2pDevice.AVAILABLE -> "Tap to connect"
            WifiP2pDevice.INVITED -> "Invited..."
            else -> "Unavailable"
        }
        holder.itemView.setOnClickListener { onClick(device) }
    }

    override fun getItemCount() = devices.size
}
