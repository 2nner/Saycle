package com.khsbs.saycle.ui

import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

abstract class BaseActivity<VB : ViewDataBinding, VM : ViewModel> : AppCompatActivity() {
    protected lateinit var binding: VB
    protected lateinit var viewModel: VM

    @LayoutRes
    protected abstract fun getLayoutId(): Int

    protected abstract fun getViewModel(): Class<VM>

    protected abstract fun getBindingVariable(): Int

    protected abstract fun initObserver()

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        performDataBinding()
        initObserver()
    }

    private fun performDataBinding() {
        binding = DataBindingUtil.setContentView(this, getLayoutId())
        this.viewModel=
            if (::viewModel.isInitialized) viewModel
            else ViewModelProvider(this).get(getViewModel())
        binding.setVariable(getBindingVariable(), viewModel)
        // 변수나 Observable이 변경되면 바인딩이 다음 프레임 전에 변경되도록 예약되지만,
        // executePendingBindings() 메소드 사용시 바인딩을 즉시 실행함
        binding.executePendingBindings()
        binding.lifecycleOwner = this
    }
}