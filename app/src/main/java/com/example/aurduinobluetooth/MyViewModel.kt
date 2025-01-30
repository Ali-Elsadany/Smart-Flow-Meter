package com.example.aurduinobluetooth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MyViewModel :ViewModel() {
    val readMessageLiveData= MutableLiveData<String>()

    fun emitMessage(message:String){
        readMessageLiveData.value = message
    }
}