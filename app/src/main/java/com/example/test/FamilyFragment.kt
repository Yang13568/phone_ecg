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
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_family.*


@SuppressLint("MissingPermission")
class FamilyFragment : Fragment() {
    private lateinit var mBluetoothAdapter: BluetoothAdapter
    private lateinit var mBluetoothService: BluetoothService
    private lateinit var mECGService: ECGService
    private var delay = 0
    val db = FirebaseFirestore.getInstance()
    companion object {
        const val MESSAGE_STATE_CHANGE = 1
        const val MESSAGE_DEVICE_NAME = 2
        const val MESSAGE_TOAST = 3
        const val MESSAGE_READ = 4
        const val MESSAGE_WRITE = 5
        const val DEVICE_NAME = "device_name"
        const val TOAST = "toast_message"
        private const val TAG = "BluetoothService"
        private const val REQUEST_ENABLE_BT = 1
        const val MESSAGE_RAW = 1
        const val MESSAGE_INFO = 2
        const val MESSAGE_KY_STATE = 3
        const val KY_INFO = "KY_Info"
        const val STATE_TYPE = 4
        const val MESSAGE_UPLOAD = 5
    }

    private val mDeviceList: MutableList<BluetoothDevice> = ArrayList()
    private lateinit var mChartView: ChartView
    private lateinit var mStatusTextView: TextView
    private lateinit var mIhrText: TextView
    private lateinit var mTeText: TextView
    private lateinit var showListButton: Button
    private lateinit var mbtn_Scan: Button
    private lateinit var mlv_device: ListView
    private lateinit var mStateText: TextView
    private lateinit var mBTArrayAdapter: ArrayAdapter<String>
    var email = requireActivity().intent.getStringExtra("email")
    private var State_array = mutableListOf<String?>()
    private var frequencyMap = mutableMapOf<String, Int>()
    private val REQUEST_BLUETOOTH_PERMISSION = 1
    private var isDeviceConnected = true
    private val handler = Handler()
    private val runnable = object : Runnable {
        override fun run() {
            update()
            handler.postDelayed(this, 1)
        }

        private fun update() {
            delay = (delay + 1) % 1000
            if (delay == 0 || delay == 500) {
                val cmd: ByteArray = byteArrayOf(0x0D)
                sendCmd(cmd)
                val cmd2: ByteArray = byteArrayOf('W'.toByte(), '+'.toByte(), 0x0D)
                sendCmd(cmd2)
            }
        }
    }

    @SuppressLint("NewApi")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_family, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("Firestore","mail:"+email)
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (mBluetoothAdapter == null) {
            Toast.makeText(requireContext(), "此裝置不支援藍芽", Toast.LENGTH_SHORT).show()
            return
        }
        if (!mBluetoothAdapter.isEnabled) {
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT)
        }
        mStatusTextView = view.findViewById(R.id.textViewStatus)

        mlv_device = view.findViewById(R.id.lv_device)
        showListButton = view.findViewById(R.id.buttonShowList)
        showListButton.setOnClickListener { showDeviceListDialog() }
        mIhrText = view.findViewById(R.id.IHR_Text)
        mTeText = view.findViewById(R.id.TE_Text)
        mbtn_Scan = view.findViewById(R.id.btn_scan)
        mStateText = view.findViewById(R.id.state)
        mbtn_Scan.setOnClickListener {
            scan()
        }
        mBTArrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1)
        mlv_device.adapter = mBTArrayAdapter

        mChartView = view.findViewById(R.id.Chart)
        val displayMetrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels
        mChartView.setX_Axis(screenWidth)
        setupBluetoothService()
        handler.postDelayed(runnable, 1);
    }

    private fun resetECGService() {
        val cmd: ByteArray = byteArrayOf('R'.code.toByte(), 'S'.code.toByte(), 0x0D)
        sendCmd(cmd)
        mECGService.reset()
        mChartView.ClearChart()
    }

    private fun sendCmd(Cmd: ByteArray) {
        // Check that there's actually something to send
        if (Cmd.isNotEmpty()) {
            // Get the message bytes and tell the BluetoothChatService to write
            mBluetoothService.write(Cmd)
        }
    }

    private fun showDeviceListDialog() {
        val builder = AlertDialog.Builder(requireContext())
        mDeviceList.clear()
        builder.setTitle("選擇藍芽裝置")
        mDeviceList.addAll(mBluetoothAdapter.bondedDevices)
        val deviceNames = arrayOfNulls<String>(mDeviceList.size)
        // Check if the BLUETOOTH_CONNECT permission is granted
        if (ActivityCompat.checkSelfPermission(
                requireContext(), android.Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            if (deviceNames.isNotEmpty()) {
                Log.d(TAG, "有再收尋")

                for (i in mDeviceList.indices) {
                    deviceNames[i] = mDeviceList[i].name
                }
                builder.setItems(deviceNames) { _, which ->
                    val selectedDevice = mDeviceList[which]
                    mBluetoothService.connect(selectedDevice)
                }
                builder.show()
            } else {
                Toast.makeText(requireContext(), "沒有以配對的藍芽裝置", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Handle permission not granted here if needed
            // For example, show a message or request permission again
            // or take any appropriate action
            // For now, I'm just printing a log message
            Log.d(TAG, "BLUETOOTH_CONNECT permission not granted.")
        }
    }


    @SuppressLint("MissingPermission")
    private fun scan() {
        if (mBluetoothAdapter.isDiscovering) {
            mBluetoothAdapter.cancelDiscovery()
            Toast.makeText(requireContext(), "Scan Stopped", Toast.LENGTH_SHORT).show()
        } else {
            if (mBluetoothAdapter.isEnabled) {
                mBTArrayAdapter.clear()
                requireContext().registerReceiver(
                    mBluetoothReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND)
                )
                mBluetoothAdapter.startDiscovery()
                Toast.makeText(requireContext(), "Discovery Started", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Bluetooth Not On", Toast.LENGTH_SHORT).show()
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.S)
    private fun checkBluetoothPermissions() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(), android.Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                requireContext(), android.Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(), arrayOf(
                    android.Manifest.permission.BLUETOOTH_CONNECT,
                    android.Manifest.permission.BLUETOOTH_SCAN
                ), REQUEST_BLUETOOTH_PERMISSION
            )
        }
    }

    override fun onStart() {
        super.onStart()
        registerBluetoothReceiver()
    }

    @SuppressLint("MissingPermission")
    override fun onStop() {
        super.onStop()
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.cancelDiscovery()
        }
        unregisterBluetoothReceiver()
    }

    private fun setupBluetoothService() {
        mBluetoothService = BluetoothService(requireContext(), mHandler)
        mECGService = ECGService(requireContext(), mECGHandler)
        mBluetoothService.start()
    }


    private val mHandler = Handler(Handler.Callback { msg ->
        when (msg.what) {
            BluetoothService.STATE_CONNECTED -> {
                mStatusTextView.text = "Bluetooth Status:已連線"
                Log.d("BluetoothService", "handleMessage: " + msg.arg1)
//                resetECGService()
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
//                val data = String(readBuffer, 0, msg.arg1)
//                Log.d("WhatReceive", "handleMessage: " + readBuffer.size)
                mECGService.DataHandler(readBuffer,email)
                Log.d("BluetoothService", "handleMessage arg1: " + msg.arg1)
                Log.d("BluetoothService", "handleMessage what: " + msg.what)
            }

            BluetoothService.MESSAGE_DEVICE_NAME -> {
                if (isDeviceConnected) {
                    val connectedDeviceName = msg.data.getString(BluetoothService.DEVICE_NAME)
                    Log.d("check_state", "connected:重複執行")
                    Toast.makeText(
                        requireContext(), "已連線至 $connectedDeviceName", Toast.LENGTH_SHORT
                    ).show()
                    isDeviceConnected = false
                }
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
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) as BluetoothDevice?
                mBTArrayAdapter.add("${device?.name}\n${device?.address}")
                mBTArrayAdapter.notifyDataSetChanged()
                if (mBTArrayAdapter.getItem(0) != null) Log.d("BluetoothAdapter", "有東西")
                else Log.d("BluetoothAdapter", "沒東西")
            }
            if (BluetoothDevice.ACTION_ACL_DISCONNECTED == action) {
                isDeviceConnected = true
                mStatusTextView.text = "Bluetooth Status:連線中斷"
                mChartView.ClearChart()
                mIhrText.text = "0"
                mTeText.text = "0.0"
                mStateText.text = "--"
            }
        }
    }

    private fun handleBluetoothState(state: Int) {
        Log.d(TAG, "handleBluetoothState: State = $state")
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

    @SuppressLint("NewApi")
    private val mECGHandler = Handler(Handler.Callback { msg ->
        when (msg.what) {
            MESSAGE_RAW -> {
                val rawBuf = msg.obj as ByteArray
                Log.d("BluetoothServiceChart", "handleMessage: " + rawBuf.size)
//                Log.d(
//                    "BluetoothServiceChart",
//                    "Received Data: ${rawBuf.joinToString(", ") { it.toString() }}"
//                )
                mChartView.Wave_Draw(rawBuf)
            }

            MESSAGE_INFO -> {
                val info: List<String> = msg.data.getString(KY_INFO)!!.split("=")
                if (info[0] == "IHR") {
                    val iHr = info[1].toInt()
                    mIhrText.text = iHr.toString()
                } else if (info[0] == "TE" || info[0] == "VER") {
                    // Log.d(TAG, "TE_Data_In")
                    val part1 = info[1].substring(0, 3)
                    try {
                        val tmp1 = (part1.toDouble() / 10) - 4.0
                        var c = tmp1.toString().split("00").toTypedArray()
                        c = c[0].split("99").toTypedArray()
                        if (tmp1 < 0)
                            mTeText.text = "--"
                        if (tmp1 >= 0)
                            mTeText.text = c[0]
                    } catch (e: Exception) {
                        // Handle exception
                    }
                } else {
                    if (info[0] == "HR") {
//                        InfoText.setText("")
                    }
//                    InfoText.append(info[0]+'='+info[1]+',')
                }
            }
            MESSAGE_UPLOAD -> {
                val heartbeat = msg.obj as ArrayList<Float>
                Thread {
                    val data = hashMapOf(
                        "heartbeat" to heartbeat
                    )

                    db.collection("USER")
                        .whereEqualTo("userEmail", email)
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            for (document in querySnapshot.documents) {
                                val documentId = document.id
                                // 在这里处理获取到的文件 ID
                                Log.d("Firestore", "对应的 ID 为：$documentId")
                                db.collection("USER")
                                    .document(documentId)
                                    .collection("Heartbeat_15s")
                                    .add(data)
                            }
                        }
                        .addOnFailureListener { e ->
                            // 查询失败
                            Log.e("Firestore", "查询文件失败：$e")
                        }
                    Log.d("Firestore", "Data: $data")
                }.start()
            }
            MESSAGE_KY_STATE -> {}
            STATE_TYPE -> {
                val heartType = msg.arg1
                val toastMessage = when (heartType) {
                    0 -> "Normal"
                    1 -> "S"
                    2 -> "V"
                    3 -> "F"
                    4 -> "Q"
                    else -> null
                }
                Log.d("ToastService", "handleMessage: $heartType")
                Log.d("ToastService", "handleMessage: $toastMessage")
                if (toastMessage != null) {
                    State_array.add(toastMessage)
                }
                if (State_array.size > 15) {
                    State_array.removeAt(0) // 移除列表中的第一個元素
                }
                if (State_array.size == 15) {
                    frequencyMap.clear()
                    for (message in State_array) {
                        if (message != null) {
                            frequencyMap[message] = frequencyMap.getOrDefault(message, 0) + 1
                        }
                        var mostFrequentToast: String? = null
                        var maxFrequency = 0

                        for ((message, frequency) in frequencyMap) {
                            if (frequency > maxFrequency) {
                                maxFrequency = frequency
                                mostFrequentToast = message
                            }
                        }
                        mStateText.text = mostFrequentToast
                        Log.d("StateService", "handleMessage: $frequencyMap")
                    }
                } else mStateText.text = "評斷中"
                if (toastMessage != null) {
                    Log.d("StateService", "handleMessage: $heartType")
//                    Toast.makeText(requireContext(), toastMessage, Toast.LENGTH_SHORT).show()

                    Log.d("StateService", "handleMessage: $State_array")
//                    Log.d("StateService", "handleMessage: $frequencyMap")
                }
            }

        }
        true
    })
}
