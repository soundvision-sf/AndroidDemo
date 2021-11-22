package com.scalefocus.soundvision.ble;

import com.scalefocus.soundvision.ble.data.BLEScanAdvertising;
import com.scalefocus.soundvision.ble.data.ColorScanConfiguration;
import com.scalefocus.soundvision.ble.data.DeviceStats;

public interface IBLETransferClient {

    int SV_KEY_UP                        =  82;
    int SV_KEY_LEFT                      =  80;
    int SV_KEY_RIGHT                     =  79;
    int SV_KEY_DOWN                      =  81;
    int SV_KEY_ENTER                     =  40;

    void OnServiceConnect(BLETransferService service);
    void OnServiceDisconnect();
    void OnServiceError();
    void OnConnect();
    void OnDisconnect();
    void OnDiscovery();


    void OnData(final byte[] data);
    void OnStatusInfoChange(DeviceStats stats);
    void OnColorScanConfig(ColorScanConfiguration stats);
    void OnBLEAdvScan(BLEScanAdvertising stats);
}
