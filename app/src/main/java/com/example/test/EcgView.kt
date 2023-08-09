package com.example.test
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View

class EcgView(context: Context, var data: FloatArray) : View(context) {

    // 画笔
    private val paint = Paint()

    init {
        // 初始化画笔
        paint.color = Color.GREEN
        paint.strokeWidth = 5f
        paint.style = Paint.Style.STROKE
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 计算画布的宽度和高度
        val canvasWidth = canvas.width
        val canvasHeight = canvas.height

        // 计算每个数据点的水平间距
        val dataWidth = canvasWidth.toFloat() / data.size

        // 计算垂直中心线的位置
        val centerY = canvasHeight.toFloat() / 2

        // 绘制心电图
        var x = 0f
        var y = centerY - data[0] * centerY
        canvas.drawLine(x, y, x, y, paint)
        for (i in 1 until data.size) {
            val newY = centerY - data[i] * centerY
            canvas.drawLine(x, y, x + dataWidth, newY, paint)
            x += dataWidth
            y = newY
        }
    }

    fun toBitmap(): Bitmap {
        // 创建一个 Bitmap 对象，并在其中绘制心电图
        val bitmap = Bitmap.createBitmap(3000, 400, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        draw(canvas)
        return bitmap
    }
}