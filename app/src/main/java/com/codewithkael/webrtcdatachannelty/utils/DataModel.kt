package com.codewithkael.webrtcdatachannelty.utils

enum class DataModelType{
    SignIn, StartConnection, Offer, Answer, IceCandidates
}


data class DataModel(
    val type: DataModelType?=null,
    val username:String,
    val target:String?=null,
    val data:Any?=null
)
