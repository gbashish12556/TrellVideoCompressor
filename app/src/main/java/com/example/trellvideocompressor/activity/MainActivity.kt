package com.example.trellvideocompressor.activity

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import com.example.trellvideocompressor.R
import com.example.trellvideocompressor.VideoCompressionViewModel
import android.provider.MediaStore
import android.content.Intent
import android.app.Activity
import android.net.Uri
import android.database.Cursor
import android.util.Log
import android.view.View
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil.setContentView
import androidx.lifecycle.Observer
import butterknife.BindView
import butterknife.ButterKnife
import com.example.trellvideocompressor.databinding.ActivityMainBinding
import java.io.File
import java.lang.Exception


class MainActivity : AppCompatActivity() {


    @BindView(R.id.compressButtton) lateinit var compressButton: Button
    @BindView(R.id.uploadButton) lateinit var uploadButton: Button
    @BindView(R.id.bitRate) lateinit var bitRate: EditText
    @BindView(R.id.videoView) lateinit var videoView: VideoView
    @BindView(R.id.videoLayout) lateinit var videoLayout: ConstraintLayout

    private var viewModel: VideoCompressionViewModel? = null
    private var isCompressed = false
    private var compressedFileUri:String? = null
    private var inputPathUri:String? = null
    private var isCompressedFilePlaying:Boolean? = null

    var dataBinding: ActivityMainBinding? = null

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dataBinding = setContentView(this,R.layout.activity_main)
        ButterKnife.bind(this)
        viewModel = ViewModelProviders.of(this).get(VideoCompressionViewModel::class.java)
        dataBinding!!.viewModel = viewModel


        uploadButton!!.setOnClickListener({
            checkPermission()
        })

        compressButton!!.setOnClickListener{

            if(isCompressed){

                playCompressedFile()

            }else{

                var bitrate = bitRate!!.text.toString()

                try {
                    if (!bitrate.equals("") && Integer.parseInt(bitrate) is Int) {
                        compressButton!!.isEnabled = false
                        Thread(Runnable {
                            viewModel!!.compressAndUploadvideo(
                                inputPathUri!!,
                                Integer.parseInt(bitrate)
                            )
                        }).start()

                    } else {

                        Toast.makeText(this, resources.getString(R.string.enter_valid_bitrate), Toast.LENGTH_LONG).show()

                    }
                }catch(e:Exception){
                    Toast.makeText(this, resources.getString(R.string.enter_valid_bitrate), Toast.LENGTH_LONG).show()
                }
            }
        }

        intialiseViewModel()

    }

    fun intialiseViewModel(){

        viewModel!!.playVideoButtonStatus.value = resources.getString(R.string.compress_video)
        viewModel!!.screenNo.value = resources.getString(R.string.screen1)

        viewModel!!.videoUrl.observe(this, Observer { videoUrl ->
            videoView!!.setVideoURI(Uri.fromFile(File(videoUrl)))
        })

        viewModel!!.failureMessage.observe(this, Observer { message ->
            if (message != "") {
                compressButton!!.isEnabled = true
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        })

        viewModel!!.progressValue.observe(this, Observer { progress ->
                viewModel!!.playVideoButtonStatus.postValue(resources.getString(R.string.compressing))
                Toast.makeText(this, resources.getString(R.string.compressing), Toast.LENGTH_LONG).show()
                 dataBinding!!.invalidateAll()
        })

        viewModel!!.isUploadSuccess.observe(this, Observer { path ->
            if (path != "") {
                isCompressed = true
                compressButton!!.isEnabled = true
                Toast.makeText(this, resources.getString(R.string.successfully_compressed), Toast.LENGTH_LONG).show()
                compressedFileUri = path
                viewModel!!.playVideoButtonStatus.value =resources.getString(R.string.play_compressed_video)
                dataBinding!!.invalidateAll()
            }
        })

    }

    fun playCompressedFile(){

        if(compressedFileUri != null){

            if(isCompressedFilePlaying == null){
                Toast.makeText(this, resources.getString(R.string.playing_compressed_video), Toast.LENGTH_LONG).show()
                viewModel!!.screenNo.value = resources.getString(R.string.screen3)
                isCompressedFilePlaying = true
                viewModel!!.playVideoButtonStatus.value = resources.getString(R.string.pause)
                playVideo(compressedFileUri!!)

            }else if(isCompressedFilePlaying!!){

                viewModel!!.playVideoButtonStatus.value = resources.getString(R.string.play)
                isCompressedFilePlaying = false
                videoView!!.pause()

            }else if(!isCompressedFilePlaying!!){

                viewModel!!.playVideoButtonStatus.value = resources.getString(R.string.pause)
                isCompressedFilePlaying = true
                videoView!!.start()

            }
        }
        dataBinding!!.invalidateAll()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            1 -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    getFile()
                } else {
                    checkPermission()
                }
                return
            }
            else -> {
                checkPermission()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun checkPermission(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
            getFile()
        }else{
            val permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
            this.requestPermissions(permissions, 1)
        }
    }

    fun getFile(){
           var intent =  Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent , 2);
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode === Activity.RESULT_OK) {

            if (requestCode === 2) {

                val selectedVideoUri = data!!.getData()
                inputPathUri = getPath(selectedVideoUri!!)
                viewModel!!.screenNo.value = resources.getString(R.string.screen2)
                uploadButton!!.visibility = View.GONE
                videoLayout!!.visibility = View.VISIBLE
                viewModel!!.videoUrl.value = inputPathUri
                dataBinding!!.invalidateAll()
                playVideo(inputPathUri!!)

            }
        }
    }

    fun getPath(uri: Uri): String? {
        var cursor: Cursor? = null
        try {
            val proj = arrayOf(MediaStore.Images.Media.DATA)
            cursor = this.getContentResolver().query(uri, proj, null, null, null)
            val column_index = cursor!!.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor!!.moveToFirst()
            return cursor!!.getString(column_index)
        } finally {
            if (cursor != null) {
                cursor!!.close()
            }
        }
    }

    fun playVideo(urlPath:String){
        viewModel!!.videoUrl.value = urlPath
        videoView!!.start();
    }
}
