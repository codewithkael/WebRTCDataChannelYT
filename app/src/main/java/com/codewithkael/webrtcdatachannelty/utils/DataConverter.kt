package com.codewithkael.webrtcdatachannelty.utils

import android.graphics.BitmapFactory
import android.util.Log
import com.codewithkael.webrtcdatachannelty.utils.FileMetaDataType.*
import org.webrtc.DataChannel
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.log

object DataConverter {
    private const val TAG = "DataConverter"

    private var isWaitingForData = false
    private var nextInputType: String? = null

    fun convertToBuffer(type: FileMetaDataType, body: String): DataChannel.Buffer {
        Log.d(TAG, "convertToBuffer: type = $type body = $body")
        return when (type) {
            TEXT -> {
                val fileDataBuffer = ByteBuffer.wrap(body.toByteArray())
                DataChannel.Buffer(fileDataBuffer, false)
            }
            IMAGE -> {
                val imageFile = File(body)
                val fileData = imageFile.readBytes()
                DataChannel.Buffer(ByteBuffer.wrap(fileData), false)
            }
            META_DATA_TEXT -> {
                val fileDataBuffer = ByteBuffer.wrap(META_DATA_TEXT.name.toByteArray())
                DataChannel.Buffer(fileDataBuffer, false)
            }
            META_DATA_IMAGE -> {
                val fileDataBuffer = ByteBuffer.wrap(META_DATA_IMAGE.name.toByteArray())
                DataChannel.Buffer(fileDataBuffer, false)
            }
        }

    }

    fun convertToModel(buffer: DataChannel.Buffer): Pair<String, Any>? {
        Log.d(TAG, "convertToModel: convert to model is called status = $isWaitingForData")
        val data = ByteArray(buffer.data.remaining())
        buffer.data.get(data)
        return if (!isWaitingForData) {
            nextInputType = when (String(data, Charsets.UTF_8)) {
                "META_DATA_TEXT" -> {
                    "TEXT"
                }
                "META_DATA_IMAGE" -> {
                    "IMAGE"
                }
                else -> {
                    null
                }
            }
            Log.d(TAG, "convertToModel: next incoming is data and the type is = $nextInputType")
            isWaitingForData = true
            null
        } else {

            when(nextInputType){
                "TEXT"->{
                    nextInputType = null
                    isWaitingForData = false
                    val textDataString = String(data,Charsets.UTF_8)
                    "TEXT" to textDataString
                }

                "IMAGE"->{
                    nextInputType = null
                    isWaitingForData = false
                   val bitmap = BitmapFactory.decodeByteArray(data,0,data.size)
                    "IMAGE" to bitmap
                }
                else->{
                    nextInputType = null
                    isWaitingForData = false
                    null
                }

            }
        }


    }
}