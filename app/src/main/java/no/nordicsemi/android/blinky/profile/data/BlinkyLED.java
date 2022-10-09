/*
 * Copyright (c) 2018, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package no.nordicsemi.android.blinky.profile.data;

import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import no.nordicsemi.android.ble.data.Data;


@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public final class BlinkyLED{
    private static final String TAG = "BlinkyLED";


    private static byte[] H2PlusSettings = new byte[] {(byte)0x7f, (byte)0x69, (byte)0x03};
    private static byte[] ACCOUNT_UUID = "012345678901".getBytes(StandardCharsets.UTF_8);


    //
    //    byte[] uuidbyte = ACCOUNT_UUID.getBytes(StandardCharsets.UTF_8);
    //
    //    String s =  new String(uuidbyte, StandardCharsets.UTF_8);


    @RequiresApi(api = Build.VERSION_CODES.O)
    @NonNull
    public static Data write(Command command) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Calendar c = Calendar.getInstance();
        switch (command.getCmd()){
            case(123):
                out.write('1');
                out.write('2');
                out.write('3');
                break;
            case(6):
                out.write(6);
                try {
                    out.write(new byte[]{0x10});
                } catch (IOException e) {
                    e.printStackTrace();
                }
                out.write(0);
                out.write(0);
                try {
                    out.write(ACCOUNT_UUID);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                out.write(0);
                try {
                    out.write(H2PlusSettings);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case(4):
                out.write(4);
                try {
                    out.write(new byte[]{0x10});
                } catch (IOException e) {
                    e.printStackTrace();
                }
                out.write(0);
                out.write(0);
                out.write(c.get(Calendar.YEAR)-2000);
                out.write(c.get(Calendar.MONTH)+1);
                out.write(c.get(Calendar.DAY_OF_MONTH));
                out.write(c.get(Calendar.HOUR_OF_DAY));
                out.write(c.get(Calendar.MINUTE));
                out.write(c.get(Calendar.SECOND));
                //                out.write(LocalDateTime.now().getYear()-2000);
                //                out.write(LocalDateTime.now().getMonthValue());
                //                out.write(LocalDateTime.now().getDayOfMonth());
                //                out.write(LocalDateTime.now().getHour());
                //                out.write(LocalDateTime.now().getMinute());
                //                out.write(LocalDateTime.now().getSecond());
                //TODO: get age from the server
                out.write(22);
                // steps/100
                out.write(80);
                out.write(0);
                // height, weired
                out.write(110);
                // weight
                out.write(65);
                // gender, 1 = M, 2 = F
                out.write(1);
                // locale, Eng 0, Zh-tw 1, Zh-cn 2, Jp 3
                out.write(1);
                // Circulation/Energy alert value
                out.write(50);
                // AvgHeartRate
                out.write(110);
                break;
            case(17):
                out.write(17);
                out.write(3);
                out.write(0);
                out.write(0);
                // steps initialization
                out.write(0);
                out.write(0);
                out.write(0);
                // Systolic Blood Pressure, SBP weights
                out.write(0);
                out.write(0);
                // Diastolic Blood Pressure, DBP weights
                out.write(0);
                out.write(0);
                break;
            case(9):
                out.write(9);
                out.write(16);
                out.write(0);
                out.write(0);
                try {
                    out.write(ACCOUNT_UUID);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                out.write(0);
                try {
                    out.write(H2PlusSettings);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case(7):
                out.write(7);
                out.write(12);
                out.write(0);
                out.write(0);
                try {
                    out.write(ACCOUNT_UUID);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case(2):
                out.write(2);
                out.write(12);
                try {
                    out.write(ACCOUNT_UUID);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                out.write(c.get(Calendar.YEAR)-2000);
                out.write(c.get(Calendar.MONTH)+1);
                out.write(c.get(Calendar.DAY_OF_MONTH));
                out.write(c.get(Calendar.HOUR_OF_DAY));
                out.write(c.get(Calendar.MINUTE));
                out.write(c.get(Calendar.SECOND));
                break;
            case(19):
                out.write(19);
                out.write(3);
                out.write(0);
                out.write(0);
                // type, 0 = start, 1 = end
                out.write(0);
                // timeout, sec
                out.write(10);
                break;
            case(29):
                out.write(29);
                out.write(3);
                out.write(0);
                out.write(0);
                // type, 0 = start, 1 = end
                out.write(0);
                // timeout, sec
                out.write(10);
                break;
            case(18):
                out.write(18);
                out.write(0);
                out.write(0);
                out.write(0);
                break;
            default:
                break;
        }
        Data data = new Data(out.toByteArray());
        byte[] ba = data.getValue();
        int[] x = new int[ba.length];
        for(int i = 0; i<ba.length;i++){
            if (ba[i]<0)
                x[i] = 256+ba[i];
            else
                x[i] = ba[i];
        }
        Log.d("Write out:", Arrays.toString(x));
        return data;
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    public static Object read(int[] x) {

        int l = x.length;
        int cmd = x[0];
        switch (cmd){
            case(119):
                int state = x[1];
                switch (state){
                    case (24):
                        Log.d(TAG, "read: start drawing");
                        break;
                    case (25):
                        Log.i(TAG, "read: data");
                        // data size = l
                        int datasize = x[2];
                        // TODO: output heartrate
                        int heartrate = x[4];
                        // discard first five byte of data
                        byte[] ba = new byte[datasize-5];
                        for(int i = 5; i<ba.length;i++){
                                ba[i-5] = (byte)x[i];
                        }
                        ByteBuffer bb = ByteBuffer.wrap(ba);
                        bb.order( ByteOrder.LITTLE_ENDIAN);
                        List<Short> shortList = new ArrayList<>();
                        while( bb.hasRemaining()) {
                            shortList.add(bb.getShort());
                        }
                        Log.d(TAG, shortList.toString());
                        return shortList;
                    case (26):
                        Log.d(TAG, "read: end drawing");

                        break;
                    default:
                        break;
                }
                break;
            case(106):
                break;
            default:
                return null;
        }
        return null;
    }
}
