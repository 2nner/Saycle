package com.khsbs.saycle.ui.setting

import android.os.Bundle
import com.khsbs.saycle.BR
import com.khsbs.saycle.R
import com.khsbs.saycle.databinding.ActivitySettingBinding
import com.khsbs.saycle.ui.BaseActivity
import com.khsbs.saycle.ui.SharedViewModel

class SettingActivity : BaseActivity<ActivitySettingBinding, SharedViewModel>() {

    override fun getLayoutId(): Int {
        return R.layout.activity_setting
    }

    override fun getViewModel(): Class<SharedViewModel> {
        return SharedViewModel::class.java
    }

    override fun getBindingVariable(): Int {
        return BR.viewmodel
    }

    override fun initObserver() {

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
}
