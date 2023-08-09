package com.example.test

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast

class bluetooth : AppCompatActivity() {
    private var device: BluetoothDevice? = null
    private var adapter: BluetoothAdapter? = null
    private var deviceName: String? = null
    private var deviceAddress: String? = null
    private var showDevice: TextView? = null
    //private var dataText: EditText? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth)
        showDevice = findViewById(R.id.textView)
        //dataText = findViewById(R.id.editTextTextPersonName)

        //藍芽調配器
        adapter = BluetoothAdapter.getDefaultAdapter()
        // bluetooth抓到設備發送廣播
        val filter = IntentFilter("android.bluetooth.devicepicker.action.DEVICE_SELECTED")
        if (receiver != null) {
            registerReceiver(receiver, filter)//廣播
        }
    }

    //廣播回傳
    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            Log.d("taggg", "" + action)
            device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            deviceName = device?.name
            deviceAddress = device?.address // MAC address
            showDevice?.text = "配對裝置:$deviceName\n位址:$deviceAddress"
            try {
                //回傳的選擇裝置進行配對
                device?.createBond()
            } catch (e: Exception) {
                Log.e("CreateBondError", e.message.toString())
            }
        }
    }

    //配對按鈕
    fun pairDevice(view: View?) {
        //當藍芽未開啟
        if (!adapter!!.isEnabled) {
            Toast.makeText(view?.context, "先開權限後再點擊按鈕", Toast.LENGTH_SHORT).show()
            //打開藍芽窗(問你是否打開藍芽)
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            startActivity(intent)
        } else {
            //藍芽scanner
            Toast.makeText(view?.context, "PairDevice", Toast.LENGTH_SHORT).show()
            val bluetoothPicker = Intent("android.bluetooth.devicepicker.action.LAUNCH")
            startActivity(bluetoothPicker)
            /*打開手機藍芽頁面
            Intent intentSettings = new Intent();
            intentSettings.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(intentSettings);
            */
        }
    }
}