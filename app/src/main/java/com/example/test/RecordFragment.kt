package com.example.test

import android.Manifest
import android.app.Activity.RESULT_OK
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import java.util.*


/**
 * A simple [Fragment] subclass.
 * Use the [RecordFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class RecordFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var mParam1: String? = null
    private var mParam2: String? = null
    private val BLUETOOTH_PERMISSION_REQUEST_CODE = 100
    private val BLUETOOTH_DEVICE_REQUEST_CODE = 101
    private var adapter: BluetoothAdapter? = null
    private var selectedBluetoothDevice: BluetoothDevice? = null
    private var str:String?=null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            mParam1 = arguments!!.getString(ARG_PARAM1)
            mParam2 = arguments!!.getString(ARG_PARAM2)
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_record, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val btn_blue = view.findViewById<Button>(R.id.button2)
        val btn_refresh = view.findViewById<Button>(R.id.button)
        val bluetoothtext = view.findViewById<TextView>(R.id.textView)

        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        val connectedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        var connectedDeviceName: String? = null

        if (connectedDevices != null) {
            for (device in connectedDevices) {
                val deviceState = bluetoothAdapter?.getProfileConnectionState(BluetoothProfile.HEADSET)
                if (deviceState == BluetoothProfile.STATE_CONNECTED) {
                    connectedDeviceName = device.name
                    break
                }
            }
        }

        btn_blue.setOnClickListener {
            requestBluetoothPermissions()
            if (selectedBluetoothDevice != null) {
                bluetoothtext.text = str
                Log.d("TestTAG","change")
            }
            else Log.d("TestTag","not change")
        }
        btn_refresh.setOnClickListener{
            bluetoothtext.text=connectedDeviceName
        }
    }

    private fun requestBluetoothPermissions() {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN
        )

        val permissionsToRequest = ArrayList<String>()

        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(permission)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                permissionsToRequest.toTypedArray(),
                BLUETOOTH_PERMISSION_REQUEST_CODE
            )
        } else {
            pairDevice()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == BLUETOOTH_PERMISSION_REQUEST_CODE) {
            var allPermissionsGranted = true

            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false
                    break
                }
            }

            if (allPermissionsGranted) {
                pairDevice()
            } else {
                // 权限被拒绝
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }
    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val action = intent.action
            Log.d("taggg", "" + action)
            val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
            str = device?.name

            try {
                //回傳的選擇裝置進行配對
                device?.createBond()
            } catch (e: Exception) {
                Log.e("CreateBondError", e.message!!)
            }
        }
    }
    private fun pairDevice() {
        val bluetoothPicker = Intent("android.bluetooth.devicepicker.action.LAUNCH")
        startActivityForResult(bluetoothPicker, BLUETOOTH_DEVICE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == BLUETOOTH_DEVICE_REQUEST_CODE && resultCode == RESULT_OK) {
            val device: BluetoothDevice? = data?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            val deviceName: String? = device?.name
            str = deviceName
        }

    }

    companion object {
        // TODO: Rename parameter arguments, choose names that match
        // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
        private const val ARG_PARAM1 = "param1"
        private const val ARG_PARAM2 = "param2"
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment RecordFragment.
         */
        // TODO: Rename and change types and number of parameters
        fun newInstance(param1: String?, param2: String?): RecordFragment {
            val fragment = RecordFragment()
            val args = Bundle()
            args.putString(ARG_PARAM1, param1)
            args.putString(ARG_PARAM2, param2)
            fragment.arguments = args
            return fragment
        }
    }
}