package com.saeed.pulltorefreshlayout

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator

class PullToRefreshProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = -1,
    defStyleRes : Int = -1,
) : View(context, attrs, defStyleAttr,defStyleRes) {

    private enum class State {
        Filling, Looping
    }

    private var progress = 0f
    private var currentState = State.Filling
    private var baseCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = -0x101011
        style = Paint.Style.STROKE
        strokeWidth = 1.25f.toDpf()
        strokeCap = Paint.Cap.ROUND
    }

    private var fillingCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = -0x383839
        style = Paint.Style.STROKE
        strokeWidth = 1.25f.toDpf()
        strokeCap = Paint.Cap.ROUND
    }

    private var loopingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        val circleCenter: Float = 16.5f.toDpf()
        val colors = intArrayOf(-0x383839, -0x101011)
        shader =  SweepGradient(circleCenter, circleCenter, colors, null)
        style = Paint.Style.STROKE
        strokeWidth = 1.25f.toDpf()
        strokeCap = Paint.Cap.ROUND
    }

    private var circleBound = RectF(0f, 0f, 0f, 0f)
    private var scale = 1.0f
    private val scaleDuration = 200f
    private var scaleStartTime = 0L
    private var showScaleAnimation = false
    private var circlePadding = 1.toDp()
    private var fillingSweepAngle = 270f
    private var loopingStartAngle = 270f
    private val loopingSweepAngle = 180f
    private var loopingStartTime = 0L
    private var currentLoopingTime = 0L
    private val arcTime = 800f
    private val loopingInterpolator = LinearInterpolator()

    init {
        init()
    }

    private fun init() {
        setBackgroundColor(Color.TRANSPARENT)
    }

    fun resetState() {
        currentState = State.Filling
        progress = 0f
        fillingSweepAngle = 270f
    }

    fun setProgress(progress: Float) {
        this.progress = progress
        if (progress < 1.0f) {
            fillingSweepAngle = 360 * progress
        } else if (currentState == State.Filling) {
            showScaleAnimation = true
            currentState = State.Looping
            loopingStartTime = System.currentTimeMillis()
        }
        invalidate()
    }

    private fun updateLoopingProgress() {
        val currentTimeMillis = System.currentTimeMillis()
        if (scaleStartTime == 0L && showScaleAnimation) {
            scaleStartTime = System.currentTimeMillis()
        }
        currentLoopingTime = currentTimeMillis - loopingStartTime
        loopingStartAngle = 360 * loopingInterpolator.getInterpolation(currentLoopingTime / arcTime)
        if (showScaleAnimation) {
            val scaleDelta = currentTimeMillis - scaleStartTime
            val scaleProgress = scaleDelta / scaleDuration
            if (scaleProgress >= 1) {
                scaleStartTime = 0
                scale = 1f
                showScaleAnimation = false
            } else {
                scale = 1 + 0.2f * getInterpolator(scaleProgress)
            }
        }
        invalidate()
    }

    private fun getInterpolator(progress: Float): Float {
        var progress = progress
        return if (progress <= 0.5) {
            progress * 2f
        } else {
            progress = 1 - progress
            1 - (1 - 2 * progress)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val layoutHeight = MeasureSpec.getSize(heightMeasureSpec)
        circleBound.left = circlePadding.toFloat()
        circleBound.right = (layoutHeight - circlePadding).toFloat()
        circleBound.top = circlePadding.toFloat()
        circleBound.bottom = (layoutHeight - circlePadding).toFloat()
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.scale(scale, scale, measuredWidth / 2.0f, measuredHeight / 2.0f)
        canvas.drawArc(circleBound, 0f, 360f, false, baseCirclePaint)
        if (currentState == State.Filling) {
            canvas.drawArc(circleBound, 270f, fillingSweepAngle, false, fillingCirclePaint)
        } else {
            canvas.save()
            canvas.rotate(loopingStartAngle, measuredWidth / 2f, measuredHeight / 2f)
            canvas.drawArc(
                circleBound,
                -loopingStartAngle,
                loopingStartAngle + 360,
                false,
                loopingPaint
            )
            canvas.restore()
            updateLoopingProgress()
        }
    }
}