package com.example.test

import android.content.Context
import android.os.Handler
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
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



    fun runModel(csvList:MutableList<List<Float>>,data_number:Int):Int {
        var herttype = 0

        val tflite = Interpreter(loadModelFile("apnea_7672.tflite", stateContext!!), null)

//        // 定義輸入張量和輸出張量的形狀
//        val inputShape = intArrayOf(1, 6000) // 輸入張量的形狀
//        val outputShape = intArrayOf(1, 2) // 輸出張量的形狀
//
//        // 建立並準備輸入張量
//        val inputTensor = prepareInputTensor(inputShape, rawData)
//        val outputTensor = prepareOutputTensor(outputShape)


        // 假设模型的输入为长度为 784 的向量，输出为长度为 10 的向量
        val inputShape = intArrayOf(1, 6000) // 输入张量的形状
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


        // 執行模型
        tflite.run(inputTensor.buffer, outputTensor.buffer)

        //val outputData = outputTensor.floatArray

        Log.d("wtf8181","out0:"+outputData[0])
        Log.d("wtf8181","out1:"+outputData[1])

        if (outputData[0].toDouble() == 1.0 ){
            herttype=0
            Log.d("wtf8181","u0")
        }else if (outputData[1].toDouble() == 1.0){
            Log.d("wtf8181","u1")
            herttype=1
        }
        if (outputData[0]>outputData[1])herttype=0
        else herttype = 1
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

    fun floatArrayToByteArray(inputData: FloatArray): ByteArray {
        val buffer = ByteBuffer.allocate(inputData.size * 4) // 每个浮点数占用4个字节
        buffer.order(ByteOrder.nativeOrder())
        for (value in inputData) {
            buffer.putFloat(value)
        }
        return buffer.array()
    }
}
