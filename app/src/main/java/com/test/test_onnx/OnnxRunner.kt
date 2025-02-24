package com.test.test_onnx

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import java.nio.IntBuffer

class OnnxRunner(context: Context) {
    private val modelName = "yolov10n_nms.onnx"
    private val ortEnvironment = OrtEnvironment.getEnvironment()
    private val ortSession = ortEnvironment.createSession(context.assets.open(modelName).readBytes())
    private val inputName = ortSession.inputNames.iterator().next()

    private val preSession = ortEnvironment.createSession(context.assets.open("rgb_processor.onnx").readBytes())
    private val preInputName = preSession.inputNames.iterator().next()

    private val inputDim = 640
    private val inputBuffer = IntArray(inputDim * inputDim)

    @Synchronized
    fun predict(inputImage: Bitmap): List<OnnxResult>{
        var t = System.currentTimeMillis()
        padBitmap(inputImage).getPixels(inputBuffer, 0, inputDim, 0, 0, inputDim, inputDim)

        val inputTensor = OnnxTensor.createTensor(
            ortEnvironment,
            IntBuffer.wrap(inputBuffer),
            longArrayOf(1, inputDim.toLong(), inputDim.toLong())
        )
        val outputBGR = preSession.run(mapOf(preInputName to inputTensor))
        Log.d("onnx", "convert time: ${System.currentTimeMillis() - t}ms")

        t = System.currentTimeMillis()
        val outputs = ortSession.run(mapOf(inputName to outputBGR.get(0) as OnnxTensor))
        Log.d("onnx", "run time: ${System.currentTimeMillis() - t}ms")

        val outputTensor = outputs.get(0) as OnnxTensor
        val outputBuffer = outputTensor.floatBuffer

        val scale = getScale(inputImage)
        val results = mutableListOf<OnnxResult>()
        var i = 0
        while(outputBuffer[i+4] > 0.2f && i < outputBuffer.limit() - 6){
            results.add(OnnxResult(RectF(
                ((outputBuffer[i]/scale)),
                ((outputBuffer[i+1]/scale)),
                ((outputBuffer[i+2])/scale),
                ((outputBuffer[i+3])/scale)
            ), outputBuffer[i+4], outputBuffer[i+5].toInt()))
            i += 6
        }

        Log.d("output", "detect ${results.size}")
        return results
    }

    fun drawResult(bitmap: Bitmap, results: List<OnnxResult>): Bitmap{
        val drawBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(drawBitmap)
        val paint = Paint().apply {
            color = Color.BLUE
            strokeWidth = 5f
            style = Paint.Style.STROKE
            textSize = 40f
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        for(result in results){
            canvas.drawText("${result.type}: ${result.score}",
                result.rect.left.toFloat(), (result.rect.top-20).toFloat(), paint)
            canvas.drawRect(result.rect, paint)
        }
        return drawBitmap
    }

    private val padBitmap = Bitmap.createBitmap(inputDim, inputDim, Bitmap.Config.ARGB_8888)
    private val canvas = Canvas(padBitmap)
    private fun padBitmap(bitmap: Bitmap): Bitmap{
        canvas.drawColor(Color.WHITE)
        val matrix = Matrix()

        matrix.apply {
            if(bitmap.width > bitmap.height){
                val scale = inputDim / bitmap.width.toFloat()
                setScale(scale, scale)
            }else{
                val scale = inputDim / bitmap.height.toFloat()
                setScale(scale, scale)
            }
        }
        val scale = if(bitmap.width > bitmap.height){
            inputDim / bitmap.width.toFloat()
        }else{
            inputDim / bitmap.height.toFloat()
        }

        matrix.setScale(scale, scale)
        canvas.drawBitmap(bitmap, matrix, Paint())
        canvas.save()
        return padBitmap
    }
    private fun getScale(bitmap: Bitmap): Float{
        return if(bitmap.width > bitmap.height){
            inputDim / bitmap.width.toFloat()
        }else{
            inputDim / bitmap.height.toFloat()
        }
    }
}

data class OnnxResult(
    val rectf: RectF,
    val score: Float,
    val type: Int,
){
    val rect = Rect(rectf.left.toInt(), rectf.top.toInt(), rectf.right.toInt(), rectf.bottom.toInt())
}