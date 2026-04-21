package com.example.aquatura.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class CropOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private enum class TouchMode {
        NONE, CREATE, MOVE, CLEAR, // Added CLEAR for X button
        RESIZE_TL, RESIZE_TR, RESIZE_BL, RESIZE_BR, // Corners
        RESIZE_TOP, RESIZE_BOTTOM, RESIZE_LEFT, RESIZE_RIGHT // Sides
    }

    private var currentMode = TouchMode.NONE
    private val selectionRect = RectF()
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    
    private var hasSelection = false
    private val touchRadius = 64f // Hit area
    private val minSelectionSize = 100f

    // Theme Colors
    private val primaryColor = Color.parseColor("#00BCD4") // AquaTura Blue
    private val gridColor = Color.parseColor("#80FFFFFF") // 50% White

    private val borderPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f // Thinner main border
        isAntiAlias = true
    }
    
    private val cornerPaint = Paint().apply {
        color = primaryColor // Use primary color for active feel
        style = Paint.Style.STROKE
        strokeWidth = 10f // Thick corners
        strokeCap = Paint.Cap.SQUARE
        isAntiAlias = true
    }

    private val gridPaint = Paint().apply {
        color = gridColor
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }

    // Hidden handle paint (transparent but interactive)
    private val transparentHandlePaint = Paint().apply {
        color = Color.TRANSPARENT
        style = Paint.Style.FILL
    }
    
    // Removed visible circle handles for cleaner look
    private val handleStrokePaint = Paint().apply {
        color = Color.TRANSPARENT
        style = Paint.Style.STROKE
    }

    private val overlayPaint = Paint().apply {
        color = Color.parseColor("#99000000") // Darker overlay for better focus
        style = Paint.Style.FILL
    }

    private val hintPaint = Paint().apply {
        color = Color.WHITE
        textSize = 48f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    private val closeButtonPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val closeIconPaint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 5f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    var onSelectionChangeListener: ((RectF?) -> Unit)? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (!hasSelection && currentMode != TouchMode.CREATE) {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)
            // Simpler hint
            canvas.drawText(
                "Drag to crop",
                width / 2f,
                height / 2f,
                hintPaint
            )
            return
        }

        // Draw overlay with hole
        canvas.drawRect(0f, 0f, width.toFloat(), selectionRect.top, overlayPaint)
        canvas.drawRect(0f, selectionRect.top, selectionRect.left, selectionRect.bottom, overlayPaint)
        canvas.drawRect(selectionRect.right, selectionRect.top, width.toFloat(), selectionRect.bottom, overlayPaint)
        canvas.drawRect(0f, selectionRect.bottom, width.toFloat(), height.toFloat(), overlayPaint)

        // Draw Rule of Thirds Grid - ONLY when interacting (moving/resizing/creating)
        // Check if user is touching AND not just clicking close button
        if (hasSelection && currentMode != TouchMode.NONE && currentMode != TouchMode.CLEAR) {
            val widthThird = selectionRect.width() / 3
            val heightThird = selectionRect.height() / 3
            
            // Vertical lines
            canvas.drawLine(selectionRect.left + widthThird, selectionRect.top, selectionRect.left + widthThird, selectionRect.bottom, gridPaint)
            canvas.drawLine(selectionRect.left + widthThird * 2, selectionRect.top, selectionRect.left + widthThird * 2, selectionRect.bottom, gridPaint)
            
            // Horizontal lines
            canvas.drawLine(selectionRect.left, selectionRect.top + heightThird, selectionRect.right, selectionRect.top + heightThird, gridPaint)
            canvas.drawLine(selectionRect.left, selectionRect.top + heightThird * 2, selectionRect.right, selectionRect.top + heightThird * 2, gridPaint)
        }

        // Draw Border
        canvas.drawRect(selectionRect, borderPaint)
        
        // Draw Corner Brackets
        val cornerLen = 60f
        // Top Left
        canvas.drawLine(selectionRect.left, selectionRect.top, selectionRect.left + cornerLen, selectionRect.top, cornerPaint)
        canvas.drawLine(selectionRect.left, selectionRect.top, selectionRect.left, selectionRect.top + cornerLen, cornerPaint)
        // Top Right
        canvas.drawLine(selectionRect.right - cornerLen, selectionRect.top, selectionRect.right, selectionRect.top, cornerPaint)
        canvas.drawLine(selectionRect.right, selectionRect.top, selectionRect.right, selectionRect.top + cornerLen, cornerPaint)
        // Bottom Left
        canvas.drawLine(selectionRect.left, selectionRect.bottom - cornerLen, selectionRect.left, selectionRect.bottom, cornerPaint)
        canvas.drawLine(selectionRect.left, selectionRect.bottom, selectionRect.left + cornerLen, selectionRect.bottom, cornerPaint)
        // Bottom Right
        canvas.drawLine(selectionRect.right - cornerLen, selectionRect.bottom, selectionRect.right, selectionRect.bottom, cornerPaint)
        canvas.drawLine(selectionRect.right, selectionRect.bottom - cornerLen, selectionRect.right, selectionRect.bottom, cornerPaint)

        // Draw Handles (Invisible but functionally needed)
        val handleSize = 40f // Larger hit area for invisible handles
        
        // Corners
        drawHandle(canvas, selectionRect.left, selectionRect.top, handleSize)
        drawHandle(canvas, selectionRect.right, selectionRect.top, handleSize)
        drawHandle(canvas, selectionRect.left, selectionRect.bottom, handleSize)
        drawHandle(canvas, selectionRect.right, selectionRect.bottom, handleSize)
        
        // Midpoints
        val centerX = selectionRect.centerX()
        val centerY = selectionRect.centerY()
        
        drawHandle(canvas, centerX, selectionRect.top, handleSize) // Top
        drawHandle(canvas, centerX, selectionRect.bottom, handleSize) // Bottom
        drawHandle(canvas, selectionRect.left, centerY, handleSize) // Left
        drawHandle(canvas, selectionRect.right, centerY, handleSize) // Right

        // Draw Close Button (X)
        drawCloseButton(canvas)
    }

    private fun drawCloseButton(canvas: Canvas) {
        val radius = 32f
        // Floating slightly outside top-right
        val cx = selectionRect.right
        val cy = selectionRect.top - 40f 
        
        // Helper to keep it on screen
        val safeCx = if (cx < radius) radius else if (cx > width - radius) width - radius else cx
        val safeCy = if (cy < radius) radius else if (cy > height - radius) height - radius else cy
        
        // Draw Shadow/Stroke for visibility
        canvas.drawCircle(safeCx, safeCy, radius + 2f, Paint().apply { color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 2f })
        
        // Draw red background circle
        canvas.drawCircle(safeCx, safeCy, radius, closeButtonPaint)
        
        // Draw X
        val size = radius * 0.4f
        canvas.drawLine(safeCx - size, safeCy - size, safeCx + size, safeCy + size, closeIconPaint)
        canvas.drawLine(safeCx + size, safeCy - size, safeCx - size, safeCy + size, closeIconPaint)
        
        // Store position for hit testing
        closeButtonRect.set(safeCx - radius * 1.5f, safeCy - radius * 1.5f, safeCx + radius * 1.5f, safeCy + radius * 1.5f)
    }
    
    private val closeButtonRect = RectF()
    
    private fun drawHandle(canvas: Canvas, x: Float, y: Float, radius: Float) {
        // Draw transparent circle for hit testing debug or just logic
        canvas.drawCircle(x, y, radius, transparentHandlePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialTouchX = x
                initialTouchY = y
                lastTouchX = x
                lastTouchY = y
                
                if (hasSelection) {
                    currentMode = getTouchMode(x, y)
                }
                
                if (currentMode == TouchMode.CLEAR) {
                    clearSelection()
                    return true
                }
                
                if (currentMode == TouchMode.NONE && !hasSelection) {
                    // Only allow creation if NO selection exists
                    currentMode = TouchMode.CREATE
                    hasSelection = true
                    selectionRect.set(x, y, x, y)
                }
                
                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = x - lastTouchX
                val dy = y - lastTouchY
                
                when (currentMode) {
                    TouchMode.CREATE -> {
                        selectionRect.left = min(initialTouchX, x)
                        selectionRect.top = min(initialTouchY, y)
                        selectionRect.right = max(initialTouchX, x)
                        selectionRect.bottom = max(initialTouchY, y)
                    }
                    TouchMode.MOVE -> {
                        val w = selectionRect.width()
                        val h = selectionRect.height()
                        var newLeft = selectionRect.left + dx
                        var newTop = selectionRect.top + dy
                        newLeft = newLeft.coerceIn(0f, width - w)
                        newTop = newTop.coerceIn(0f, height - h)
                        selectionRect.set(newLeft, newTop, newLeft + w, newTop + h)
                    }
                    TouchMode.RESIZE_TL -> {
                        selectionRect.left = min(x, selectionRect.right - minSelectionSize).coerceAtLeast(0f)
                        selectionRect.top = min(y, selectionRect.bottom - minSelectionSize).coerceAtLeast(0f)
                    }
                    TouchMode.RESIZE_TR -> {
                        selectionRect.right = max(x, selectionRect.left + minSelectionSize).coerceAtMost(width.toFloat())
                        selectionRect.top = min(y, selectionRect.bottom - minSelectionSize).coerceAtLeast(0f)
                    }
                    TouchMode.RESIZE_BL -> {
                        selectionRect.left = min(x, selectionRect.right - minSelectionSize).coerceAtLeast(0f)
                        selectionRect.bottom = max(y, selectionRect.top + minSelectionSize).coerceAtMost(height.toFloat())
                    }
                    TouchMode.RESIZE_BR -> {
                        selectionRect.right = max(x, selectionRect.left + minSelectionSize).coerceAtMost(width.toFloat())
                        selectionRect.bottom = max(y, selectionRect.top + minSelectionSize).coerceAtMost(height.toFloat())
                    }
                    TouchMode.RESIZE_TOP -> {
                         selectionRect.top = min(y, selectionRect.bottom - minSelectionSize).coerceAtLeast(0f)
                    }
                    TouchMode.RESIZE_BOTTOM -> {
                        selectionRect.bottom = max(y, selectionRect.top + minSelectionSize).coerceAtMost(height.toFloat())
                    }
                    TouchMode.RESIZE_LEFT -> {
                        selectionRect.left = min(x, selectionRect.right - minSelectionSize).coerceAtLeast(0f)
                    }
                    TouchMode.RESIZE_RIGHT -> {
                         selectionRect.right = max(x, selectionRect.left + minSelectionSize).coerceAtMost(width.toFloat())
                    }
                    else -> {}
                }
                
                lastTouchX = x
                lastTouchY = y
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (currentMode == TouchMode.CREATE && (selectionRect.width() < minSelectionSize || selectionRect.height() < minSelectionSize)) {
                     // If just created and too small, clear it
                     clearSelection()
                } else if (currentMode != TouchMode.NONE) {
                     // Finish other actions
                     if (hasSelection) {
                         // Normalize rect (handle negative width/height during drag)
                         selectionRect.sort()
                         onSelectionChangeListener?.invoke(selectionRect)
                     }
                }
                
                currentMode = TouchMode.NONE
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun getTouchMode(x: Float, y: Float): TouchMode {
        if (!hasSelection) return TouchMode.NONE

        // Check Close Button first via stored rect
        if (closeButtonRect.contains(x, y)) {
            return TouchMode.CLEAR
        }
        
        val centerX = selectionRect.centerX()
        val centerY = selectionRect.centerY()

        // Check corners first
        if (isNear(x, y, selectionRect.left, selectionRect.top)) return TouchMode.RESIZE_TL
        if (isNear(x, y, selectionRect.right, selectionRect.top)) return TouchMode.RESIZE_TR
        if (isNear(x, y, selectionRect.left, selectionRect.bottom)) return TouchMode.RESIZE_BL
        if (isNear(x, y, selectionRect.right, selectionRect.bottom)) return TouchMode.RESIZE_BR
        
        // Check sides
        if (isNear(x, y, centerX, selectionRect.top)) return TouchMode.RESIZE_TOP
        if (isNear(x, y, centerX, selectionRect.bottom)) return TouchMode.RESIZE_BOTTOM
        if (isNear(x, y, selectionRect.left, centerY)) return TouchMode.RESIZE_LEFT
        if (isNear(x, y, selectionRect.right, centerY)) return TouchMode.RESIZE_RIGHT
        
        // Check inside
        if (selectionRect.contains(x, y)) return TouchMode.MOVE
        
        return TouchMode.NONE
    }

    private fun isNear(x1: Float, y1: Float, x2: Float, y2: Float, threshold: Float = touchRadius): Boolean {
        return abs(x1 - x2) < threshold && abs(y1 - y2) < threshold
    }

    fun getSelectionRect(): RectF? {
        return if (hasSelection) selectionRect else null
    }

    fun selectAll() {
        if (width > 0 && height > 0) {
            hasSelection = true
            selectionRect.set(0f, 0f, width.toFloat(), height.toFloat())
            onSelectionChangeListener?.invoke(selectionRect)
            invalidate()
        }
    }

    fun clearSelection() {
        hasSelection = false
        selectionRect.setEmpty()
        onSelectionChangeListener?.invoke(null)
        invalidate()
    }
    
    fun setSelection(rect: RectF) {
        if (width > 0 && height > 0) {
            hasSelection = true
            selectionRect.set(rect)
            invalidate()
            onSelectionChangeListener?.invoke(selectionRect)
        }
    }
}
