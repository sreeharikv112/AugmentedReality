package com.augreality

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.drawable.Drawable

class PointerDrawable() : Drawable() {
    val paint: Paint = Paint()
    var enabled: Boolean = false

    override fun draw(canvas: Canvas) {
        var cx : Float = ((canvas.width)/2).toFloat()
        var cy : Float = ((canvas.height)/2).toFloat()
        if(enabled){
            paint.color = Color.GREEN
            canvas.drawCircle(cx,cy,10.0F,paint)
        }else{
            paint.color = Color.GRAY
            canvas.drawText("X",cx,cy,paint)
        }
    }

    override fun setAlpha(alpha: Int) {
    }

    override fun getOpacity(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
    }
}