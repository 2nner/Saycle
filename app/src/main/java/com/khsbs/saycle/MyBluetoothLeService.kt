package com.khsbs.saycle

import android.app.Service
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import java.util.*

class MyBluetoothLeService : Service() {

    companion object {
        private val TAG = MyBluetoothLeService::class.java.simpleName

        private const val STATE_DISCONNECTED = 0
        private const val STATE_CONNECTING = 1
        private const val STATE_CONNECTED = 2

        private var mBluetoothManager: BluetoothManager? = null
        private var mBluetoothAdapter: BluetoothAdapter? = null
        private var mBluetoothDeviceAddress: String? = null
        private var mBluetoothGatt: BluetoothGatt? = null
        private var mConnectionState = STATE_DISCONNECTED

        const val ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED"
        const val ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED"
        const val ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED"
        const val ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE"
        const val EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA"

        val UUID_HM_RX_TX: UUID = UUID.fromString(Attr.HM_RX_TX)
    }

    // GATT 이벤트에서 발행되는 Callback 구현
    private val mGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        // 연결 상태가 변경됨
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val intentAction: String
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    intentAction = ACTION_GATT_CONNECTED
                    mConnectionState = STATE_CONNECTED
                    broadcastUpdate(intentAction)
                    Log.i(TAG, "Connected to GATT server.")
                    Log.i(
                        TAG, "Attempting to start service discovery:" +
                                mBluetoothGatt?.discoverServices()
                    )
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    intentAction = ACTION_GATT_DISCONNECTED
                    mConnectionState = STATE_DISCONNECTED
                    Log.i(TAG, "Disconnected from GATT server.")
                    broadcastUpdate(intentAction)
                }
            }
        }

        // 새로운 Service가 발견됨
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            when (status) {
                BluetoothGatt.GATT_SUCCESS ->
                    broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
                else ->
                    Log.w(TAG, "onServicesDiscovered received: $status")
            }
        }

        // Characteristic 읽기 연산의 결과
        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            when (status) {
                BluetoothGatt.GATT_SUCCESS ->
                    broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
            }
        }

        // Characteristic에 대한 알림이 활성화된 후에, 디바이스에서 Characteristic의 변경을 감지했을 때
        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
        }
    }

    private fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        sendBroadcast(intent)
    }

    private fun broadcastUpdate(action: String, characteristic: BluetoothGattCharacteristic) {
        val intent = Intent(action)

        // Characteristic의 uuid로 어떤 행동을 취할 것인지를 구분
        when (characteristic.uuid) {
            UUID_HM_RX_TX -> { } // 예시
            else -> {
                // Characteristic에 담긴 값 검사
                if (characteristic.value.toString(Charsets.UTF_8) == "EMERGENCY") {
                    Intent(this, CountdownActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(this)
                    }
                }
                else {
                    // 다른 모든 profile은 16진수로 변환 후 write
                    val data: ByteArray? = characteristic.value
                    Log.i(TAG, "Received data : " + characteristic.value)

                    if (data?.isNotEmpty() == true) {
                        val hexString = data.joinToString(separator = " ") {
                            String.format("%02X", it) // 10진수를 16진수로 변환하여 저장
                        }
                        intent.putExtra(EXTRA_DATA, "$data\n$hexString")
                    }
                }
            }
        }
        sendBroadcast(intent)
    }

    inner class LocalBinder : Binder() {
        val service: MyBluetoothLeService
            get() = this@MyBluetoothLeService
    }

    private val mBinder: IBinder = LocalBinder()

    override fun onBind(p0: Intent?): IBinder? = mBinder

    override fun onUnbind(intent: Intent?): Boolean {
        close()
        return super.onUnbind(intent)
    }

    /**
     * BluetoothAdapter를 초기화하는 함수.
     *
     * @return 초기화가 정상적으로 이루어졌다면 true를, BluetoothManager나 BluetoothAdapter가 하나라도 null값이라면
     * false를 반환함.
     */
    fun init(): Boolean {
        if (mBluetoothManager == null) {
            mBluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            if (mBluetoothManager == null) {
                return false
            }
        }

        mBluetoothAdapter = mBluetoothManager!!.adapter
        if (mBluetoothAdapter == null) {
            return false
        }

        return true
    }

    /**
     * GATT server 역할을 하는 BLE 디바이스에 연결을 시도하는 함수.
     *
     * @param address 연결할 BLE 디바이스의 주소값
     */
    fun connect(address: String?): Boolean {
        if (mBluetoothAdapter == null || address == null) {
            return false
        }

        // 이전에 연결했던 디바이스가 있다면 재연결 시도
        if (mBluetoothDeviceAddress != null && address == mBluetoothDeviceAddress
            && mBluetoothGatt != null) {
            return if (mBluetoothGatt!!.connect()) {
                mConnectionState = STATE_CONNECTING
                true
            } else {
                false
            }
        }

        // 디바이스 연결이 처음일 때 - getRemoteDevice(address)가 null 일때 false 반환
        mBluetoothAdapter!!.getRemoteDevice(address).let {
            mBluetoothGatt = it.connectGatt(this, false, mGattCallback)
            mConnectionState = STATE_CONNECTING
            return true
        }
    }


    /**
     * 현재 연결된 connection을 종료하거나 연결 중인 connection을 취소하는 함수.
     */
    fun disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return
        }
        mBluetoothGatt!!.disconnect()
    }

    /**
     * 주어진 BLE 디바이스 사용 후, 리소스가 제대로 release 되었는지 보장하기 위해 호출해야 하는 함수
     */
    fun close() {
        if (mBluetoothGatt == null) {
            return
        }
        mBluetoothGatt!!.close()
        mBluetoothGatt = null
    }
}