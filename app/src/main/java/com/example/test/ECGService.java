package com.example.test;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.Arrays;

public class ECGService {
    // Debugging
    private static final String TAG = "ECGService";
    private static final boolean D = false;

    // Constants that indicate the current KY-Module state
    public static final int STATE_NONE = 0;        // doing nothing

    private static final int RawSize32 = 32;
    private static final int RawBufferSize = 32;        //為32的倍數
    private final Handler mHandler;

    private int iRawData;            //讀入RawData計數
    private int iRawBuffer;            //RawBuffer計數
    private int mState;

    private byte[] Info_Buffer;        //存放完整的資訊
    private final byte[] Raw_Buffer = new byte[RawBufferSize];        //存放32bytes Raw Data

    private StateService mStateService;

    public static final int STATE_TYPE = 1;

    public ECGService(Context context, Handler handler) {
        mHandler = handler;
        iRawData = RawSize32;        //Raw Data 計數初始值設為全滿
        iRawBuffer = 0;
        Info_Buffer = new byte[0];
        mStateService = new StateService(context, mHandler);
    }

    public void reset() {
        iRawData = RawSize32;
        iRawBuffer = 0;
        setState(STATE_NONE);
    }

    public synchronized void setState(int state) {
        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(FamilyFragment.MESSAGE_KY_STATE, state, -1).sendToTarget();
    }

    /**
     * Return the current KY state.
     */
    public synchronized int getState() {
        return mState;
    }


    public void DataHandler(byte[] Data) {
//		if (D) Log.d(TAG, "DataHandler");

        int iDataEnd = -1;            //存放資訊結尾('\n')的指標在data中的位置

        for (int i = 0; i < Data.length; i++) {
            //若資訊為Raw=32,後面接到的32bytes 全為RawData
            if (iRawData < RawSize32) {
                Log.d(TAG, "DataHandler 在這裡: " + iRawBuffer);
                Raw_Buffer[iRawBuffer] = Data[i];
                iRawData++;
                iRawBuffer++;
                iDataEnd = i;

                if (iRawBuffer == RawBufferSize) {

                    // Notice (A)
                    byte[] rawData = new byte[RawBufferSize];
                    System.arraycopy(Raw_Buffer, 0, rawData, 0, RawBufferSize);
//                    int[] value = new int[rawData.length];
//                    for (int k = 0; k < rawData.length; k++) {
//                        value[k] = rawData[k] & 0xFF;
//                    }
//
//                    byte[] byteArray = new byte[rawData.length];
//                    for (int k = 0; k < rawData.length; k++) {
//                        byteArray[k] = (byte) (value[k]);
//                    }

//                    mStateService.runModel(rawData);
                    // Send the obtained bytes to the UI Activity
                    // arg1-> length, arg2-> -1, obj->buffer
                    mHandler.obtainMessage(FamilyFragment.MESSAGE_RAW, RawBufferSize, -1, rawData)
                            .sendToTarget();
                    iRawBuffer = 0;
                }

            }
            //若字元為資訊結尾字元(0x0D)('\n')
            else if (Data[i] == 0x0D) {
                //若Info_Buffer 為空
                if (Info_Buffer.length == 0) {
                    Info_Buffer = new byte[i - iDataEnd];
                    System.arraycopy(Data, iDataEnd + 1, Info_Buffer, 0, i - iDataEnd);
                } else {
                    byte[] Temp = new byte[i - iDataEnd];
                    System.arraycopy(Data, iDataEnd + 1, Temp, 0, Temp.length);
                    Info_Buffer = Combine(Info_Buffer, Temp);
                }

                iDataEnd = i;//指向資訊結尾的指標指在('\n')的位置

                //將完整的資訊傳給InfoHandler處理
                Log.d(TAG, "DataHandler Info_Buffer: " + Arrays.toString(Info_Buffer));
                InfoHandler(Info_Buffer);

            }//End of [if(Data[i] == 0x0D)]
            else if (i == Data.length - 1)//將作0X0D判斷後不完整的資訊存入Info_Buffer
            {
                byte[] Temp = new byte[i - iDataEnd];
                System.arraycopy(Data, iDataEnd + 1, Temp, 0, Temp.length);
                Info_Buffer = Combine(Info_Buffer, Temp);
            }

        }//End of [for(int i=0; i <= Data.length;i++)]

    }//End of [public void DataHandler (byte[] Data)]

    public void InfoHandler(byte[] Info) {
        String InfoStr = new String(Info, 0, Info.length - 1);    //去掉結尾0x0D
        Log.d(TAG, "DataHandler InfoStr: " + InfoStr);
        if (InfoStr.equals("RAW=32")) {
            iRawData = 0;
        } else {
            // Send a Info message back to the Activity
            Message msg = mHandler.obtainMessage(FamilyFragment.MESSAGE_INFO);
            Bundle bundle = new Bundle();
            bundle.putString(FamilyFragment.KY_INFO, InfoStr);
            msg.setData(bundle);
            mHandler.sendMessage(msg);
        }
        Info_Buffer = new byte[0];
    }

    private byte[] Combine(byte[] A1, byte[] A2) {
        byte[] R = new byte[A1.length + A2.length];
        System.arraycopy(A1, 0, R, 0, A1.length);
        System.arraycopy(A2, 0, R, A1.length, A2.length);

        return R;
    }

}