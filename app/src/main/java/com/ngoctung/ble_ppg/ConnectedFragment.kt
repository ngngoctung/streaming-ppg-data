package com.ngoctung.ble_ppg

import android.bluetooth.BluetoothGatt
import android.graphics.Color
import android.hardware.SensorEvent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.fragment.app.Fragment
import com.clj.fastble.BleManager
import com.clj.fastble.callback.BleGattCallback
import com.clj.fastble.data.BleDevice
import com.clj.fastble.callback.BleNotifyCallback
import com.clj.fastble.exception.BleException
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.ngoctung.ble_ppg.databinding.FragmentConnectedBinding
import kotlinx.coroutines.*


class ConnectedFragment : Fragment() {
    private var _binding: FragmentConnectedBinding? = null
    private val binding get() = _binding!!
    private var plot_data = true
    private var job: Job? = null
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
        binding.lineChart.isDragEnabled = true
        binding.lineChart.setScaleEnabled(true)
        binding.lineChart.setDrawGridBackground(false)
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
        xl.textColor = Color.WHITE
        xl.setDrawGridLines(true)
        xl.setAvoidFirstLastClipping(true)
        xl.isEnabled = true

        val leftAxis: YAxis = binding.lineChart.axisLeft
        leftAxis.textColor = Color.WHITE
        leftAxis.setDrawGridLines(false)
        leftAxis.axisMaximum = 10f
        leftAxis.axisMinimum = 0f
        leftAxis.setDrawGridLines(true)

        val rightAxis: YAxis = binding.lineChart.axisRight
        rightAxis.isEnabled = false

        binding.lineChart.axisLeft.setDrawGridLines(false)
        binding.lineChart.xAxis.setDrawGridLines(false)
        binding.lineChart.setDrawBorders(false)

        receiveMibandHeartRateNotify()


        return binding.root
    }

    private fun receiveMibandHeartRateNotify() {
        val deviceMac = "EC:62:60:93:7F:3E"
        val serviceUuid = "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
        val notifyUuid = "beb5483e-36e1-4688-b7f5-ea07361b26a8"

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
                            runOnUiThread {
//                                Toast.makeText(
//                                    requireContext(),
//                                    intValue.toString(),
//                                    Toast.LENGTH_SHORT
//                                ).show()
                                Log.i("BLE", intValue.toString())
                                if(plot_data){
                                    addEntry();
                                    plot_data = false;
                                }
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
            }
        })
    }

    private fun runOnUiThread(runnable: java.lang.Runnable) {
        if (isAdded && activity != null) requireActivity().runOnUiThread(runnable)
    }

    private fun addEntry() {
        val data: LineData = binding.lineChart.data
        var set = data.getDataSetByIndex(0)
        // set.addEntry(...); // can be called as well
        if (set == null) {
            set = createSet()
            data.addDataSet(set)
        }

        data.addEntry(Entry(set.entryCount.toFloat(), (Math.random() * 80 + 10f).toFloat()), 0)
//            data.addEntry(
//                MutableMap.MutableEntry<Any?, Any?>(set.entryCount, event.values[0] + 5),
//                0
//            )
        data.notifyDataChanged()

        // let the chart know it's data has changed
        binding.lineChart.notifyDataSetChanged()

        // limit the number of visible entries
//        binding.lineChart.setVisibleXRangeMaximum(150F)
        // mChart.setVisibleYRange(30, AxisDependency.LEFT);

        // move to the latest entry
        binding.lineChart.moveViewToX(data.entryCount.toFloat())
    }

    private fun createSet(): LineDataSet {
        val set = LineDataSet(null, "Dynamic Data")
        set.axisDependency = YAxis.AxisDependency.LEFT
        set.lineWidth = 3f
        set.color = Color.MAGENTA
        set.isHighlightEnabled = false
        set.setDrawValues(false)
        set.setDrawCircles(false)
        set.mode = LineDataSet.Mode.CUBIC_BEZIER
        set.cubicIntensity = 0.2f
        return set
    }

    private fun feedMultiple() {
        job?.cancel()
        job = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                plot_data = true
                delay(10)
            }
        }
    }

}