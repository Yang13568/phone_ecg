package com.example.test

import android.annotation.SuppressLint
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
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_family.*
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset


@SuppressLint("MissingPermission")
class FamilyFragment : Fragment() {

    @SuppressLint("NewApi")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_family, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mApneaService: ApneaService
        val mChartView: ChartView
        var csvList = mutableListOf<List<Float>>()
        mChartView = view.findViewById(R.id.chart)
        val displayMetrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels
        mChartView.setX_Axis(screenWidth)
        mApneaService = ApneaService(context, Handler())
        Log.d("wtf8181", "123");
        val lines = mutableListOf<Array<Float>>()
        var normal = 0
        var apnea = 0
        try {
//            val inputStream = resources.openRawResource(R.raw.a01_data)
//            val reader = BufferedReader(InputStreamReader(inputStream))
//            var line: String?
//
//            while (reader.readLine().also { line = it } != null) {
//                val row = line!!.split(",") // 逗号分隔符，根据你的 CSV 文件格式修改
//                val frow = row.map { it.toFloat() }
//                lines.add(frow.toTypedArray())
//            }

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

            // 关闭资源
            reader.close()
            inputStream.close()
            for (i in 1..487) {
                val consult = mApneaService.runModel(csvList,i)

                if (consult == 0) {
                    normal++
                } else if (consult == 1) {
                    apnea++
                }
                Log.d("wtf8181", "第$i 筆:${consult}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        Log.d("wtf8181", "normal:" + normal)
        Log.d("wtf8181", "apnea:" + apnea)
    }

    fun floatArrayToByteArray(floatArray: Array<Float>): ByteArray {
        val byteArray = ByteArray(floatArray.size * 4) // 4字节（32位）浮点数

        for (i in floatArray.indices) {
            val floatBits = java.lang.Float.floatToIntBits(floatArray[i])
            byteArray[i * 4] = (floatBits shr 24).toByte()
            byteArray[i * 4 + 1] = (floatBits shr 16).toByte()
            byteArray[i * 4 + 2] = (floatBits shr 8).toByte()
            byteArray[i * 4 + 3] = floatBits.toByte()
        }

        return byteArray
    }

    fun floatArrayToByteArray(inputData: FloatArray): ByteArray {
        val buffer = ByteBuffer.allocate(inputData.size * 4) // 每个浮点数占用4个字节
        buffer.order(ByteOrder.nativeOrder())
        for (value in inputData) {
            buffer.putFloat(value)
        }
        return buffer.array()
    }


}
