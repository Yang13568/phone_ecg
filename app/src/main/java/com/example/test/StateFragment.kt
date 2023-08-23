package com.example.test

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.firebase.ui.auth.AuthUI.getApplicationContext
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.tabs.TabLayout.Tab
import com.google.firebase.firestore.FirebaseFirestore
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.*
import kotlin.concurrent.schedule


/**
 * A simple [Fragment] subclass.
 * Use the [StateFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class StateFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private lateinit var linechart: LineChart
    private var mParam1: String? = null
    private var mParam2: String? = null
    private var timer: Timer? = null
    private var csvList = mutableListOf<List<Float>>()
    private var anslist = mutableListOf<Float>()
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
        return inflater.inflate(R.layout.fragment_state, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 找到按鈕 bluetooth_btn
        val bluetoothBtn = view.findViewById<Button>(R.id.bluetooth_btn)
        val testBtn = view.findViewById<Button>(R.id.button2)
        val typetextView = view.findViewById<TextView>(R.id.textView11)
        val accuracy_textview = view.findViewById<TextView>(R.id.textView5)
        val linechart = view.findViewById<LineChart>(R.id.linechart)
        var data_number: Int = 0
        // 藍芽按鈕點擊
        bluetoothBtn.setOnClickListener {
            getEcgfromdatabase()
        }
        //測資按鈕點擊
        var l = 0
        var st = 0
        var correct_num = 0
        var sum_num = 0
        testBtn.setOnClickListener() {

            if (st == 0) {
                timer = Timer()
                timer?.schedule(0, 100) {
                    activity?.runOnUiThread {
                        val herttypr = runmodel(l, linechart)
                        if (herttypr == 0) {
                            typetextView.setText("Normal")
                        } else if (herttypr == 1) {
                            typetextView.setText("S")
                        } else if (herttypr == 2) {
                            typetextView.setText("V")
                        } else if (herttypr == 3) {
                            typetextView.setText("F")
                        } else if (herttypr == 4) {
                            typetextView.setText("Q")
                        }
                        sum_num++
                        if (herttypr == anslist[l].toInt()) {
                            correct_num++
                        }
                        val str = "準確率:" + correct_num + "/" + sum_num
                        accuracy_textview.setText(str)
                    }
                    if (l < csvList.size - 1) l++
                    else {
                        timer?.cancel()
                    }
                }

                st = 1
                testBtn.text = "停止"
            } else {
                st = 0
                testBtn.text = "開始"
                timer?.cancel()
                timer?.purge()
            }
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
         * @return A new instance of fragment StateFragment.
         */
        // TODO: Rename and change types and number of parameters
        fun newInstance(param1: String?, param2: String?): StateFragment {
            val fragment = StateFragment()
            val args = Bundle()
            args.putString(ARG_PARAM1, param1)
            args.putString(ARG_PARAM2, param2)
            fragment.arguments = args
            return fragment
        }
    }

    private fun loadModelFile(modelFilePath: String, context: Context): ByteBuffer {
        val assetManager = context.assets
        val fileDescriptor = assetManager.openFd(modelFilePath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    //找最大值
    private fun m(array: FloatArray): Int {
        var maxindex = -1
        var maxnum = 0.0F
        for (i in 0..4) {
            if (array[i] > maxnum) {
                maxnum = array[i]
                maxindex = i
            }
        }
        return maxindex
    }

    fun floatArrayToByteArray(inputData: FloatArray): ByteArray {
        val buffer = ByteBuffer.allocate(inputData.size * 4) // 每个浮点数占用4个字节
        buffer.order(ByteOrder.nativeOrder())
        for (value in inputData) {
            buffer.putFloat(value)
        }
        return buffer.array()
    }


    fun runmodel(data_number: Int, linechart: LineChart): Int {
        var herttype = 0
        //讀csv檔

//        csv的讀取
//        val inputStream = resources.openRawResource(R.raw.mitdb_360_sample)
//        val reader = BufferedReader(InputStreamReader(inputStream))
//        reader.use {
//            var line = it.readLine() // 跳過首行標題
//            while (line != null) {
//                val row = line.split(",") // 以逗號為分隔符號切割每行
//                val floatList = row.map { it.toFloat() }
//                csvList.add(floatList)
//                line = it.readLine()
//            }
//        }
        linechart.apply {
            setTouchEnabled(false)
            setPinchZoom(false)
            description.isEnabled = false
            legend.isEnabled = false
            xAxis.isEnabled = false
            axisLeft.isEnabled = false
            axisRight.isEnabled = false
        }
        drawChart(data_number, linechart)
//畫圖

//        val data = csvList[data_number].toFloatArray()
//        val ecgView = EcgView(requireContext(), data)
//        val ecg = view?.findViewById<ImageView>(R.id.imageView3)
//        val bitmap = ecgView.toBitmap()
//        if (ecg != null) {
//            ecg.setImageBitmap(bitmap)
//        }

        // 執行畫圖的 Runnable
        val tflite = Interpreter(loadModelFile("model_9532.tflite", requireContext()), null)

// 假设模型的输入为长度为 784 的向量，输出为长度为 10 的向量
        val inputShape = intArrayOf(1, 360) // 输入张量的形状
        val outputShape = intArrayOf(1, 5) // 输出张量的形状

// 创建输入张量
        val inputTensor = TensorBuffer.createFixedSize(inputShape, DataType.FLOAT32) // 创建指定形状的张量缓冲区
        val inputBuffer = inputTensor.buffer // 获取底层缓冲区

        inputBuffer.rewind() // 将缓冲区指针重置为起始位置
// 将一维数据加载到输入张量
        val inputData = csvList[data_number].toFloatArray()
        val byteinputData = floatArrayToByteArray(inputData)
// 将 inputData 数组中的数据复制到 inputBuffer 缓冲区中
        //inputData.forEachIndexed { index, value -> inputBuffer.putFloat(index, value) }
        inputBuffer.put(byteinputData)
// 创建输出张量
        val outputTensor =
            TensorBuffer.createFixedSize(outputShape, DataType.FLOAT32) // 创建指定形状的张量缓冲区
        tflite.run(inputTensor.buffer, outputTensor.buffer)
        val outputData = outputTensor.floatArray
        for (i in 0..4) {
            println(outputData[i])
        }
        herttype = m(outputData)
        tflite.close()
        return herttype
    }

    private fun drawChart(index: Int, linechart: LineChart) {
        if (csvList.isNotEmpty()) {
            val lineDataSet = LineDataSet(getData(index), "My Data")
            lineDataSet.color = Color.GREEN
            lineDataSet.valueTextColor = Color.RED
            lineDataSet.setDrawCircles(false)
            val lineData = LineData(lineDataSet)
            linechart.data = lineData
            linechart.animateXY(4000, 0)
        }
    }

    // 讀取 csvList 裡指定索引的資料並回傳一個 Entry 清單
    private fun getData(index: Int): ArrayList<Entry> {
        val data = ArrayList<Entry>()
        val row = csvList[index]
        for (i in row.indices) {
            data.add(Entry(i.toFloat(), row[i]))
        }
        return data
    }

    //從資料庫抓所有心跳
    @SuppressLint("RestrictedApi")
    private fun getEcgfromdatabase() {
        val email = requireActivity().intent.getStringExtra("email")
        val db = FirebaseFirestore.getInstance()
        val ref = db.collection("USER")
        val query = ref.whereEqualTo("userEmail", email)
        var count = 0
        query.get().addOnSuccessListener { documents ->
            for (document in documents) {
                // 取得 "Ecg_Data" 集合的參考
                val ecgDataRef = document.reference.collection("Ecg_Data")

                // 讀取 "Ecg_Data" 集合中的文件資料
                ecgDataRef.get().addOnSuccessListener { ecgDataQuerySnapshot ->
                    for (ecgDataDocument in ecgDataQuerySnapshot.documents) {
                        val ecgData = ecgDataDocument.data // 取得每個文件的資料
                        val ecgDataList = ecgData?.get("ecgData") as List<Float>
                        val newData = ecgDataList.subList(0, 360)
                        val data361 = ecgDataList[360]
                        Log.d("DEBUG", "ans: " + data361)
                        anslist.add(data361)
                        // 將 ecgDataList 添加到 csvList 中
                        csvList.add(newData)
                    }
                }
            }
            val toast = Toast.makeText(
                getApplicationContext(),
                "存取完畢:" + csvList.size.toString(),
                Toast.LENGTH_SHORT
            )
            toast.show()
            Log.d("DEBUG", "Data added to csvList: ${csvList.size}")
        }

    }
}
