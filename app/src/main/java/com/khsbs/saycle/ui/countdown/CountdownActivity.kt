package com.khsbs.saycle.ui.countdown

import android.os.Bundle
import androidx.lifecycle.Observer
import com.khsbs.saycle.BR
import com.khsbs.saycle.R
import com.khsbs.saycle.databinding.ActivityCountdownBinding
import com.khsbs.saycle.ui.BaseActivity
import com.khsbs.saycle.ui.SharedViewModel
import com.khsbs.saycle.ui.main.MainActivity.Companion.USER_EMERGENCY
import com.khsbs.saycle.ui.main.MainActivity.Companion.USER_OK
import kotlinx.android.synthetic.main.activity_countdown.*

class CountdownActivity : BaseActivity<ActivityCountdownBinding, SharedViewModel>() {

    override fun getLayoutId(): Int {
        return R.layout.activity_countdown
    }

    override fun getViewModel(): Class<SharedViewModel> {
       return SharedViewModel::class.java
    }

    override fun getBindingVariable(): Int {
        return BR.viewmodel
    }

    override fun initObserver() {
        with(viewModel) {
            userStatus.observe(this@CountdownActivity, Observer {
                if (it) {
                    setResult(USER_EMERGENCY)
                }
                else {
                    setResult(USER_OK)
                }
                finish()
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cdv_countdown.start(10000)
    }
}
