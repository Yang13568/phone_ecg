package com.example.test

import android.content.Context
import android.os.Handler
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.sign

class ApneaService(context: Context?, handler: Handler) {
    private var mHandler: Handler?
    private var stateContext: Context?

    init {
        mHandler = handler
        stateContext = context
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



    fun runModel(rawData: ByteArray):Int {
        var herttype = 0

        // 執行畫圖的 Runnable
        val tflite = Interpreter(loadModelFile("apnea_7672.tflite", stateContext!!), null)

        // 定義輸入張量和輸出張量的形狀
        val inputShape = intArrayOf(1, 6000) // 輸入張量的形狀
        val outputShape = intArrayOf(1, 2) // 輸出張量的形狀

        // 建立並準備輸入張量
        val inputTensor = prepareInputTensor(inputShape, rawData)
        val outputTensor = prepareOutputTensor(outputShape)

        // 執行模型
        tflite.run(inputTensor.buffer, outputTensor.buffer)

        // 獲取輸出數據並執行 m 函式
        val outputData = outputTensor.floatArray
//        for(data in outputData){
//            Log.d("Apena", "DataHandler interpolatedData: $data")
//        }
        val float =  1.0
        if (outputData[0].toDouble() == 1.0 ){
            herttype=0
        }else if (outputData[1].toDouble() == 1.0){
            herttype=1
        }
        tflite.close()
//        mHandler!!.obtainMessage(FamilyFragment.STATE_TYPE, herttype, -1)
//            .sendToTarget()
        return herttype;
    }

    // 創建並準備輸入張量
    private fun prepareInputTensor(
        shape: IntArray,
        data: ByteArray,
    ): TensorBuffer {
        val inputTensor = TensorBuffer.createFixedSize(shape, DataType.FLOAT32)
        val inputBuffer = inputTensor.buffer
        inputBuffer.rewind()
        inputBuffer.put(data)

        return inputTensor
    }

    // 創建輸出張量
    private fun prepareOutputTensor(shape: IntArray): TensorBuffer {
        return TensorBuffer.createFixedSize(shape, DataType.FLOAT32)
    }
}
