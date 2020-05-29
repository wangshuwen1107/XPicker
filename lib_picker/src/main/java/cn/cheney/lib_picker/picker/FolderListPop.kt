package cn.cheney.lib_picker.picker

import android.content.Context
import android.content.res.Resources
import android.graphics.Rect
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.AnimationUtils
import android.widget.PopupWindow
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.cheney.lib_picker.R
import cn.cheney.lib_picker.adapter.FolderAdapter
import cn.cheney.lib_picker.entity.MediaFolder

typealias FolderDismissListener = (name: String) -> Unit

class FolderListPop(val context: Context, folderList: List<MediaFolder>, var chooseName: String) :
    PopupWindow() {


    private var rootView: View =
        LayoutInflater.from(context).inflate(R.layout.xpicker_pop_folder, null)
    private var folderRv: RecyclerView
    private var folderAdapter: FolderAdapter

    private var animationIn: Animation
    private var animationOut: Animation

    var folderDismissListener: FolderDismissListener? = null

    init {
        folderRv = rootView.findViewById(R.id.folder_rv)
        this.contentView = rootView
        this.isFocusable = true
        this.isOutsideTouchable = true

        folderAdapter = FolderAdapter()
        folderRv.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        folderRv.adapter = folderAdapter
        folderAdapter.folderList = folderList
        folderAdapter.chooseName = chooseName

        width = Resources.getSystem().displayMetrics.widthPixels
        height = Resources.getSystem().displayMetrics.heightPixels

        animationIn = AnimationUtils.loadAnimation(context, R.anim.picker_pop_in)
        animationOut = AnimationUtils.loadAnimation(context, R.anim.picker_pop_out)

        rootView.setOnClickListener {
            dismiss()
        }

        setOnDismissListener {
            folderDismissListener?.invoke(chooseName)
        }

        folderAdapter.itemClickListener = { _, mediaFolder ->
            this@FolderListPop.chooseName = mediaFolder.name
            dismiss()
        }
    }


    override fun showAsDropDown(anchor: View?) {
        if (Build.VERSION.SDK_INT >= 24) {
            val visibleFrame = Rect()
            anchor!!.getGlobalVisibleRect(visibleFrame)
            height = anchor.resources.displayMetrics.heightPixels - visibleFrame.bottom
            super.showAsDropDown(anchor)
        } else {
            super.showAsDropDown(anchor)
        }
        folderRv.startAnimation(animationIn)
    }

    override fun dismiss() {
        folderRv.startAnimation(animationOut)
        animationOut.setAnimationListener(object : AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                super@FolderListPop.dismiss()
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })
    }
}