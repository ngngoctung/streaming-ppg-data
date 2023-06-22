package com.ngoctung.ble_ppg

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.clj.fastble.BleManager
import com.clj.fastble.data.BleDevice
import com.ngoctung.ble_ppg.databinding.ItemDevicesBinding

class ScanDevicesAdapter: RecyclerView.Adapter<ScanDevicesAdapter.ScanDevicesViewHolder>() {

    private val bleDeviceList = mutableListOf<BleDevice>()
    private var listener: OnDeviceClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScanDevicesViewHolder {
        val binding = ItemDevicesBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ScanDevicesViewHolder(binding, parent.context, listener)
    }

    override fun getItemCount(): Int = bleDeviceList.size

    override fun onBindViewHolder(holder: ScanDevicesViewHolder, position: Int) {
        holder.bind(bleDeviceList[position])
    }

    @SuppressLint("NotifyDataSetChanged")
    fun clearScanResults() {
        bleDeviceList.forEach {
            if (!BleManager.getInstance().isConnected(it))
                bleDeviceList.remove(it)
        }
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun addDevice(bleDevice: BleDevice) {
        removeDevice(bleDevice)
        bleDeviceList.add(bleDevice)
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun removeDevice(bleDevice: BleDevice) {
        bleDeviceList.remove(bleDevice)
        notifyDataSetChanged()
    }

    interface OnDeviceClickListener {
        fun onConnect(bleDevice: BleDevice)
        fun onDisconnect(bleDevice: BleDevice)
        fun onDetail(bleDevice: BleDevice)
    }

    fun setOnDeviceClickListener(listener: OnDeviceClickListener?) {
        this.listener = listener
    }

    class ScanDevicesViewHolder(
        private val binding: ItemDevicesBinding,
        private val context: Context,
        private val listener: OnDeviceClickListener?
    ): RecyclerView.ViewHolder(binding.root) {

        fun bind(device: BleDevice) {
           binding.textViewDeviceName.text = String.
           format("%s\n%s", device.name ?: "Unknow", device.mac)
            binding.textViewMacAddress.text = String.format("RSSI: %d", device.rssi)

            val isConnected = BleManager.getInstance().isConnected(device)
            if(isConnected){
                binding.imageViewIcBluetooth.
                setImageResource(R.drawable.baseline_bluetooth_connected_24)
                binding.buttonConnect.text = "Disconnected"
            }
            else
            {
                binding.imageViewIcBluetooth.
                setImageResource(R.drawable.baseline_bluetooth_24)
                binding.buttonConnect.text = "Connect"
            }

            binding.buttonConnect.setOnClickListener{
                if(isConnected)
                {
                    binding.buttonConnect.text = "Disconnecting"
                    listener?.onDisconnect(device)
                }
                else
                {
                    binding.buttonConnect.text = "Connecting"
                    listener?.onConnect(device)
                }
            }
        }
    }
}