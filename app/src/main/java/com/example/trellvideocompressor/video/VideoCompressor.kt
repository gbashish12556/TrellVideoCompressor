package com.example.trellvideocompressor.video

import android.app.Application
import android.os.Environment
import android.util.Log
import com.github.hiteshsondhi88.libffmpeg.FFmpeg
import com.github.hiteshsondhi88.libffmpeg.FFmpegExecuteResponseHandler
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler

import java.io.File
import java.util.*

class VideoCompressor(var context: Application) {

    private var isFinished: Boolean = false
    private var status = NONE
    private var errorMessage = "Compression Failed!"

    val appDir: String
        get() {
            var outputPath = Environment.getExternalStorageDirectory().absolutePath
            outputPath += "/" + "CompressedVideos"
            var file = File(outputPath)
            if (!file.exists()) {
                file.mkdir()
            }
            return outputPath
        }

    val isDone: Boolean
        get() = status == SUCCESS || status == NONE


    fun startCompressing(inputPath: String?, bitRate:Int,listener: CompressionListener?) {
        if (inputPath == null || inputPath.isEmpty()) {
            status = NONE
            listener?.compressionFinished(NONE, false, null)
            return
        }
        var timeNow = Date().time
        var outputPath = ""
        outputPath = "$appDir/video_compress_"+timeNow.toString()+".mp4"
        val commandParams = arrayOfNulls<String>(26)
        commandParams[0] = "-y"
        commandParams[1] = "-i"
        commandParams[2] = inputPath
        commandParams[3] = "-s"
        commandParams[4] = "240x320"
        commandParams[5] = "-r"
        commandParams[6] = "20"
        commandParams[7] = "-c:v"
        commandParams[8] = "libx264"
        commandParams[9] = "-preset"
        commandParams[10] = "ultrafast"
        commandParams[11] = "-c:a"
        commandParams[12] = "copy"
        commandParams[13] = "-me_method"
        commandParams[14] = "zero"
        commandParams[15] = "-tune"
        commandParams[16] = "fastdecode"
        commandParams[17] = "-tune"
        commandParams[18] = "zerolatency"
        commandParams[19] = "-strict"
        commandParams[20] = "-2"
        commandParams[21] = "-b:v"
        commandParams[22] = bitRate.toString()+"k"
        commandParams[23] = "-pix_fmt"
        commandParams[24] = "yuv420p"
        commandParams[25] = outputPath

        compressVideo(commandParams, outputPath, listener)

    }

    private fun compressVideo(
        command: Array<String?>,
        outputFilePath: String,
        listener: CompressionListener?
    ) {
        try {
        var ffmpeg = FFmpeg.getInstance(context)
        ffmpeg.loadBinary(object: LoadBinaryResponseHandler() {
            override fun onFailure() {
                Log.d("LoadBinaryResponseHandler","failure")
            }

            override fun onFinish() {
                Log.d("LoadBinaryResponseHandler","onFinish")
            }

            override fun onStart() {
                Log.d("LoadBinaryResponseHandler","onStart")
            }

            override fun onSuccess() {
                Log.d("LoadBinaryResponseHandler","onSuccess")
            }

        });

        ffmpeg.execute(command, object : FFmpegExecuteResponseHandler {
                override  fun onSuccess(message: String) {
                    status = SUCCESS
                }

                override  fun onProgress(message: String) {
                    status = RUNNING
                }

                override  fun onFailure(message: String) {
                    status = FAILED
                    Log.e("VideoCompressor", message)
                    listener?.onFailure("Error : $message")
                }

                override  fun onStart() {
                    listener!!.onProgress(0)
                }

                override  fun onFinish() {
                    Log.e("VideoCronProgress", "finnished")
                    isFinished = true
                    listener?.compressionFinished(status, true, outputFilePath)
                }
            })
        } catch (e: Exception) {
            status = FAILED
            errorMessage = e.message.toString()
            listener?.onFailure("Error : " + e.message.toString())
        }

    }

    interface CompressionListener {
        fun compressionFinished(status: Int, isVideo: Boolean, fileOutputPath: String?)

        fun onFailure(message: String)

        fun onProgress(progress: Int)
    }

    companion object {

        val SUCCESS = 1
        val FAILED = 2
        val NONE = 3
        val RUNNING = 4
    }

}
