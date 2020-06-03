package cn.cheney.xpicker.view

import android.app.Dialog
import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import cn.cheney.xpicker.R

class LoadingDialog(context: Context) : Dialog(context, R.style.XPicker_LoadingDialog) {

    private var loadingTxt: TextView
    private var rootView: View = View.inflate(context, R.layout.xpicker_loading_dialog, null)
    private var content: String? = null

    init {
        setContentView(rootView)
        loadingTxt = rootView.findViewById(R.id.loading_tv)
        loadingTxt.text = content
        val params = window!!.attributes
        params.width = ViewGroup.LayoutParams.WRAP_CONTENT
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT
        window!!.attributes = params
        window!!.setGravity(Gravity.CENTER)
    }

    fun showLoading(text: String?) {
        content = text
        loadingTxt.text = content
        if (!isShowing) {
            show()
        }
    }
}