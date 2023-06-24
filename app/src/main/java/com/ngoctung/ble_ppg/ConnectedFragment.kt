package com.ngoctung.ble_ppg

import android.bluetooth.BluetoothGatt
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.clj.fastble.BleManager
import com.clj.fastble.callback.BleGattCallback
import com.clj.fastble.callback.BleNotifyCallback
import com.clj.fastble.callback.BleWriteCallback
import com.clj.fastble.data.BleDevice
import com.clj.fastble.exception.BleException
import com.clj.fastble.utils.HexUtil
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.ngoctung.ble_ppg.databinding.FragmentConnectedBinding


class ConnectedFragment : Fragment() {
    private var _binding: FragmentConnectedBinding? = null
    private val binding get() = _binding!!

    val deviceMac = "DE:79:D6:9C:6E:57"
    val serviceUuid = "6218e200-aa57-4302-9785-9d3727b0bde9"
    val notifyUuid = "6218e203-aa57-4302-9785-9d3727b0bde9"
    val writeUuid = "6218e201-aa57-4302-9785-9d3727b0bde9"
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConnectedBinding.inflate(inflater, container, false)

        BleManager.getInstance().init(requireActivity().application)
        BleManager.getInstance()
            .enableLog(true)
            .setReConnectCount(1, 5000)
            .setSplitWriteNum(20)
            .setConnectOverTime(10000).operateTimeout = 5000



        binding.lineChart.description.isEnabled = true
        binding.lineChart.description.text = "PPG Data Plot"
        binding.lineChart.setTouchEnabled(true)
        binding.lineChart.isAutoScaleMinMaxEnabled = true
        binding.lineChart.isDragEnabled = true
        binding.lineChart.setScaleEnabled(true)
        binding.lineChart.setDrawGridBackground(true)
        binding.lineChart.setPinchZoom(true)
        binding.lineChart.setBackgroundColor(Color.WHITE)

        val data = LineData()
        data.setValueTextColor(Color.BLUE)

        binding.lineChart.data = data
        // get the legend (only possible after setting data)
        // get the legend (only possible after setting data)
        val l: Legend = binding.lineChart.legend


        // modify the legend ...
        l.form = Legend.LegendForm.LINE
        l.textColor = Color.WHITE

        val xl: XAxis = binding.lineChart.xAxis
        xl.textColor = Color.BLACK
        xl.setDrawGridLines(true)
        xl.setAvoidFirstLastClipping(true)
        xl.isEnabled = true

        val leftAxis: YAxis = binding.lineChart.axisLeft
        leftAxis.textColor = Color.BLACK
        leftAxis.setDrawGridLines(true)
//        leftAxis.axisMaximum = 100000f
//        leftAxis.axisMinimum = 0f
        leftAxis.setDrawGridLines(true)

        val rightAxis: YAxis = binding.lineChart.axisRight
        rightAxis.isEnabled = false

        binding.lineChart.axisLeft.setDrawGridLines(true)
        binding.lineChart.xAxis.setDrawGridLines(true)
        binding.lineChart.setDrawBorders(true)

        receiveMibandHeartRateNotify()


        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()

    }

    private fun receiveMibandHeartRateNotify() {
        BleManager.getInstance().init(requireActivity().application)
        BleManager.getInstance()
            .enableLog(true)
            .setReConnectCount(1, 5000)
            .setSplitWriteNum(20)
            .setConnectOverTime(10000).operateTimeout = 5000

        BleManager.getInstance().connect(deviceMac, object : BleGattCallback() {
            override fun onStartConnect() {
                Log.i("BLE", "CONNECT START")
            }

            override fun onConnectFail(bleDevice: BleDevice?, exception: BleException?) {
                Log.i("BLE", "CONNECT FAIL")
            }

            override fun onConnectSuccess(bleDevice: BleDevice?, gatt: BluetoothGatt?, status: Int) {
                Log.i("BLE", "CONNECT SUCCESS")
                Toast.makeText(requireContext(),
                    "Connected device",
                    Toast.LENGTH_SHORT
                ).show()
                binding.buttonStart.setOnClickListener{
                    BleManager.getInstance().write(
                        bleDevice,
                        serviceUuid,
                        writeUuid,
                        HexUtil.hexStringToBytes("0801100A"),
                        object : BleWriteCallback() {
                            override fun onWriteSuccess(current: Int, total: Int, justWrite: ByteArray) {
                                Log.i("BLE", "START GET DATA AFE4420")
                            }
                            override fun onWriteFailure(exception: BleException) {
                                Log.i("BLE", "FAILED START GET DATA AFE4420")
                            }
                        })
                }
                binding.buttonStop.setOnClickListener{
                    BleManager.getInstance().write(
                        bleDevice,
                        serviceUuid,
                        writeUuid,
                        HexUtil.hexStringToBytes("0801100B"),
                        object : BleWriteCallback() {
                            override fun onWriteSuccess(current: Int, total: Int, justWrite: ByteArray) {
                                Log.i("BLE", "STOP GET DATA AFE4420")
                            }
                            override fun onWriteFailure(exception: BleException) {
                                Log.i("BLE", "FAILED STOP GET DATA AFE4420")
                            }
                        })
                }
                BleManager.getInstance().notify(
                    bleDevice,
                    serviceUuid,
                    notifyUuid,
                    object : BleNotifyCallback() {
                        override fun onNotifySuccess() {
                            Log.i("BLE", "OPEN NOTIFY SUCCESS")
                        }

                        override fun onNotifyFailure(exception: BleException?) {
                            Log.i("BLE", "OPEN NOTIFY FAIL")
                        }

                        override fun onCharacteristicChanged(data: ByteArray?) {
                            val intValue = ((data?.get(3)!!.toInt() and 0xFF) shl 24) or
                                    ((data[2].toInt() and 0xFF) shl 16) or
                                    ((data[1].toInt() and 0xFF) shl 8) or
                                    ((data[0].toInt() and 0xFF))
                            Log.i("NOTIFY DATA", intValue.toString())
                            runOnUiThread {
                                drawChart(intValue)
                            }
                        }
                    })
            }

            override fun onDisConnected(
                isActiveDisConnected: Boolean,
                bleDevice: BleDevice?,
                gatt: BluetoothGatt?,
                status: Int
            ) {
                Log.i("BLE", "CONNECT DISCONNECT")
                Toast.makeText(requireContext(),
                    "Disconnected BLE with device",
                    Toast.LENGTH_SHORT
                ).show()
                findNavController().navigate(R.id.action_connectedFragment_to_scanFragment)
            }
        })
    }

    private fun runOnUiThread(runnable: java.lang.Runnable) {
        if (isAdded && activity != null) requireActivity().runOnUiThread(runnable)
    }

    private fun drawChart(samples: Int) {
        val data: LineData = binding.lineChart.data
        var set = data.getDataSetByIndex(0)
        if (set == null) {
            set = createSet()
            data.addDataSet(set)
        }
        data.addEntry(Entry(set.entryCount.toFloat(), samples.toFloat()), 0)

        data.notifyDataChanged()
        binding.lineChart.notifyDataSetChanged()
        binding.lineChart.setVisibleXRangeMaximum(30F)
        binding.lineChart.moveViewToX(data.entryCount.toFloat())
    }

    private fun createSet(): LineDataSet {
        val set = LineDataSet(null, "Dynamic Data")
        set.axisDependency = YAxis.AxisDependency.LEFT
        set.lineWidth = 3f
        set.color = Color.BLUE
        set.isHighlightEnabled = false
        set.setDrawValues(false)
        set.setDrawCircles(false)
        set.mode = LineDataSet.Mode.CUBIC_BEZIER
        set.cubicIntensity = 0.2f
        return set
    }

}