package com.example.test

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.android.synthetic.main.fragment_family.*
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.experimental.and


@SuppressLint("MissingPermission")
class FamilyFragment : Fragment() {

    private lateinit var linechart: LineChart
    private var csvList = mutableListOf<List<Float>>()
    private var currentIndex = 0
    private var iapnea_count = 0
    private var apneaList = ArrayList<Float>()
    private var mApneaText = apnea_textview
    private var iheart_count = 0
    private var heartList = ArrayList<Float>()
    private var mHeartText = heart_textview
    private var isVariableTrue = true

    @SuppressLint("NewApi")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_family, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mApneaText = view.findViewById(R.id.apnea_textview)
        mHeartText = view.findViewById(R.id.heart_textview)
        //        csv的讀取
        val inputStream = resources.openRawResource(R.raw.a01_data)
        val reader = BufferedReader(InputStreamReader(inputStream))
        reader.use {
            var line = it.readLine() // 跳過首行標題
            while (line != null) {
                val row = line.split(",") // 以逗號為分隔符號切割每行
                val floatList = row.map { it.toFloat() }
                csvList.add(floatList)
                line = it.readLine()
            }
        }
        initializeChart()
        startChartUpdate()

    }
    override fun onDestroy() {
        super.onDestroy()
        isVariableTrue = false
    }

    private fun startChartUpdate() {
        if (csvList.isNotEmpty()) {

            if (currentIndex < csvList.size) {
                val row = csvList[currentIndex]
                val data = ArrayList<Entry>()
                val windowsize = 360
                var start = 0
                var end = windowsize

                val handler = Handler()
                val delayMillis = 4000 // 延迟1秒

                val updateChart = object : Runnable {
                    override fun run() {
                        if (end > row.size) {
                            handler.removeCallbacks(this)
                            currentIndex++ // 处理下一个索引
                            if (currentIndex < csvList.size) {
                                startChartUpdate() // 处理下一个索引的数据
                            }
                        } else {
                            data.clear()
                            for (i in start until end) {
                                if (i < row.size) {
                                    data.add(Entry(i.toFloat(), row[i]))
                                    apneaList.add(row[i])
                                    heartList.add(row[i])
                                    iapnea_count += 1
                                    iheart_count += 1
                                    if (iapnea_count == 6000 && isVariableTrue == true) {
                                        val apnea_result =
                                            run_apnea_Model(apneaList, "apnea_7672.tflite")
                                        if (apnea_result == 0) {
                                            mApneaText.setTextColor(Color.GREEN)
                                            mApneaText.text = "正常"
                                        } else {
                                            mApneaText.setTextColor(Color.RED)
                                            mApneaText.text = "異常"
                                        }
                                        iapnea_count = 0
                                        apneaList.clear()
                                    }
                                    if (iheart_count == 100 && isVariableTrue==true) {
                                        val after_heartList = linearInterpolation2(heartList,360)
                                        val heart_result =
                                            after_heartList?.let { run_apnea_Model(it, "model_9532.tflite") }
                                        if (heart_result == 0) mHeartText.setTextColor(Color.GREEN)
                                        else mHeartText.setTextColor(Color.RED)
                                        val result = when (heart_result) {
                                            0 -> "Normal"
                                            1 -> "S"
                                            2 -> "V"
                                            3 -> "F"
                                            4 -> "Q"
                                            else -> null
                                        }
                                        mHeartText.text = result
//                                        Log.d("wtf8181","result:"+result)
                                        iheart_count = 0
                                        heartList.clear()
                                    }
                                }
                            }
                            val lineDataSet = LineDataSet(data, "My Data")
                            lineDataSet.color = Color.GREEN
                            lineDataSet.valueTextColor = Color.RED
                            lineDataSet.setDrawCircles(false)
                            val lineData = LineData(lineDataSet)
                            linechart.data = lineData
                            linechart.animateXY(4000, 0)
                            linechart.invalidate()
                            start += windowsize
                            end += windowsize
                            handler.postDelayed(this, delayMillis.toLong())
                        }
                    }
                }

                handler.post(updateChart)
            }
        }
    }

    private fun initializeChart() {
        linechart = view?.findViewById(R.id.linechart)!!
        linechart.apply {
            setTouchEnabled(false)
            setPinchZoom(false)
            description.isEnabled = false
            legend.isEnabled = false
            xAxis.isEnabled = false
            axisLeft.isEnabled = false
            axisRight.isEnabled = false
        }
    }

    fun run_apnea_Model(csvList: ArrayList<Float>, modelname: String): Int {
        var modelinput = 0
        if (modelname == "apnea_7672.tflite") {
            modelinput = 6000
        } else {
            modelinput = 360
        }
        var herttype = 0
        val tflite = Interpreter(loadModelFile(modelname, requireContext()), null)

        // 假设模型的输入为长度为 784 的向量，输出为长度为 10 的向量
        val inputShape = intArrayOf(1, modelinput) // 输入张量的形状
        val outputShape = intArrayOf(1, 5) // 输出张量的形状

// 创建输入张量
        val inputTensor = TensorBuffer.createFixedSize(inputShape, DataType.FLOAT32) // 创建指定形状的张量缓冲区
        val inputBuffer = inputTensor.buffer // 获取底层缓冲区

        inputBuffer.rewind() // 将缓冲区指针重置为起始位置
// 将一维数据加载到输入张量
        val inputData = csvList.toFloatArray()
        val byteinputData = floatArrayToByteArray(inputData)
// 将 inputData 数组中的数据复制到 inputBuffer 缓冲区中
        //inputData.forEachIndexed { index, value -> inputBuffer.putFloat(index, value) }
        inputBuffer.put(byteinputData)
// 创建输出张量
        val outputTensor =
            TensorBuffer.createFixedSize(outputShape, DataType.FLOAT32) // 创建指定形状的张量缓冲区
        tflite.run(inputTensor.buffer, outputTensor.buffer)
        val outputData = outputTensor.floatArray


        // 執行模型
        tflite.run(inputTensor.buffer, outputTensor.buffer)

        if (modelinput == 6000) {
            if (outputData[0] > outputData[1]) herttype = 0
            else herttype = 1
        } else {
            herttype = m(outputData)
        }
        tflite.close()
        return herttype;
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


    fun floatArrayToByteArray(inputData: FloatArray): ByteArray {
        val buffer = ByteBuffer.allocate(inputData.size * 4) // 每个浮点数占用4个字节
        buffer.order(ByteOrder.nativeOrder())
        for (value in inputData) {
            buffer.putFloat(value)
        }
        return buffer.array()
    }

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

    fun linearInterpolation(data: ArrayList<Float>, newLength: Int): ArrayList<Float>? {
        val intData = FloatArray(data.size)

//        for (int i = 0;i < intData.length;i++)Log.d("wtf8181","intdata:"+intData[i]);
        val interpolatedIntData = ArrayList<Float>(newLength)
        val step = (intData.size - 1).toFloat() / (newLength - 1)
        for (i in 0 until newLength) {
            val index = (i * step).toInt()
            val fraction = i * step - index
            if (index == intData.size - 1) {
                interpolatedIntData.add(0.0f)
                interpolatedIntData[i] = intData[index]
            } else {
                interpolatedIntData.add(0.0f)
                val interpolatedValue =
                    Math.round((1 - fraction) * intData[index] + fraction * intData[index + 1])
                interpolatedIntData[i] = interpolatedValue.toFloat()
            }
        }
        return interpolatedIntData
    }
    fun linearInterpolation2(data: ArrayList<Float>, newLength: Int): ArrayList<Float> {
        val interpolatedData = ArrayList<Float>(newLength)

        if (data.size < 2) {
            // 如果原始数据点少于2个，无法进行插值
            interpolatedData.addAll(data)
        } else {
            val step = (data.size - 1).toFloat() / (newLength - 1).toFloat()

            for (i in 0 until newLength) {
                val index = i * step
                val lowerIndex = index.toInt()
                val upperIndex = if (lowerIndex < data.size - 1) lowerIndex + 1 else lowerIndex

                val fraction = index - lowerIndex

                val lowerValue = data[lowerIndex]
                val upperValue = data[upperIndex]

                val interpolatedValue = lowerValue + fraction * (upperValue - lowerValue)
                interpolatedData.add(interpolatedValue)
            }
        }

        return interpolatedData
    }
}