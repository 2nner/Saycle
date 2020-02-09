package com.khsbs.saycle.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {

    private val _userStatus: MutableLiveData<Boolean> = MutableLiveData() // True - 위험한 상태, False - 안전한 상태

    val userStatus: LiveData<Boolean> get() = _userStatus

    // Main //

    // Setting //

    // Countdown //
    fun userSafe() {
        _userStatus.value = false
    }

    fun userDanger() {
        _userStatus.value = true
    }
}