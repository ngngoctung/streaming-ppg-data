package com.ngoctung.ble_ppg

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.clj.fastble.BleManager
import com.ngoctung.ble_ppg.databinding.FragmentScanBinding

class ScanFragment : Fragment() {
    private var _binding: FragmentScanBinding? = null
    private val binding get() = _binding!!

    private val permissions = arrayOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initBleManager()

        binding.fab.setOnClickListener {
            checkPermissions()
        }
    }

    private fun initBleManager() {
        BleManager.getInstance().init(requireActivity().application)
        BleManager.getInstance()
            .enableLog(true)
            .setReConnectCount(1, 5000)
            .setSplitWriteNum(20)
            .setConnectOverTime(10000).operateTimeout = 5000
    }

    private fun checkPermissions() {
        if (allPermissionsGranted()) {
            onPermissionGranted()
        } else {
            requestPermissions(permissions, REQUEST_CODE_PERMISSIONS)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_PERMISSIONS)
            if (allPermissionsGranted())
                onPermissionGranted()
            else
                onPermissionDenied()
    }

    private fun onPermissionGranted() {
        gpsNeeded()
    }

    private fun onPermissionDenied() {
        Toast.makeText(
            requireContext(),
            "Permissions needed to scan bluetooth devices",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun allPermissionsGranted() = permissions.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    private fun gpsNeeded(){
        if(checkGPSIsOpen())
//            bleScan()
        else{
            val alertDialog: AlertDialog? = activity?.let {
                val builder = AlertDialog.Builder(it)
                builder.apply {
                    setTitle(R.string.gps_must_be_enable)
                    setMessage(R.string.please_activate_your_gps)
                    setPositiveButton(R.string.active_gps) {dialog,_ ->
                        actvateGps()
                        dialog.dismiss()
                    }
                    setNegativeButton(R.string.not_now) {dialog,_ ->
                        dialog.dismiss()
                    }
                }
                builder.create()
            }
            alertDialog?.show()
        }
    }

    private fun checkGPSIsOpen(): Boolean {
        val locationManager =
            context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager?
                ?: return false
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun actvateGps(){
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        startActivity(intent)
    }

    companion object {
        const val REQUEST_CODE_PERMISSIONS = 9999
        const val BLE_DEVICE = "bleDevice"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}