package com.cheney.camera2.core

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.media.MediaPlayer.OnPreparedListener
import android.net.Uri
import android.view.Surface

class XMediaPlayer constructor(private var mContext: Context?) {

    private var mMediaPlayer: MediaPlayer? = null

    private var mMediaListener: MediaListener? = null

    interface MediaListener {
        fun onPrepared(mediaPlayer: MediaPlayer?)
        fun onCompleted()
        fun onError()
    }

    fun setMediaListener(listener: MediaListener?) {
        mMediaListener = listener
    }

    fun play(surface: Surface?, uri: Uri?): MediaPlayer? {
        if (null == uri) {
            return null
        }
        stop()
        if (null == mMediaPlayer) {
            mMediaPlayer = MediaPlayer()
        }
        try {
            mMediaPlayer!!.reset()
            mMediaPlayer!!.setSurface(surface)
            mMediaPlayer!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
            mMediaPlayer!!.setOnPreparedListener(mOnPrepareListener)
            mMediaPlayer!!.setOnErrorListener(mOnErrorListener)
            mMediaPlayer!!.setOnCompletionListener(onCompletionListener)
            mMediaPlayer!!.setDataSource(mContext!!, uri)
            mMediaPlayer!!.isLooping = true
            mMediaPlayer!!.prepareAsync()
            return mMediaPlayer
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }


    fun isPlaying(): Boolean {
        return if (null == mMediaPlayer) {
            false
        } else mMediaPlayer!!.isPlaying
    }

    fun stop() {
        if (null == mMediaPlayer) {
            return
        }
        if (mMediaPlayer!!.isPlaying) {
            mMediaPlayer!!.pause()
        }
        mMediaPlayer!!.reset()
        mMediaPlayer!!.release()
        mMediaPlayer = null
    }

    private val mOnPrepareListener = OnPreparedListener { mp ->
        mMediaPlayer!!.start()
        if (null != mMediaListener) {
            mMediaListener!!.onPrepared(mp)
        }
    }

    private var onCompletionListener = OnCompletionListener {
        if (null != mMediaListener) {
            mMediaListener!!.onCompleted()
        }
    }

    private var mOnErrorListener =
        MediaPlayer.OnErrorListener { mp, what, extra ->
            if (null != mMediaListener) {
                mMediaListener!!.onError()
            }
            true
        }
}