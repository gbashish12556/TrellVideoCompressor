package com.example.trellvideocompressor

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.example.trellvideocompressor.video.VideoCompressor

class VideoCompressModel:VideoCompressor.CompressionListener{

    var isUploadSuccess =  MutableLiveData<String>()
    var failureMMessage =  MutableLiveData<String>()
    var progressValue =  MutableLiveData<Int>()
    var context:Application? = null
    var mVideoCompressor:VideoCompressor? = null

    constructor(context:Application){
        this.context  = context
        mVideoCompressor = VideoCompressor(context!!)
    }

    override fun compressionFinished(status: Int, isVideo: Boolean, fileOutputPath: String?) {
        if (mVideoCompressor!!.isDone) {
            isUploadSuccess.postValue(fileOutputPath)
        }
    }

    override fun onFailure(message: String) {
        failureMMessage.postValue(message)
    }

    override fun onProgress(progress: Int) {
        progressValue.postValue(progress)
    }


    fun startMediaCompression(mInputPath:String,bitRate:Int) {
        mVideoCompressor!!.startCompressing(
            mInputPath,
            bitRate,
            this)
    }

}