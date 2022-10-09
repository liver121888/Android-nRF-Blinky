package no.nordicsemi.android.blinky.profile.data;

import static no.nordicsemi.android.blinky.profile.BlinkyManager.NORDIC_UART_SERVICE;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public enum Command{

    KEEP_CONNECTION("keep_connection",123),
    REQUEST_BOND("request_bond",6),
    SYNC_TIME_AGE("sync_time_age",4),
    COMPLETE_BOND("complete_bond",17),
    VALIDATE_BOND("validate_bond",9),
    BREAK_BOND("break_bond",7),
    SYNC_FAST("sync_fast", 2),
    MEASURE_FATIGUE_CIRCULATION("measure_fatigue_circulation", 18),
    MEASURE_HRV_REALTIME("measure_hrv_realtime", 29),
    MEASURE_ECG_REALTIME("measure_ecg_realtime", 19);

    private final String label;
    private final int cmd;
    Command(String label, int cmd){
        this.label = label;
        this.cmd   = cmd;
    }
    public String getLabel(){
        return this.label;
    }
    public int getCmd(){
        return this.cmd;
    }
}


