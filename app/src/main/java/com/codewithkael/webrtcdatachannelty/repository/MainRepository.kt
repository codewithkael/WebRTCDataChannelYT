package com.codewithkael.webrtcdatachannelty.repository

import com.codewithkael.webrtcdatachannelty.socket.SocketClient
import com.codewithkael.webrtcdatachannelty.utils.DataConverter
import com.codewithkael.webrtcdatachannelty.utils.DataModel
import com.codewithkael.webrtcdatachannelty.utils.DataModelType.*
import com.codewithkael.webrtcdatachannelty.utils.FileMetaDataType
import com.codewithkael.webrtcdatachannelty.webrtc.MyPeerObserver
import com.codewithkael.webrtcdatachannelty.webrtc.WebrtcClient
import com.google.gson.Gson
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import java.nio.ByteBuffer
import javax.inject.Inject

class MainRepository @Inject constructor(
    private val socketClient: SocketClient,
    private val webrtcClient: WebrtcClient,
    private val gson: Gson
) : SocketClient.Listener, WebrtcClient.Listener, WebrtcClient.ReceiverListener {
    private lateinit var username: String
    private lateinit var target: String
    private var dataChannel: DataChannel? = null

    var listener: Listener? = null

    fun init(username: String) {
        this.username = username
        initSocket()
        initWebrtcClient()

    }

    private fun initSocket() {
        socketClient.listener = this
        socketClient.init(username)

    }

    private fun initWebrtcClient() {
        webrtcClient.listener = this
        webrtcClient.receiverListener = this
        webrtcClient.initializeWebrtcClient(username, object : MyPeerObserver() {
            override fun onIceCandidate(p0: IceCandidate?) {
                super.onIceCandidate(p0)
                p0?.let {
                    webrtcClient.sendIceCandidate(it, target)
                }
            }

            override fun onDataChannel(p0: DataChannel?) {
                super.onDataChannel(p0)
                dataChannel = p0
                listener?.onDataChannelReceived()

            }

        })
    }

    fun sendStartConnection(target: String) {
        this.target = target
        socketClient.sendMessageToSocket(
            DataModel(
                type = StartConnection,
                username = username,
                target = target,
                data = null
            )
        )
    }

    fun startCall(target: String) {
        webrtcClient.call(target)
    }

    fun sendTextToDataChannel(text:String){
        sendBufferToDataChannel(DataConverter.convertToBuffer(FileMetaDataType.META_DATA_TEXT,text))
        sendBufferToDataChannel(DataConverter.convertToBuffer(FileMetaDataType.TEXT,text))
    }

    fun sendImageToChannel(path:String){
        sendBufferToDataChannel(DataConverter.convertToBuffer(FileMetaDataType.META_DATA_IMAGE,path))
        sendBufferToDataChannel(DataConverter.convertToBuffer(FileMetaDataType.IMAGE,path))
    }

    private fun sendBufferToDataChannel(buffer: DataChannel.Buffer){
        dataChannel?.send(buffer)

    }

    override fun onNewMessageReceived(model: DataModel) {
        when (model.type) {
            StartConnection -> {
                this.target = model.username
                listener?.onConnectionRequestReceived(model.username)
            }
            Offer -> {
                webrtcClient.onRemoteSessionReceived(
                    SessionDescription(
                        SessionDescription.Type.OFFER, model.data.toString()
                    )
                )
                this.target = model.username
                webrtcClient.answer(target)
            }
            Answer -> {
                webrtcClient.onRemoteSessionReceived(
                    SessionDescription(
                        SessionDescription.Type.ANSWER, model.data.toString()
                    )
                )
            }
            IceCandidates -> {
                val candidate = try {
                    gson.fromJson(model.data.toString(), IceCandidate::class.java)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
                candidate?.let {
                    webrtcClient.addIceCandidate(it)
                }
            }
            else -> Unit
        }

    }

    override fun onTransferEventToSocket(data: DataModel) {
        socketClient.sendMessageToSocket(data)
    }

    override fun onDataReceived(it: DataChannel.Buffer) {
        listener?.onDataReceivedFromChannel(it)
    }

    interface Listener {
        fun onConnectionRequestReceived(target: String)
        fun onDataChannelReceived()
        fun onDataReceivedFromChannel(it: DataChannel.Buffer)
    }
}