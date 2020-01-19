package com.khsbs.saycle

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.khsbs.saycle.MainActivity.Companion.USER_EMERGENCY
import com.khsbs.saycle.MainActivity.Companion.USER_OK
import kotlinx.android.synthetic.main.activity_countdown.*

class CountdownActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_countdown)

        cdv_countdown.start(5000)

        cl_countdown.setOnClickListener {
            setResult(USER_OK)
            finish()
        }
        cdv_countdown.setOnCountdownEndListener {
            setResult(USER_EMERGENCY)
            finish()
        }
    }
}
