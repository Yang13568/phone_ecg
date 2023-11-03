package com.example.test


import MyViewModel
import android.Manifest
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.withStateAtLeast
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.fragment_state.*
import org.w3c.dom.Text
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.ArrayList
import kotlin.math.log


/**
 * A simple [Fragment] subclass.
 * Use the [RecordFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class RecordFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var mParam1: String? = null
    private var mParam2: String? = null
    private lateinit var viewModel: MyViewModel
    private lateinit var dataList: MutableList<String>
    private lateinit var adapter: ArrayAdapter<String>
    private var mtextview: TextView? = null
    private var mbarchart: BarChart? = null
    private var choose_date = ""
    private var record_data = mutableListOf<MutableList<String?>>()
    private var apnea_record_data = mutableListOf<MutableList<String?>>()
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
        viewModel = ViewModelProvider(requireActivity()).get(MyViewModel::class.java)
        var email = viewModel.sharedData
        var change_state = 1
        var choose_time = -1
        var choose_unit = -1
        var choose_mod = -1
        val db = FirebaseFirestore.getInstance()
        val documents = mutableListOf<DocumentSnapshot>()
        val Data = mutableListOf<LongArray>()
        val btn_date = view.findViewById<Button>(R.id.showdate_btn)
        val btn_search = view.findViewById<Button>(R.id.search_btn)
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        //日期選擇
        btn_date.setOnClickListener {
            showDatePicker(requireContext(), btn_date)
            Log.d("wtf8181", "date:" + choose_date)
            Log.d("wtf8181", "time:" + choose_time)
            Log.d("wtf8181", "unit:" + choose_unit)
            Log.d("wtf8181", "mode:" + choose_mod)
        }

        //時間選擇
        val hourPicker = view.findViewById<NumberPicker>(R.id.hourPicker)
        val showhourbtn = view.findViewById<Button>(R.id.showHourButton)
        hourPicker.minValue = 0
        hourPicker.maxValue = 23
        showhourbtn.setOnClickListener {
            showhourbtn.visibility = View.INVISIBLE
            hourPicker.visibility = View.VISIBLE
        }
        val wheelisScrolling = AtomicBoolean(false) // 用于追踪滚轮是否正在滚动

        hourPicker.setOnScrollListener { picker, scrollState ->
            when (scrollState) {
                NumberPicker.OnScrollListener.SCROLL_STATE_IDLE -> {
                    if (wheelisScrolling.get()) {
                        showhourbtn.text = hourPicker.value.toString()
                        choose_time = hourPicker.value
                        hourPicker.visibility = View.INVISIBLE
                        showhourbtn.visibility = View.VISIBLE
                    }
                    wheelisScrolling.set(false)
                }
                NumberPicker.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL -> {
                    wheelisScrolling.set(true)
                }
            }
        }

        //單位選擇
        val numberPicker = view.findViewById<NumberPicker>(R.id.number_Picker)
        val showButton = view.findViewById<Button>(R.id.showPickerButton)
        val data = arrayOf("24h", "12h", "1h")
        numberPicker.minValue = 0
        numberPicker.maxValue = data.size - 1
        numberPicker.displayedValues = data
        showButton.setOnClickListener {
            showButton.visibility = View.INVISIBLE
            numberPicker.visibility = View.VISIBLE
        }
        val isScrolling = AtomicBoolean(false) // 用于追踪滚轮是否正在滚动

        numberPicker.setOnScrollListener { picker, scrollState ->
            when (scrollState) {
                NumberPicker.OnScrollListener.SCROLL_STATE_IDLE -> {
                    if (isScrolling.get()) {
                        when (numberPicker.value) {
                            0 -> showButton.text = "24h"
                            1 -> showButton.text = "12h"
                            2 -> showButton.text = "1h"
                        }
                        choose_unit = numberPicker.value
                        numberPicker.visibility = View.INVISIBLE
                        showButton.visibility = View.VISIBLE
                    }
                    isScrolling.set(false)
                }
                NumberPicker.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL -> {
                    isScrolling.set(true)
                }
            }
        }

        //模式選擇
        val modPicker = view.findViewById<NumberPicker>(R.id.modPicker)
        val showmodbtn = view.findViewById<Button>(R.id.modPickerButton)
        val moddata = arrayOf("心律不整", "睡眠呼吸中止")
        modPicker.minValue = 0
        modPicker.maxValue = moddata.size - 1
        modPicker.displayedValues = moddata
        showmodbtn.setOnClickListener {
            showmodbtn.visibility = View.INVISIBLE
            modPicker.visibility = View.VISIBLE
        }
        val modisScrolling = AtomicBoolean(false) // 用于追踪滚轮是否正在滚动

        modPicker.setOnScrollListener { picker, scrollState ->
            when (scrollState) {
                NumberPicker.OnScrollListener.SCROLL_STATE_IDLE -> {
                    if (modisScrolling.get()) {
                        when (modPicker.value) {
                            0 -> showmodbtn.text = "心律不整"
                            1 -> showmodbtn.text = "睡眠呼吸"
                        }
                        choose_mod = modPicker.value
                        modPicker.visibility = View.INVISIBLE
                        showmodbtn.visibility = View.VISIBLE
                    }
                    modisScrolling.set(false)
                }
                NumberPicker.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL -> {
                    modisScrolling.set(true)
                }
            }
        }
        btn_search.setOnClickListener {
            if (choose_date != "" && choose_time != -1 && choose_mod != -1 && choose_unit != -1) {
                val choosetime = "$choose_date $choose_time:00:00"
                val starttime = convertTimeToTimestamp(choosetime)
                getRecord(starttime, choose_unit, choose_time, choose_mod,view)
            }
        }
        //HeartRecord
        db.collection("USER")
            .whereEqualTo("userEmail", email)
            .get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot.documents) {
                    val documentId = document.id
                    val userRef = db.collection("USER").document(documentId)
                    dataList = mutableListOf()
                    // 获取用户的 Heartbeat_15s 子集合并按照时间戳倒序排序
                    userRef.collection("heartRecord")
                        .orderBy("timestamp", Query.Direction.ASCENDING)
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            // 构建文档列表
                            for (doc in querySnapshot.documents) {
                                documents.add(doc)
                                val timestamp = doc.getTimestamp("timestamp")
                                val state = doc.getString("state")
                                if (timestamp != null) {
                                    record_data.add(
                                        mutableListOf(
                                            timestamp.seconds.toString(),
                                            state
                                        )
                                    )
                                }
                            }

                        }
                }
            }
        //ApneaRecord
        db.collection("USER")
            .whereEqualTo("userEmail", email)
            .get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot.documents) {
                    val documentId = document.id
                    val userRef = db.collection("USER").document(documentId)
                    dataList = mutableListOf()
                    // 获取用户的 Heartbeat_15s 子集合并按照时间戳倒序排序
                    userRef.collection("apneaRecord")
                        .orderBy("timestamp", Query.Direction.ASCENDING)
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            // 构建文档列表
                            for (doc in querySnapshot.documents) {
                                documents.add(doc)
                                val timestamp = doc.getTimestamp("timestamp")
                                val state = doc.getLong("state")?.toString()
                                if (timestamp != null) {
                                    apnea_record_data.add(
                                        mutableListOf(
                                            timestamp.seconds.toString(),
                                            state
                                        )
                                    )
                                }
                            }

                        }
                }
            }
    }

    fun convertTimeToTimestamp(timeString: String): Long {
        // 创建一个 SimpleDateFormat 对象，定义日期和时间格式
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

        try {
            // 使用 SimpleDateFormat 解析日期和时间字符串，得到 Date 对象
            val date = dateFormat.parse(timeString)

            if (date != null) {
                // 获取 Date 对象的时间戳（以毫秒为单位），并将其转换为秒
                val timestamp = date.time / 1000
                return timestamp
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return -1
    }

    fun getRecord(starttime: Long, unit: Int, choosehour: Int, mode: Int, view: View) {
        if (mode == 1) {
            Log.d("wtf8181","睡眠呼吸中止")
            if (unit == 0){
                var Data = MutableList(6) { LongArray(2) }
                var date_array = ArrayList<String>()
                for (i in 0..5) {
                    var x = choosehour + (i * 4)
                    if (x >= 24) x -= 24
                    date_array.add(x.toString())
                }
                for (i in apnea_record_data.indices) {
                    if (apnea_record_data[i][0]?.toLong()!! > starttime && apnea_record_data[i][0]?.toLong()!! <= starttime + 86400) {
                        if (apnea_record_data[i][0]?.toLong()!! > starttime && apnea_record_data[i][0]?.toLong()!! <= starttime + 14400) {
                            Log.d("wtf8181","time:"+apnea_record_data[i][0]+"state:"+apnea_record_data[i][1])
                            when (apnea_record_data[i][1]) {
                                "0" -> Data[0][0]++
                                "1" -> Data[0][1]++
                            }
                        } else if (apnea_record_data[i][0]?.toLong()!! > starttime + 14400 && apnea_record_data[i][0]?.toLong()!! <= starttime + 28800) {
                            Log.d("wtf8181","time:"+apnea_record_data[i][0]+"state:"+apnea_record_data[i][1])
                            when (apnea_record_data[i][1]) {
                                "0" -> Data[1][0]++
                                "1" -> Data[1][1]++
                            }
                        } else if (apnea_record_data[i][0]?.toLong()!! > starttime + 28800 && apnea_record_data[i][0]?.toLong()!! <= starttime + 43200) {
                            Log.d("wtf8181","time:"+apnea_record_data[i][0]+"state:"+apnea_record_data[i][1])
                            when (apnea_record_data[i][1]) {
                                "0" -> Data[2][0]++
                                "1" -> Data[2][1]++
                            }
                        } else if (apnea_record_data[i][0]?.toLong()!! > starttime + 43200 && apnea_record_data[i][0]?.toLong()!! <= starttime + 57600) {
                            Log.d("wtf8181","time:"+apnea_record_data[i][0]+"state:"+apnea_record_data[i][1])
                            when (apnea_record_data[i][1]) {
                                "0" -> Data[3][0]++
                                "1" -> Data[3][1]++
                            }
                        } else if (apnea_record_data[i][0]?.toLong()!! > starttime + 57600 && apnea_record_data[i][0]?.toLong()!! <= starttime + 72000) {
                            Log.d("wtf8181","time:"+apnea_record_data[i][0]+"state:"+apnea_record_data[i][1])
                            when (apnea_record_data[i][1]) {
                                "0" -> Data[4][0]++
                                "1" -> Data[4][1]++
                            }
                        } else if (apnea_record_data[i][0]?.toLong()!! > starttime + 72000 && apnea_record_data[i][0]?.toLong()!! < starttime + 86400) {
                            Log.d("wtf8181","time:"+apnea_record_data[i][0]+"state:"+apnea_record_data[i][1])
                            when (apnea_record_data[i][1]) {
                                "0" -> Data[5][0]++
                                "1" -> Data[5][1]++
                            }
                        }
                    }
                }
                drawchart(view, Data, date_array,mode)
            }else if(unit==1){
                var Data = MutableList(6) { LongArray(2) }
                var date_array = ArrayList<String>()
                for (i in 0..5) {
                    var x = choosehour + (i * 2)
                    if (x >= 24) x -= 24
                    date_array.add(x.toString())
                }
                for (i in apnea_record_data.indices) {
                    if (apnea_record_data[i][0]?.toLong()!! > starttime && apnea_record_data[i][0]?.toLong()!! <= starttime + 43200) {
                        if (apnea_record_data[i][0]?.toLong()!! > starttime && apnea_record_data[i][0]?.toLong()!! <= starttime + 7200) {
                            Log.d("wtf8181","time:"+apnea_record_data[i][0]+"state:"+apnea_record_data[i][1])
                            when (apnea_record_data[i][1]) {
                                "0" -> Data[0][0]++
                                "1" -> Data[0][1]++
                            }
                        } else if (apnea_record_data[i][0]?.toLong()!! > starttime + 7200 && apnea_record_data[i][0]?.toLong()!! <= starttime + 14400) {
                            Log.d("wtf8181","time:"+apnea_record_data[i][0]+"state:"+apnea_record_data[i][1])
                            when (apnea_record_data[i][1]) {
                                "0" -> Data[1][0]++
                                "1" -> Data[1][1]++
                            }
                        } else if (apnea_record_data[i][0]?.toLong()!! > starttime + 14400 && apnea_record_data[i][0]?.toLong()!! <= starttime + 21600) {
                            Log.d("wtf8181","time:"+apnea_record_data[i][0]+"state:"+apnea_record_data[i][1])
                            when (apnea_record_data[i][1]) {
                                "0" -> Data[2][0]++
                                "1" -> Data[2][1]++
                            }
                        } else if (apnea_record_data[i][0]?.toLong()!! > starttime + 21600 && apnea_record_data[i][0]?.toLong()!! <= starttime + 28800) {
                            Log.d("wtf8181","time:"+apnea_record_data[i][0]+"state:"+apnea_record_data[i][1])
                            when (apnea_record_data[i][1]) {
                                "0" -> Data[3][0]++
                                "1" -> Data[3][1]++
                            }
                        } else if (apnea_record_data[i][0]?.toLong()!! > starttime + 28800 && apnea_record_data[i][0]?.toLong()!! <= starttime + 36000) {
                            Log.d("wtf8181","time:"+apnea_record_data[i][0]+"state:"+apnea_record_data[i][1])
                            when (apnea_record_data[i][1]) {
                                "0" -> Data[4][0]++
                                "1" -> Data[4][1]++
                            }
                        } else if (apnea_record_data[i][0]?.toLong()!! > starttime + 36000 && apnea_record_data[i][0]?.toLong()!! < starttime + 43200) {
                            Log.d("wtf8181","time:"+apnea_record_data[i][0]+"state:"+apnea_record_data[i][1])
                            when (apnea_record_data[i][1]) {
                                "0" -> Data[5][0]++
                                "1" -> Data[5][1]++
                            }
                        }
                    }
                }
                drawchart(view, Data, date_array,mode)
            }else if(unit==2){
                var Data = MutableList(6) { LongArray(2) }
                var date_array = ArrayList<String>()
                date_array.add("0")
                date_array.add("10")
                date_array.add("20")
                date_array.add("30")
                date_array.add("40")
                date_array.add("50")
                for (i in apnea_record_data.indices) {
                    if (apnea_record_data[i][0]?.toLong()!! > starttime && apnea_record_data[i][0]?.toLong()!! <= starttime + 3600) {
                        if (apnea_record_data[i][0]?.toLong()!! > starttime && apnea_record_data[i][0]?.toLong()!! <= starttime + 600) {
                            Log.d("wtf8181","time:"+apnea_record_data[i][0]+"state:"+apnea_record_data[i][1])
                            when (apnea_record_data[i][1]) {
                                "0" -> Data[0][0]++
                                "1" -> Data[0][1]++
                            }
                        } else if (apnea_record_data[i][0]?.toLong()!! > starttime + 600 && apnea_record_data[i][0]?.toLong()!! <= starttime + 1200) {
                            Log.d("wtf8181","time:"+apnea_record_data[i][0]+"state:"+apnea_record_data[i][1])
                            when (apnea_record_data[i][1]) {
                                "0" -> Data[1][0]++
                                "1" -> Data[1][1]++
                            }
                        } else if (apnea_record_data[i][0]?.toLong()!! > starttime + 1200 && apnea_record_data[i][0]?.toLong()!! <= starttime + 1800) {
                            Log.d("wtf8181","time:"+apnea_record_data[i][0]+"state:"+apnea_record_data[i][1])
                            when (apnea_record_data[i][1]) {
                                "0" -> Data[2][0]++
                                "1" -> Data[2][1]++
                            }
                        } else if (apnea_record_data[i][0]?.toLong()!! > starttime + 1800 && apnea_record_data[i][0]?.toLong()!! <= starttime + 2400) {
                            Log.d("wtf8181","time:"+apnea_record_data[i][0]+"state:"+apnea_record_data[i][1])
                            when (apnea_record_data[i][1]) {
                                "0" -> Data[3][0]++
                                "1" -> Data[3][1]++
                            }
                        } else if (apnea_record_data[i][0]?.toLong()!! > starttime + 2400 && apnea_record_data[i][0]?.toLong()!! <= starttime + 3000) {
                            Log.d("wtf8181","time:"+apnea_record_data[i][0]+"state:"+apnea_record_data[i][1])
                            when (apnea_record_data[i][1]) {
                                "0" -> Data[4][0]++
                                "1" -> Data[4][1]++
                            }
                        } else if (apnea_record_data[i][0]?.toLong()!! > starttime + 3000 && apnea_record_data[i][0]?.toLong()!! < starttime + 3600) {
                            Log.d("wtf8181","time:"+apnea_record_data[i][0]+"state:"+apnea_record_data[i][1])
                            when (apnea_record_data[i][1]) {
                                "0" -> Data[5][0]++
                                "1" -> Data[5][1]++
                            }
                        }
                    }
                }
                drawchart(view, Data, date_array,mode)
            }
        } else if (mode == 0) {
            Log.d("wtf8181","心律不整")
            if (unit == 0) {//24h
                var Data = MutableList(6) { LongArray(5) }
                var date_array = ArrayList<String>()
                for (i in 0..5) {
                    var x = choosehour + (i * 4)
                    if (x >= 24) x -= 24
                    date_array.add(x.toString())
                }
                for (i in record_data.indices) {
                    if (record_data[i][0]?.toLong()!! > starttime && record_data[i][0]?.toLong()!! <= starttime + 86400) {
                        if (record_data[i][0]?.toLong()!! > starttime && record_data[i][0]?.toLong()!! <= starttime + 14400) {
                            when (record_data[i][1]) {
                                "Normal" -> Data[0][0]++
                                "S" -> Data[0][1]++
                                "V" -> Data[0][2]++
                                "F" -> Data[0][3]++
                                "Q" -> Data[0][4]++
                            }
                        } else if (record_data[i][0]?.toLong()!! > starttime + 14400 && record_data[i][0]?.toLong()!! <= starttime + 28800) {
                            when (record_data[i][1]) {
                                "Normal" -> Data[1][0]++
                                "S" -> Data[1][1]++
                                "V" -> Data[1][2]++
                                "F" -> Data[1][3]++
                                "Q" -> Data[1][4]++
                            }
                        } else if (record_data[i][0]?.toLong()!! > starttime + 28800 && record_data[i][0]?.toLong()!! <= starttime + 43200) {
                            when (record_data[i][1]) {
                                "Normal" -> Data[2][0]++
                                "S" -> Data[2][1]++
                                "V" -> Data[2][2]++
                                "F" -> Data[2][3]++
                                "Q" -> Data[2][4]++
                            }
                        } else if (record_data[i][0]?.toLong()!! > starttime + 43200 && record_data[i][0]?.toLong()!! <= starttime + 57600) {
                            when (record_data[i][1]) {
                                "Normal" -> Data[3][0]++
                                "S" -> Data[3][1]++
                                "V" -> Data[3][2]++
                                "F" -> Data[3][3]++
                                "Q" -> Data[3][4]++
                            }
                        } else if (record_data[i][0]?.toLong()!! > starttime + 57600 && record_data[i][0]?.toLong()!! <= starttime + 72000) {
                            when (record_data[i][1]) {
                                "Normal" -> Data[4][0]++
                                "S" -> Data[4][1]++
                                "V" -> Data[4][2]++
                                "F" -> Data[4][3]++
                                "Q" -> Data[4][4]++
                            }
                        } else if (record_data[i][0]?.toLong()!! > starttime + 72000 && record_data[i][0]?.toLong()!! < starttime + 86400) {
                            when (record_data[i][1]) {
                                "Normal" -> Data[5][0]++
                                "S" -> Data[5][1]++
                                "V" -> Data[5][2]++
                                "F" -> Data[5][3]++
                                "Q" -> Data[5][4]++
                            }
                        }
                    }
                }
                drawchart(view, Data, date_array,mode)
            } else if (unit == 1) {//12h
                var Data = MutableList(6) { LongArray(5) }
                var date_array = ArrayList<String>()
                for (i in 0..5) {
                    var x = choosehour + (i * 2)
                    if (x >= 24) x -= 24
                    date_array.add(x.toString())
                }
                for (i in record_data.indices) {
                    if (record_data[i][0]?.toLong()!! > starttime && record_data[i][0]?.toLong()!! <= starttime + 43200) {
                        if (record_data[i][0]?.toLong()!! > starttime && record_data[i][0]?.toLong()!! <= starttime + 7200) {
                            when (record_data[i][1]) {
                                "Normal" -> Data[0][0]++
                                "S" -> Data[0][1]++
                                "V" -> Data[0][2]++
                                "F" -> Data[0][3]++
                                "Q" -> Data[0][4]++
                            }
                        } else if (record_data[i][0]?.toLong()!! > starttime + 7200 && record_data[i][0]?.toLong()!! <= starttime + 14400) {
                            when (record_data[i][1]) {
                                "Normal" -> Data[1][0]++
                                "S" -> Data[1][1]++
                                "V" -> Data[1][2]++
                                "F" -> Data[1][3]++
                                "Q" -> Data[1][4]++
                            }
                        } else if (record_data[i][0]?.toLong()!! > starttime + 14400 && record_data[i][0]?.toLong()!! <= starttime + 21600) {
                            when (record_data[i][1]) {
                                "Normal" -> Data[2][0]++
                                "S" -> Data[2][1]++
                                "V" -> Data[2][2]++
                                "F" -> Data[2][3]++
                                "Q" -> Data[2][4]++
                            }
                        } else if (record_data[i][0]?.toLong()!! > starttime + 21600 && record_data[i][0]?.toLong()!! <= starttime + 28800) {
                            when (record_data[i][1]) {
                                "Normal" -> Data[3][0]++
                                "S" -> Data[3][1]++
                                "V" -> Data[3][2]++
                                "F" -> Data[3][3]++
                                "Q" -> Data[3][4]++
                            }
                        } else if (record_data[i][0]?.toLong()!! > starttime + 28800 && record_data[i][0]?.toLong()!! <= starttime + 36000) {
                            when (record_data[i][1]) {
                                "Normal" -> Data[4][0]++
                                "S" -> Data[4][1]++
                                "V" -> Data[4][2]++
                                "F" -> Data[4][3]++
                                "Q" -> Data[4][4]++
                            }
                        } else if (record_data[i][0]?.toLong()!! > starttime + 36000 && record_data[i][0]?.toLong()!! < starttime + 43200) {
                            when (record_data[i][1]) {
                                "Normal" -> Data[5][0]++
                                "S" -> Data[5][1]++
                                "V" -> Data[5][2]++
                                "F" -> Data[5][3]++
                                "Q" -> Data[5][4]++
                            }
                        }
                    }
                }
                drawchart(view, Data, date_array,mode)
            } else if (unit == 2) {//1h
                var Data = MutableList(6) { LongArray(5) }
                var date_array = ArrayList<String>()
                date_array.add("0")
                date_array.add("10")
                date_array.add("20")
                date_array.add("30")
                date_array.add("40")
                date_array.add("50")
                for (i in record_data.indices) {
                    if (record_data[i][0]?.toLong()!! > starttime && record_data[i][0]?.toLong()!! <= starttime + 3600) {
                        if (record_data[i][0]?.toLong()!! > starttime && record_data[i][0]?.toLong()!! <= starttime + 600) {
                            when (record_data[i][1]) {
                                "Normal" -> Data[0][0]++
                                "S" -> Data[0][1]++
                                "V" -> Data[0][2]++
                                "F" -> Data[0][3]++
                                "Q" -> Data[0][4]++
                            }
                        } else if (record_data[i][0]?.toLong()!! > starttime + 600 && record_data[i][0]?.toLong()!! <= starttime + 1200) {
                            when (record_data[i][1]) {
                                "Normal" -> Data[1][0]++
                                "S" -> Data[1][1]++
                                "V" -> Data[1][2]++
                                "F" -> Data[1][3]++
                                "Q" -> Data[1][4]++
                            }
                        } else if (record_data[i][0]?.toLong()!! > starttime + 1200 && record_data[i][0]?.toLong()!! <= starttime + 1800) {
                            when (record_data[i][1]) {
                                "Normal" -> Data[2][0]++
                                "S" -> Data[2][1]++
                                "V" -> Data[2][2]++
                                "F" -> Data[2][3]++
                                "Q" -> Data[2][4]++
                            }
                        } else if (record_data[i][0]?.toLong()!! > starttime + 1800 && record_data[i][0]?.toLong()!! <= starttime + 2400) {
                            when (record_data[i][1]) {
                                "Normal" -> Data[3][0]++
                                "S" -> Data[3][1]++
                                "V" -> Data[3][2]++
                                "F" -> Data[3][3]++
                                "Q" -> Data[3][4]++
                            }
                        } else if (record_data[i][0]?.toLong()!! > starttime + 2400 && record_data[i][0]?.toLong()!! <= starttime + 3000) {
                            when (record_data[i][1]) {
                                "Normal" -> Data[4][0]++
                                "S" -> Data[4][1]++
                                "V" -> Data[4][2]++
                                "F" -> Data[4][3]++
                                "Q" -> Data[4][4]++
                            }
                        } else if (record_data[i][0]?.toLong()!! > starttime + 3000 && record_data[i][0]?.toLong()!! < starttime + 3600) {
                            when (record_data[i][1]) {
                                "Normal" -> Data[5][0]++
                                "S" -> Data[5][1]++
                                "V" -> Data[5][2]++
                                "F" -> Data[5][3]++
                                "Q" -> Data[5][4]++
                            }
                        }
                    }
                }
                drawchart(view, Data, date_array,mode)
            }
        } else Log.d("wtf8181", "getRecord Wrong")
    }

    fun showDatePicker(context: Context, btn: Button) {
        val calendar = Calendar.getInstance()
        val datePickerView = LayoutInflater.from(context).inflate(R.layout.date_picker_dialog, null)
        val datePicker = datePickerView.findViewById<DatePicker>(R.id.date_picker)
        datePicker.maxDate = calendar.timeInMillis
        datePicker.init(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ) { _, year, monthOfYear, dayOfMonth ->
            calendar.set(year, monthOfYear, dayOfMonth)
            var format = SimpleDateFormat("yyyy-MM-dd")
            choose_date = format.format(calendar.time)
            format = SimpleDateFormat("MM-dd")
            btn.text = format.format(calendar.time)
        }

        val dialog = AlertDialog.Builder(context).create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(false)
        dialog.setView(datePickerView)
        dialog.setButton(
            DialogInterface.BUTTON_POSITIVE, "OK"
        ) { dialog, which -> dialog.dismiss() }
        dialog.show()
    }

    fun drawchart(view: View, Data: MutableList<LongArray>, date_array: ArrayList<String>,mode: Int) {
        val barChart = view.findViewById<BarChart>(R.id.barChart)
        val entries = ArrayList<BarEntry>()
        for (i in Data.indices) {
            val dataArray = Data[i]
            val stackedValues = mutableListOf<Float>()
            for (j in dataArray.indices) {
                stackedValues.add(dataArray[j].toFloat())
            }
            entries.add(BarEntry(i.toFloat(), stackedValues.toFloatArray()))
        }

        val barDataSet = BarDataSet(entries, "")
        if (mode == 0) {
            barDataSet.setColors(
                Color.GREEN,
                Color.RED,
                Color.MAGENTA,
                Color.YELLOW,
                Color.DKGRAY
            )
            barDataSet.stackLabels = arrayOf("Normal", "S", "V", "F", "Q")
        }else if(mode==1){
            barDataSet.setColors(
                Color.GREEN,
                Color.RED
            )
            barDataSet.stackLabels = arrayOf("正常", "異常")
        }
        barDataSet.setDrawValues(false)
        val barData = BarData(barDataSet)
        barChart.legend.textSize = 20f
        barData.barWidth = 0.5f
        barChart.data = barData
        barChart.setFitBars(true)
        barChart.xAxis.position = (XAxis.XAxisPosition.BOTTOM)
        barChart.description.isEnabled = false
        barChart.xAxis.setDrawGridLines(false)
        barChart.axisRight.isEnabled = false
        barChart.xAxis.axisMinimum = 0F
        barChart.xAxis.axisMaximum = 6F
//                            barChart.axisLeft.axisMaximum = 15F
        barChart.axisLeft.axisMinimum = 0F
        barChart.xAxis.setLabelCount(6, false)
//                            barChart.axisLeft.setLabelCount(15,false)
        barChart.xAxis.axisMinimum = -0.5f
        val formatter = IndexAxisValueFormatter(date_array.toTypedArray())
        barChart.xAxis.valueFormatter = formatter
        barChart.invalidate()
    }

    companion object {
        // TODO: Rename parameter arguments, choose names that match
        // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
        private const val ARG_PARAM1 = "param1"
        private const val ARG_PARAM2 = "param2"

        /**
         * Use this factory method to create a new instance oftaskkill /f /t
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

    data class MyData(
        val firstString: String? = null,
        val secondString: String? = null
    )

}