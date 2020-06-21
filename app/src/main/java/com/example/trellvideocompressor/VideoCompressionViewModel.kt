package com.example.trellvideocompressor

import android.app.Application
import androidx.annotation.NonNull
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.util.ArrayList

open class VideoCompressionViewModel(application: Application) : AndroidViewModel(application) {

    private var videoCompressModel: VideoCompressModel? = null
    var playVideoButtonStatus: MutableLiveData<String> = MutableLiveData()
    var videoUrl: MutableLiveData<String> = MutableLiveData()
    var screenNo: MutableLiveData<String> = MutableLiveData()

    val failureMessage: MutableLiveData<String>
        get() {
            return videoCompressModel!!.failureMMessage
        }

    val progressValue: MutableLiveData<Int>
        get() {
            return videoCompressModel!!.progressValue
        }

    val isUploadSuccess: MutableLiveData<String>
        get() {
            return videoCompressModel!!.isUploadSuccess
        }


    fun compressAndUploadvideo(inputPath:String, bitRate:Int) {
        videoCompressModel!!.startMediaCompression(inputPath,bitRate)
    }

    init {
        videoCompressModel = VideoCompressModel(application)
    }


}