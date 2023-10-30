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
    private var choose_date=""
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
        var change_state=1
        var choose_time=0
        var choose_unit=0
        var choose_mod=0
        val db = FirebaseFirestore.getInstance()
        val documents = mutableListOf<DocumentSnapshot>()
        val Data = mutableListOf<LongArray>()
        val btn_date = view.findViewById<Button>(R.id.showdate_btn)
        val barChart = view.findViewById<BarChart>(R.id.barChart)
        val entries = ArrayList<BarEntry>()
        var date_array = ArrayList<String>()
        //模式選擇
        val modPicker = view.findViewById<NumberPicker>(R.id.modPicker)
        val showmodbtn = view.findViewById<Button>(R.id.modPickerButton)
        val moddata = arrayOf("心律不整","睡眠呼吸中止")
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
                        when(modPicker.value){
                            0->showmodbtn.text = "心律不整"
                            1->showmodbtn.text = "睡眠呼吸"
                        }
                        choose_mod=modPicker.value
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
                        when(numberPicker.value){
                            0->showButton.text = "24h"
                            1->showButton.text = "12h"
                            2->showButton.text = "1h"
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
        //日期選擇
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) // 你可以根据你的需求选择不同的日期时间格式
        btn_date.setOnClickListener{
            showDatePicker(requireContext(),btn_date)
            Log.d("wtf8181","date:"+choose_date)
            Log.d("wtf8181","time:"+choose_time)
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
                        choose_time=hourPicker.value
                        Log.d("wtf8181","time:"+choose_time)
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

        var counter = 0
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
                            var longarray = longArrayOf(0,0,0,0,0)
                            // 构建文档列表
                            for (doc in querySnapshot.documents) {
                                documents.add(doc)
                                val timestamp = doc.getTimestamp("timestamp")
                                val state = doc.getString("state")
                                if (timestamp != null) {
                                    if (counter == 0){
                                        date_array.add( sdf.format(timestamp.toDate()).toString())
                                        counter++
                                    }else if (counter == 5){
                                        Data.add(longarray)
                                        counter = 0
                                        longarray=longArrayOf(0,0,0,0,0)
                                    }else counter++
                                    when(state){
                                        "Normal" -> longarray[0]++
                                        "S" -> longarray[1]++
                                        "V" -> longarray[2]++
                                        "F" -> longarray[3]++
                                        "Q" -> longarray[4]++
                                    }
                                    val date = timestamp.toDate()
                                    val formattedDate = sdf.format(date)
                                    val displayText = "$formattedDate - $state"
                                    dataList.add(displayText)
                                }
                            }
//                            Log.d("wtf8181","dateArraysize"+date_array.size)
//                            Log.d("wtf8181","datasize"+Data.size)
                            if (date_array.size>Data.size) {
                                date_array.removeAt(date_array.size-1)
//                                Log.d("wtf8181","Data:"+Data.joinToString(", ") { it.joinToString(", ", "[", "]") })
//                                Log.d("wtf8181","date_array:"+date_array)
                            }
                            adapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_list_item_1, dataList)
//                            val listView = view.findViewById<ListView>(R.id.listview)
//                            listView.adapter = adapter
                            for (i in Data.indices) {
                                val dataArray = Data[i]
                                val stackedValues = mutableListOf<Float>()
                                for (j in dataArray.indices){
                                    stackedValues.add(dataArray[j].toFloat())
                                }
//                                Log.d("wtf8181","stack:"+stackedValues)
//                                Log.d("wtf8181","i:"+i)
                                entries.add(BarEntry(i.toFloat(), stackedValues.toFloatArray()))
                            }

                            val barDataSet = BarDataSet(entries,"")
                            barDataSet.setColors(Color.GREEN, Color.RED, Color.BLUE, Color.YELLOW, Color.CYAN)
                            barDataSet.stackLabels = arrayOf("Normal", "S", "V", "F", "Q")
                            barDataSet.setDrawValues(false)
                            val barData = BarData(barDataSet)
                            barData.barWidth = 0.1f
                            barChart.data = barData
                            barChart.setFitBars(true)
//                            barChart.xAxis.position = (XAxis.XAxisPosition.BOTTOM)
                            barChart.description.isEnabled = false
                            barChart.xAxis.labelRotationAngle = 45f // 设置标签旋转角度
                            barChart.xAxis.setDrawGridLines(false)
                            barChart.axisRight.isEnabled = false
                            barChart.xAxis.axisMinimum = 0F
                            barChart.xAxis.axisMaximum = 6F
//                            barChart.axisLeft.axisMaximum = 15F
                            barChart.axisLeft.axisMinimum = 0F
                            barChart.xAxis.setLabelCount(6,false)
//                            barChart.axisLeft.setLabelCount(15,false)
                            barChart.xAxis.axisMinimum = -0.5f
                            val formatter = IndexAxisValueFormatter(date_array.toTypedArray())
                            barChart.xAxis.valueFormatter = formatter
                            barChart.invalidate()
                        }
                }
            }

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
            calendar.set(year,monthOfYear, dayOfMonth)
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