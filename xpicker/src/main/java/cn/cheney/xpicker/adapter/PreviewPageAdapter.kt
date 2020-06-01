package cn.cheney.xpicker.adapter

import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter

class PreviewPageAdapter(private var viewList: List<View>) : PagerAdapter() {

    override fun getCount(): Int {
        return viewList.size
    }


    override fun getItemPosition(`object`: Any): Int {
        return POSITION_NONE
    }

    override fun isViewFromObject(arg0: View, arg1: Any): Boolean {
        return arg0 === arg1
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val view = viewList[position]
        val vp = view.parent
        if (vp != null) {
            val parent = vp as ViewGroup
            parent.removeView(view)
        }
        container.addView(
            view,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        return view
    }
}