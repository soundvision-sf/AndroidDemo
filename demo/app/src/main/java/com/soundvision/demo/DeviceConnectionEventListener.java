package com.soundvision.demo;

import android.bluetooth.BluetoothDevice;

public interface DeviceConnectionEventListener {
    void OnDeviceConnect(BluetoothDevice device);
    void OnDeviceDisconnect();
}
