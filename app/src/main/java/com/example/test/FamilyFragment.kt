package com.example.test

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment

class FamilyFragment : Fragment() {
    private lateinit var mBluetoothAdapter: BluetoothAdapter
    private lateinit var mBluetoothService: BluetoothService

    companion object {
        @kotlin.jvm.JvmField
        val MESSAGE_STATE_CHANGE: Int = 1
        const val MESSAGE_DEVICE_NAME = 2
        const val MESSAGE_TOAST = 3
        const val MESSAGE_READ = 4
        const val MESSAGE_WRITE = 5
        const val DEVICE_NAME = "device_name"
        const val TOAST = "toast_message"
        private const val TAG = "BluetoothService"
        private const val REQUEST_ENABLE_BT = 1
    }

    private val mDeviceList: MutableList<BluetoothDevice> = ArrayList()
    private lateinit var mChartView: ChartView
    private lateinit var mStatusTextView: TextView
    private lateinit var mDataTextView: TextView
    private lateinit var showListButton: Button
    private lateinit var mbtn_Scan: Button
    private lateinit var mlv_device: ListView
    private lateinit var mBTArrayAdapter: ArrayAdapter<String>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_family, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (mBluetoothAdapter == null) {
            Toast.makeText(requireContext(), "此裝置不支援藍芽", Toast.LENGTH_SHORT).show()
            return
        }

        mStatusTextView = view.findViewById(R.id.textViewStatus)
        mDataTextView = view.findViewById(R.id.textViewData)
        mlv_device = view.findViewById(R.id.lv_device)
        showListButton = view.findViewById(R.id.buttonShowList)
        showListButton.setOnClickListener { showDeviceListDialog() }
        mbtn_Scan = view.findViewById(R.id.btn_scan)
        mbtn_Scan.setOnClickListener { scan() }
        mBTArrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1)
        mlv_device.adapter = mBTArrayAdapter

        mChartView = view.findViewById(R.id.Chart)
        val displayMetrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels
        mChartView.setX_Axis(screenWidth)
    }

    private fun resetECGService() {
        val cmd = byteArrayOf('R'.toByte(), 'S'.toByte(), 0x0D)
        sendCmd(cmd)
        if (mChartView != null)
            mChartView.ClearChart()
    }

    private fun sendCmd(Cmd: ByteArray) {
        if (mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
            return
        }

        if (Cmd.isNotEmpty()) {
            mBluetoothService.write(Cmd)
        }
    }

    private fun showDeviceListDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("選擇藍芽裝置")
        val deviceNames = Array(mDeviceList.size) { "" }

        // Check if the BLUETOOTH_CONNECT permission is granted
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            for (i in mDeviceList.indices) {
                deviceNames[i] = mDeviceList[i].name
            }
            builder.setItems(deviceNames) { _, which ->
                val selectedDevice = mDeviceList[which]
                mBluetoothService.connect(selectedDevice)
            }
            builder.show()
        } else {
            // Handle permission not granted here if needed
            // For example, show a message or request permission again
            // or take any appropriate action
            // For now, I'm just printing a log message
            Log.d(TAG, "BLUETOOTH_CONNECT permission not granted.")
        }
    }


    private fun scan() {
        if (mBluetoothAdapter.isDiscovering) {
            mBluetoothAdapter.cancelDiscovery()
            Toast.makeText(requireContext(), "Scan Stopped", Toast.LENGTH_SHORT).show()
        } else {
            if (mBluetoothAdapter.isEnabled) {
                mBTArrayAdapter.clear()
                requireContext().registerReceiver(
                    mBluetoothReceiver,
                    IntentFilter(BluetoothDevice.ACTION_FOUND)
                )
                mBluetoothAdapter.startDiscovery()
                Toast.makeText(requireContext(), "Discovery Started", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Bluetooth Not On", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private val REQUEST_BLUETOOTH_PERMISSION = 1
    @RequiresApi(Build.VERSION_CODES.S)
    private fun checkBluetoothPermissions() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.BLUETOOTH_CONNECT
            ) !=
            PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.BLUETOOTH_SCAN
            ) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(
                    android.Manifest.permission.BLUETOOTH_CONNECT,
                    android.Manifest.permission.BLUETOOTH_SCAN
                ),
                REQUEST_BLUETOOTH_PERMISSION
            )
        } else {
            setupBluetoothService()
        }
    }


    override fun onStart() {
        super.onStart()
        registerBluetoothReceiver()
    }

    override fun onStop() {
        super.onStop()
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.cancelDiscovery()
        }
        unregisterBluetoothReceiver()
    }

    private fun setupBluetoothService() {
        mBluetoothService = BluetoothService(requireContext(), mHandler)
        mBluetoothService.start()
    }

    private val mHandler = Handler(Handler.Callback { msg ->
        when (msg.what) {
            BluetoothService.STATE_CONNECTED -> {
                mStatusTextView.text = "Bluetooth Status:已連線"
                Log.d("BluetoothService", "handleMessage: " + msg.arg1)
            }
            BluetoothService.STATE_CONNECTING -> {
                mStatusTextView.text = "Bluetooth Status:連線中..."
                Log.d("BluetoothService", "handleMessage: " + msg.arg1)
            }
            BluetoothService.STATE_LISTEN -> {
                mStatusTextView.text = "Bluetooth Status:收尋中..."
                Log.d("BluetoothService", "handleMessage: " + msg.arg1)
                handleBluetoothState(msg.arg1)
            }
            BluetoothService.STATE_NONE -> {
                mStatusTextView.text = "Bluetooth Status:未連線"
                Log.d("BluetoothService", "handleMessage: " + msg.arg1)
            }
            BluetoothService.MESSAGE_READ -> {
                val readBuffer = msg.obj as ByteArray
                val data = String(readBuffer, 0, msg.arg1)
                mDataTextView.text = data
                mChartView.Wave_Draw(readBuffer)
                Log.d("BluetoothService", "handleMessage arg1: " + msg.arg1)
                Log.d("BluetoothService", "handleMessage what: " + msg.what)
            }
            BluetoothService.MESSAGE_DEVICE_NAME -> {
                val connectedDeviceName = msg.data.getString(BluetoothService.DEVICE_NAME)
                Toast.makeText(
                    requireContext(),
                    "已連線至 $connectedDeviceName",
                    Toast.LENGTH_SHORT
                ).show()
            }
            BluetoothService.MESSAGE_TOAST -> {
                val toastMessage = msg.data.getString(BluetoothService.TOAST)
                Toast.makeText(requireContext(), toastMessage, Toast.LENGTH_SHORT).show()
            }
        }
        true
    })
    private val mBluetoothReceiver = object : BroadcastReceiver() {
        @SuppressLint("SetTextI18n", "MissingPermission")
        override fun onReceive(context: Context?, intent: Intent) {
            Log.d("BluetoothAdapter", "開始搜尋")
            val action = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                mBTArrayAdapter.add("${device?.name}\n${device?.address}")
                mBTArrayAdapter.notifyDataSetChanged()
                if (mBTArrayAdapter.getItem(0) != null)
                    Log.d("BluetoothAdapter", "有東西")
                else
                    Log.d("BluetoothAdapter", "沒東西")
            }
            if (BluetoothDevice.ACTION_ACL_DISCONNECTED == action) {
                mStatusTextView.text = "Bluetooth Status:連線中斷"
            }
        }
    }

    private fun handleBluetoothState(state: Int) {
        when (state) {
            BluetoothService.STATE_CONNECTED -> {
                mStatusTextView.text = "Bluetooth Status:已連線"
                Log.d("BluetoothService", "handleBluetoothState: Connected")
            }
            BluetoothService.STATE_CONNECTING -> {
                mStatusTextView.text = "Bluetooth Status:連線中..."
                Log.d("BluetoothService", "handleBluetoothState: Connecting")
            }
            BluetoothService.STATE_LISTEN -> {
                mStatusTextView.text = "Bluetooth Status:收尋中..."
                Log.d("BluetoothService", "handleBluetoothState: Listen")
            }
            BluetoothService.STATE_NONE -> {
                mStatusTextView.text = "Bluetooth Status:未連線"
                Log.d("BluetoothService", "handleBluetoothState: None")
            }
        }
    }

    private fun registerBluetoothReceiver() {
        val filter = IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        requireContext().registerReceiver(mBluetoothReceiver, filter)
    }

    private fun unregisterBluetoothReceiver() {
        requireContext().unregisterReceiver(mBluetoothReceiver)
    }


}
