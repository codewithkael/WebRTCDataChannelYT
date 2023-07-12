package com.codewithkael.webrtcdatachannelty.ui

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.codewithkael.webrtcdatachannelty.R
import com.codewithkael.webrtcdatachannelty.databinding.ActivityMainBinding
import com.codewithkael.webrtcdatachannelty.repository.MainRepository
import com.codewithkael.webrtcdatachannelty.utils.DataConverter
import com.codewithkael.webrtcdatachannelty.utils.getFilePath
import dagger.hilt.android.AndroidEntryPoint
import org.webrtc.DataChannel
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), MainRepository.Listener {

    private var username: String? = null
    private var imagePathToSend:String? = null

    @Inject
    lateinit var mainRepository: MainRepository

    private lateinit var views: ActivityMainBinding

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if(isGranted){
                //open gallery
                pickImageLauncher.launch("image/*")
            }else{
                Toast.makeText(this, "we need storage permission", Toast.LENGTH_SHORT).show()
            }

        }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()){uri ->
            //convert the uri to real path
            uri?.let {
                val imagePath : String? = it.getFilePath(this)
                if (imagePath!=null){
                    this.imagePathToSend = imagePath
                    Glide.with(this).load(imagePath).into(views.sendingImageView)
                } else {
                    Toast.makeText(this, "image was not found", Toast.LENGTH_SHORT).show()
                }
            }

        }

    private fun openGallery(){
        val permission = android.Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(
                this,permission
        ) != PackageManager.PERMISSION_GRANTED ) {
            requestPermissionLauncher.launch(permission)
        } else {
            pickImageLauncher.launch("image/*")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        views = ActivityMainBinding.inflate(layoutInflater)
        setContentView(views.root)
        init()
    }

    private fun init() {
        username = intent.getStringExtra("username")
        if (username.isNullOrEmpty()) {
            finish()
        }

        mainRepository.listener = this
        mainRepository.init(username!!)

        views.apply {
            requestBtn.setOnClickListener {
                if (targetEt.text.toString().isEmpty()) {
                    Toast.makeText(this@MainActivity, "hey, Fill up the target", Toast.LENGTH_SHORT)
                        .show()
                    return@setOnClickListener
                }
                mainRepository.sendStartConnection(targetEt.text.toString())
            }
            sendImageButton.setOnClickListener {
                if (imagePathToSend.isNullOrEmpty()){
                    Toast.makeText(this@MainActivity, "select image first", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                mainRepository.sendImageToChannel(imagePathToSend!!)
                imagePathToSend = null
                sendingImageView.setImageResource(R.drawable.ic_add_photo)

            }
            sendTextButton.setOnClickListener {
                if(sendingTextEditText.text.isEmpty()){
                    Toast.makeText(this@MainActivity, "fill send text editText", Toast.LENGTH_SHORT)
                        .show()
                    return@setOnClickListener
                }

                mainRepository.sendTextToDataChannel(sendingTextEditText.text.toString())
                sendingTextEditText.setText("")
            }
            sendingImageView.setOnClickListener {
                openGallery()
            }
        }

    }

    override fun onConnectionRequestReceived(target: String) {
        runOnUiThread {
            views.apply {
                requestLayout.isVisible = false
                notificationLayout.isVisible = true
                notificationAcceptBtn.setOnClickListener {
                    mainRepository.startCall(target)
                    notificationLayout.isVisible = false
                }
                notificationDeclineBtn.setOnClickListener {
                    notificationLayout.isVisible = false
                    requestLayout.isVisible = true
                }
            }
        }
    }

    override fun onDataChannelReceived() {
        runOnUiThread {
            views.apply {
                requestLayout.isVisible = false
                receivedDataLayout.isVisible = true
                sendDataLayout.isVisible = true
            }
        }
    }

    override fun onDataReceivedFromChannel(it: DataChannel.Buffer) {
        runOnUiThread {
            val model = DataConverter.convertToModel(it)
            model?.let {
                when(it.first){
                    "TEXT"->{
                        views.receivedText.text = it.second
                            .toString()
                    }
                    "IMAGE"->{
                        Glide.with(this).load(it.second as Bitmap).into(
                            views.receivedImageView
                        )
                    }
                }
            }
        }
    }
}