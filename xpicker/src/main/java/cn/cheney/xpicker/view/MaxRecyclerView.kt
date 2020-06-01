package cn.cheney.xpicker.view

import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView


class MaxRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {


    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        val heightSpec = MeasureSpec.makeMeasureSpec(
            (Resources.getSystem().displayMetrics.heightPixels * 0.8).toInt(),
            MeasureSpec.AT_MOST
        )
        super.onMeasure(widthSpec, heightSpec)
    }

}