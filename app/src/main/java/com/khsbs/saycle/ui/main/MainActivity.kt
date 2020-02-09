package com.khsbs.saycle.ui.main

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.telephony.SmsManager
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.khsbs.saycle.BR
import com.khsbs.saycle.R
import com.khsbs.saycle.databinding.ActivityMainBinding
import com.khsbs.saycle.ui.BaseActivity
import com.khsbs.saycle.ui.SharedViewModel
import com.khsbs.saycle.ui.setting.SettingActivity
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.startActivity


// TODO : MVVM 패턴으로 코드 리팩토링해보기
class MainActivity : BaseActivity<ActivityMainBinding, SharedViewModel>() {
    companion object {
        const val GPS_ENABLE_REQUEST_CODE = 2001
        const val PERMISSIONS_REQUEST_CODE = 100
        const val COUNTDOWN_REQUEST = 1001

        const val REQUEST_ENABLE_BT = 3
        const val USER_OK = 4
        const val USER_EMERGENCY = 5
        const val TAG = "Saycle"

        const val OFF = "OFF"
        const val ON = "ON"
        const val DISCONNECTED = "DISCONNECTED"
    }

    private lateinit var mBluetoothAdapter: BluetoothAdapter
    private var mDevices: Set<BluetoothDevice> = emptySet()
    private var mPairedDeviceCount = 0
    lateinit var mDeviceName: String
    lateinit var mDeviceAddress: String
    private var mBluetoothLeService: MyBluetoothLeService? = null
    private val mViewModel: SharedViewModel by viewModels()
//    private var characteristicTX: BluetoothGattCharacteristic? = null
//    private var characteristicRX: BluetoothGattCharacteristic? = null

    // BluetoothLeService가 연결/연결 해제되었을 때
    private val mServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(p0: ComponentName?) {
            tv_main_remote.text =
                OFF
            mBluetoothLeService?.disconnect()
            mBluetoothLeService?.close()
            mBluetoothLeService = null
        }

        override fun onServiceConnected(componentName: ComponentName?, service: IBinder?) {
            mBluetoothLeService = (service as MyBluetoothLeService.LocalBinder).service
            if (!mBluetoothLeService!!.init()) {
                Log.e(TAG, "Unable to initialize Bluetooth")
            } else {
                Log.d(TAG, "Initialize Bluetooth")
                mBluetoothLeService!!.connect(mDeviceAddress)
            }
        }
    }

    private val mGattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                MyBluetoothLeService.ACTION_GATT_CONNECTED -> {
                    tv_main_remote.text =
                        ON
                }
                MyBluetoothLeService.ACTION_GATT_DISCONNECTED ->
                    tv_main_remote.text =
                        OFF
                /*
                MyBluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED ->
                    displayGattServices(mBluetoothLeService?.supportedGattServices)
                MyBluetoothLeService.ACTION_DATA_AVAILABLE ->
                    displayGattServices(mBluetoothLeService?.supportedGattServices)
                */
            }
        }
    }

    override fun getLayoutId(): Int {
        return R.layout.activity_main
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
        setContentView(R.layout.activity_main)

        Glide.with(baseContext)
            .asGif()
            .load(R.raw.loading)
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .into(iv_main_loading)

        tv_main_remote.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                when (p0.toString()) {
                    "ON" -> {
                        iv_main_loading.visibility = View.INVISIBLE
                        bg_main.background =
                            ContextCompat.getDrawable(
                                this@MainActivity,
                                R.drawable.bg_main_default
                            )

                    }
                    "OFF" -> {
                        iv_main_loading.visibility = View.INVISIBLE
                        bg_main.background =
                            ContextCompat.getDrawable(
                                this@MainActivity,
                                R.drawable.bg_main_default
                            )
                        switch_main.isChecked = false
                    }
                    "DISCONNECTED" -> {
                        iv_main_loading.visibility = View.VISIBLE
                        bg_main.background = ContextCompat.getDrawable(
                            this@MainActivity,
                            R.drawable.bg_main_disconnected
                        )
                    }
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })

        switch_main.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkBluetooth()
                tv_main_remote.text = DISCONNECTED
            } else {
                mBluetoothLeService?.disconnect()
                mBluetoothLeService?.close()
                tv_main_remote.text = OFF
            }
        }

        iv_main_user.setOnClickListener {
            startActivity<SettingActivity>()
        }

        bindService(
            Intent(this, MainActivity::class.java),
            mServiceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter())

        if (mBluetoothLeService != null) {
            if (mBluetoothLeService!!.connect(mDeviceAddress))
                tv_main_remote.text =
                    ON
            else
                tv_main_remote.text =
                    OFF
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(mGattUpdateReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(mServiceConnection)
        mBluetoothLeService = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_ENABLE_BT -> selectDevice()
            COUNTDOWN_REQUEST ->
                when (resultCode) {
                    USER_EMERGENCY -> {
                        sendMessage()
                    }
                }
        }
    }

    private fun checkBluetooth() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (mBluetoothAdapter.isEnabled.not()) {
            // 블루투스를 지원하지만 비활성 상태인 경우
            // 블루투스를 활성 상태로 바꾸기 위해 사용자 동의 요청
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(
                enableBtIntent,
                REQUEST_ENABLE_BT
            )
        } else {
            selectDevice()
        }
    }

    private fun selectDevice() {
        // 페어링되었던 기기 목록 획득
        mDevices = mBluetoothAdapter.bondedDevices
        // 페어링되어던 기기 갯수
        mPairedDeviceCount = mDevices.size
        // AlertDialog
        val builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle("디바이스 선택")
        builder.setCancelable(false)

        // 페어링 된 블루투스 장치의 이름 목록 작성
        val items_name = ArrayList<String>()
        val items_address = ArrayList<String>()
        for (device in mDevices) {
            items_name.add(device.name)
            items_address.add(device.address)
        }
        items_name.add("취소")
        val itemsWithCharSequence = items_name.toArray(arrayOfNulls<CharSequence>(items_name.size))

        builder.setItems(itemsWithCharSequence) { dialog, pos ->
            if (pos == items_name.size - 1) {
                dialog.dismiss()
                switch_main.isChecked = false
            } else {
                connectToSelectedDevice(items_name[pos] to items_address[pos])
            }
        }

        val alert = builder.create()
        alert.show()
    }

    @SuppressLint("HandlerLeak")
    private fun connectToSelectedDevice(deviceInfo: Pair<String, String>) {
        mDeviceName = deviceInfo.first
        mDeviceAddress = deviceInfo.second
        Log.d(TAG, "Connect to $mDeviceName $mDeviceAddress")
        val gattServiceIntent = Intent(baseContext, MyBluetoothLeService::class.java)
        bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun makeGattUpdateIntentFilter(): IntentFilter {
        val intentFilter = IntentFilter()
        intentFilter.addAction(MyBluetoothLeService.ACTION_GATT_CONNECTED)
        intentFilter.addAction(MyBluetoothLeService.ACTION_GATT_DISCONNECTED)
        intentFilter.addAction(MyBluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
        intentFilter.addAction(MyBluetoothLeService.ACTION_DATA_AVAILABLE)
        return intentFilter
    }

    /*private fun displayGattServices(gattServices: List<BluetoothGattService>?) {
        if (gattServices == null) return
        var uuid: String?
        val unknownServiceString = "unknown service"
        val gattServiceData = ArrayList<HashMap<String, String>>()

        // Loops through available GATT Services.
        for (gattService in gattServices) {
            val currentServiceData = HashMap<String, String>()
            uuid = gattService.uuid.toString()
            currentServiceData[LIST_NAME] = Attr.lookup(uuid, unknownServiceString)

            // If the service exists for HM 10 Serial, say so.
            //if(SampleGattAttributes.lookup(uuid, unknownServiceString) == "HM 10 Serial") { isSerial.setText("Yes, serial :-)"); } else {  isSerial.setText("No, serial :-("); }
            currentServiceData[LIST_UUID] = uuid
            gattServiceData.add(currentServiceData)

            // get characteristic when UUID matches RX/TX UUID
            characteristicTX = gattService.getCharacteristic(MyBluetoothLeService.UUID_HM_RX_TX)
            characteristicRX = gattService.getCharacteristic(MyBluetoothLeService.UUID_HM_RX_TX)
            if (Attr.lookup(uuid, unknownServiceString) === "HM 10 Serial") {
                mBluetoothLeService?.setCharacteristicNotification(characteristicRX, true)
            }
        }
    }*/

    private fun sendMessage() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(android.Manifest.permission.SEND_SMS),
                PERMISSIONS_REQUEST_CODE
            )
        }
        message(
            "10********",
            "서울 동작구 상도로 369 숭실대학교에서 사고가 발생하였습니다."
        )
    }

    // TODO : FusedLocationProvider로 기기 현재위치 가져오기 구현 후 적용
    /*private fun getCurrentAddress(latitude: Double, longitude: Double) {

    }*/

    private fun message(number: String, text: String) {
        val smsManager = SmsManager.getDefault()
        smsManager.sendTextMessage("+82$number", null, text, null, null)
    }
}