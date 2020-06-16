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
import androidx.lifecycle.Observer
import java.io.File
import java.lang.Exception


class MainActivity : AppCompatActivity() {

    private var viewModel: VideoCompressionViewModel? = null
    private var compressButton:Button? = null
    private var uploadButton:Button? = null
    private var bitRate:EditText? = null
    private var videoView: VideoView? = null
    private var screenNo: TextView? = null
    private var videoLayout: ConstraintLayout? = null
    private var isCompressed = false
    private var compressedFileUri:Uri? = null
    private var inputPathUri:String? = null
    private var isCompressedFilePlaying:Boolean? = null

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        compressButton = findViewById(R.id.compressButtton)
        uploadButton = findViewById(R.id.uploadButton)
        bitRate = findViewById(R.id.bitRate)
        videoView = findViewById(R.id.videoView)
        videoLayout = findViewById(R.id.videoLayout)
        screenNo = findViewById(R.id.screenNo)

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

                        Toast.makeText(this, "Enter a valid bitrate..", Toast.LENGTH_LONG).show()

                    }
                }catch(e:Exception){
                    Toast.makeText(this, "Enter a valid bitrate..", Toast.LENGTH_LONG).show()
                }
            }
        }

        intialiseViewModel()

    }

    fun intialiseViewModel(){

        viewModel = ViewModelProviders.of(this).get(VideoCompressionViewModel::class.java)

        viewModel!!.failureMessage.observe(this, Observer { message ->
            if (message != "") {
                compressButton!!.isEnabled = true
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        })

        viewModel!!.progressValue.observe(this, Observer { progress ->
            if (progress == 0) {
                compressButton!!.setText("Compressing..")
                Toast.makeText(this, "Compressing..", Toast.LENGTH_LONG).show()
            }
        })

        viewModel!!.isUploadSuccess.observe(this, Observer { path ->
            if (path != "") {
                isCompressed = true
                compressButton!!.isEnabled = true
                Toast.makeText(this, "Successfully Compressed..", Toast.LENGTH_LONG).show()
                compressedFileUri = Uri.fromFile(File(path))
                compressButton!!.setText("Play Compressed Video")
            }
        })

    }

    fun playCompressedFile(){

        if(compressedFileUri != null){

            if(isCompressedFilePlaying == null){

                screenNo!!.setText("Screen 3")
                Toast.makeText(this, "Playing Compressed Video", Toast.LENGTH_LONG).show()
                isCompressedFilePlaying = true
                compressButton!!.setText("Pause")
                playVideo(compressedFileUri!!)

            }else if(isCompressedFilePlaying!!){

                compressButton!!.setText("Play")
                isCompressedFilePlaying = false
                videoView!!.pause()

            }else if(!isCompressedFilePlaying!!){

                compressButton!!.setText("Pause")
                isCompressedFilePlaying = true
                videoView!!.start()

            }
        }
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
                screenNo!!.setText("Screen 2")
                uploadButton!!.visibility = View.GONE
                videoLayout!!.visibility = View.VISIBLE
                playVideo(Uri.fromFile(File(inputPathUri)))

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

    fun playVideo(uriPath:Uri){
        videoView!!.setVideoURI(uriPath);
        videoView!!.start();
    }
}
