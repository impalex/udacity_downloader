package com.udacity.downloader.control

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Region
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import com.udacity.downloader.R
import kotlin.math.abs

private const val DEFAULT_TEXT_SIZE = 24f
private const val DEFAULT_PRIMARY_COLOR = android.R.color.holo_green_light
private const val DEFAULT_SECONDARY_COLOR = android.R.color.holo_blue_bright
private const val DEFAULT_CIRCLE_COLOR = android.R.color.holo_orange_light
private const val DEFAULT_TEXT_COLOR = android.R.color.white
private const val DEFAULT_CIRCLE_RADIUS = 16f
private const val DEFAULT_CIRCLE_MARGIN = 8f

class LoadingButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {


    private lateinit var text: String
    private var primaryColor: Int = 0
    private var secondaryColor: Int = 0
    private var circleColor: Int = 0
    private var textColor: Int = 0
    private var secondaryColorStart: Float = 0f
    private var secondaryColorEnd: Float = 0f
    private var circleProgress: Float = 0f
    private var showCircle: Boolean = false
    private var textWidth: Float = 0f
    private var circleRadius: Float = 0f
    private var circleMargin: Float = 0f

    private val paint = Paint().apply {
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        isDither = true
    }


    init {
        isClickable = true
        context.withStyledAttributes(attrs, R.styleable.LoadingButton) {
            text = getString(R.styleable.LoadingButton_buttonText) ?: context.getString(R.string.button_name)
            primaryColor = getColor(R.styleable.LoadingButton_primaryColor, ContextCompat.getColor(context, DEFAULT_PRIMARY_COLOR))
            secondaryColor = getColor(R.styleable.LoadingButton_secondaryColor, ContextCompat.getColor(context, DEFAULT_SECONDARY_COLOR))
            circleColor = getColor(R.styleable.LoadingButton_circleColor, ContextCompat.getColor(context, DEFAULT_CIRCLE_COLOR))
            textColor = getColor(R.styleable.LoadingButton_textColor, ContextCompat.getColor(context, DEFAULT_TEXT_COLOR))
            secondaryColorStart = getFloat(R.styleable.LoadingButton_secondaryColorStart, 0f)
            secondaryColorEnd = getFloat(R.styleable.LoadingButton_secondaryColorEnd, 0f)
            circleProgress = getFloat(R.styleable.LoadingButton_circleProgress, 0f)
            showCircle = getBoolean(R.styleable.LoadingButton_showCircle, false)
            circleRadius = getDimension(
                R.styleable.LoadingButton_radius,
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_CIRCLE_RADIUS, context.resources.displayMetrics)
            )
            circleMargin = getDimension(
                R.styleable.LoadingButton_showCircle,
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_CIRCLE_MARGIN, context.resources.displayMetrics)
            )
            paint.textSize = getDimension(
                R.styleable.LoadingButton_textSize,
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, DEFAULT_TEXT_SIZE, context.resources.displayMetrics)
            )
        }
        textWidth = paint.measureText(text)
    }

    @Suppress("unused")
    fun setSecondaryColorStart(value: Float) {
        secondaryColorStart = value
        invalidate()
    }

    @Suppress("unused")
    fun getSecondaryColorEnd() = secondaryColorEnd

    @Suppress("unused")
    fun setSecondaryColorEnd(value: Float) {
        secondaryColorEnd = value
        invalidate()
    }

    @Suppress("unused")
    fun setText(value: String) {
        if (text != value) {
            text = value
            textWidth = paint.measureText(text)
            invalidate()
        }
    }

    @Suppress("unused")
    fun getCircleProgress() = circleProgress

    @Suppress("unused")
    fun setCircleProgress(value: Float) {
        circleProgress = value
        invalidate()
    }

    @Suppress("unused")
    fun setShowCircle(value: Boolean) {
        if (showCircle != value) {
            showCircle = value
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val h = height.toFloat()
        val showSecondary = abs(secondaryColorStart / secondaryColorEnd - 1) > 0.001
        var startBorder = 0f
        var endBorder = 0f
        if (showSecondary) {
            startBorder = width * secondaryColorStart.coerceAtLeast(0f).coerceAtMost(1f)
            endBorder = width * secondaryColorEnd.coerceAtLeast(0f).coerceAtMost(1f)
            if (startBorder > endBorder)
                startBorder = endBorder.also { endBorder = startBorder }
            canvas.save()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                canvas.clipOutRect(startBorder, 0f, endBorder, h)
            } else {
                @Suppress("DEPRECATION")
                canvas.clipRect(startBorder, 0f, endBorder, h, Region.Op.DIFFERENCE)
            }
        }
        canvas.drawColor(primaryColor)
        if (showSecondary) {
            canvas.restore()
            paint.color = secondaryColor
            canvas.drawRect(startBorder, 0f, endBorder, h, paint)
        }
        paint.color = textColor
        val textX = width / 2f - if (showCircle) circleRadius + circleMargin else 0f
        canvas.drawText(text, textX, height / 2f - (paint.ascent() + paint.descent()) / 2f, paint)
        if (showCircle) {
            val circleX = textX + textWidth / 2f + circleMargin
            val circleY = height / 2f - circleRadius
            paint.color = circleColor
            canvas.drawArc(
                circleX, circleY, circleX + circleRadius * 2, circleY + circleRadius * 2, 0f,
                360f * circleProgress.coerceAtLeast(0f).coerceAtMost(1f), true, paint
            )
        }
    }

}