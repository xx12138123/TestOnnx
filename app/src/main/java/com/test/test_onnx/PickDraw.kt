package com.test.test_onnx

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View


class PickDraw : View, Runnable {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    private var results: List<OnnxResult>? = null
    fun setResults(papers: List<OnnxResult>?) {
        this.results = papers
        postInvalidate()
    }

    fun clearResults() {
        results = null
        postInvalidate()
    }

    private var focusX = 0f
    private var focusY = 0f
    private var lastFocus: Long = 0
    fun setFocus(x: Float, y: Float) {
        focusX = x
        focusY = y
        lastFocus = System.currentTimeMillis()
        postInvalidate()
        postDelayed(this, focusShowTime)
    }

    override fun run() {
        invalidate()
    }

    private val paint = Paint()
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        paint.style = Paint.Style.STROKE
        val results = this.results
        if (!results.isNullOrEmpty()) {
            paint.color = Color.BLUE
            paint.strokeWidth = 7f
            for (result in results) {
                canvas.drawRect(result.rectf, paint)
            }
        }
        if (System.currentTimeMillis() - lastFocus < focusShowTime) {
            paint.color = resources.getColor(R.color.white)
            paint.strokeWidth = 5f
            canvas.drawRoundRect(
                focusX - 75,
                focusY - 75,
                focusX + 75,
                focusY + 75,
                20f,
                20f,
                paint
            )
        }
    }

    companion object {
        private const val focusShowTime: Long = 1000
    }
}
