package com.example.test;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ECGService {
    // Debugging
    private static final String TAG = "ECGService";
    private static final boolean D = false;

    // Constants that indicate the current KY-Module state
    public static final int STATE_NONE = 0;        // doing nothing

    private static final int RawSize32 = 32;
    private static final int RawBufferSize = 1250;        //每秒有250個點
    private static final int DrawBufferSize = 16;        //畫圖
    private static final int UploadBufferSize = 1250;        //每5秒上傳
    private int uploadcounter = 0;//10000000000000000000000000000000000000000000000000000000000
    private final Handler mHandler;

    private int iRawData;           //讀入RawData計數
    private int iRawBuffer;         //RawBuffer計數
    private int iDrawBuffer;        //DrawBuffer計數
    private int iUploadBuffer1;        //DrawBuffer計數
    private int iUploadBuffer2;        //DrawBuffer計數
    boolean buff1=true;
    boolean buff2=false;
    private int mState;

    private byte[] Info_Buffer;        //存放完整的資訊
    private final byte[] Raw_Buffer = new byte[RawBufferSize];        //存放250bytes Raw Data
    private final byte[] Draw_Buffer = new byte[DrawBufferSize];        //存放32bytes Draw Data
    //private final byte[] Upload_Buffer = new byte[UploadBufferSize];        //存放1250bytes Draw Data
    private List<Float> Upload_Buffer1 = new ArrayList<>();  //會有1跟2是要讓他們輪流使用，留5秒上傳到資料庫
    private List<Float> Upload_Buffer2 = new ArrayList<>();

    private final StateService mStateService;

    public static final int STATE_TYPE = 1;

    public ECGService(Context context, Handler handler) {
        mHandler = handler;
        iRawData = RawSize32;        //Raw Data 計數初始值設為全滿
        iRawBuffer = 0;
        iUploadBuffer1 = 0;
        iUploadBuffer2 = 1249;
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
        mHandler.obtainMessage(StateFragment.MESSAGE_KY_STATE, state, -1).sendToTarget();
    }

    /**
     * Return the current KY state.
     */
    public synchronized int getState() {
        return mState;
    }


    public void DataHandler(byte[] Data,String email) {
//		if (D) Log.d(TAG, "DataHandler");

        int iDataEnd = -1;            //存放資訊結尾('\n')的指標在data中的位置

        for (int i = 0; i < Data.length; i++) {
            //若資訊為Raw=32,後面接到的32bytes 全為RawData
            if (iRawData < RawSize32) {
                if (buff1){
                    Upload_Buffer1.add((float)Data[i]);
                    iUploadBuffer1++;
                    if (iUploadBuffer1==UploadBufferSize){
                        iUploadBuffer2=0;
                        Upload_Buffer2.clear();
                    }
                }
                else if (buff2){
                    Upload_Buffer2.add((float)Data[i]);
                    iUploadBuffer2++;
                    if (iUploadBuffer2==UploadBufferSize){
                        iUploadBuffer1=0;
                        Upload_Buffer1.clear();
                    }
                }
                Raw_Buffer[iRawBuffer] = Data[i];
                Draw_Buffer[iDrawBuffer] = Data[i];
                iRawData++;
                iRawBuffer++;
                iDrawBuffer++;
                iDataEnd = i;
                if (iDrawBuffer == DrawBufferSize) {
                    byte[] rawData = new byte[DrawBufferSize];
                    System.arraycopy(Draw_Buffer, 0, rawData, 0, DrawBufferSize);
                    mHandler.obtainMessage(StateFragment.MESSAGE_RAW, DrawBufferSize, -1, rawData)
                            .sendToTarget();
                    iDrawBuffer = 0;
                }
                if (((iUploadBuffer1 == UploadBufferSize&&buff1) ||(iUploadBuffer2 == UploadBufferSize&&buff2))){
                    if (iUploadBuffer1 == UploadBufferSize&&buff1) {
                        buff1=false;
                        buff2=true;
                        int[] statearr = new int[5];
                        for (int count = 0,statecounter=0;count < 1250;count+=250) {
                            byte[] rawData = new byte[RawBufferSize];
                            System.arraycopy(Raw_Buffer, count, rawData, 0, 250);
                            byte[] interpolatedData = linearInterpolation(rawData, 360);
                            statearr[statecounter++] = mStateService.runModel(interpolatedData);
                        }
                        ArrayList<Object> dataList = new ArrayList<>();
                        dataList.add(Upload_Buffer1);
                        dataList.add(statearr);
                        iRawBuffer = 0;
                        mHandler.obtainMessage(StateFragment.MESSAGE_UPLOAD, UploadBufferSize, -1, dataList).sendToTarget();
                    }else if (iUploadBuffer2 == UploadBufferSize&&buff2){
                        buff1=true;
                        buff2=false;
                        int[] statearr = new int[5];
                        for (int count = 0;count < 1250;count+=250) {
                            byte[] rawData = new byte[RawBufferSize];
                            System.arraycopy(Raw_Buffer, count, rawData, 0, 250);
                            byte[] interpolatedData = linearInterpolation(rawData, 360);
                            statearr[((count+250)/250)-1] = mStateService.runModel(interpolatedData);
                        }
                        ArrayList<Object> dataList = new ArrayList<>();
                        dataList.add(Upload_Buffer2);
                        dataList.add(statearr);
                        iRawBuffer = 0;
                        mHandler.obtainMessage(StateFragment.MESSAGE_UPLOAD, UploadBufferSize, -1, dataList).sendToTarget();
                    }
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
            Message msg = mHandler.obtainMessage(StateFragment.MESSAGE_INFO);
            Bundle bundle = new Bundle();
            bundle.putString(StateFragment.KY_INFO, InfoStr);
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

    public static byte[] linearInterpolation(byte[] data, int newLength) {
        int[] intData = new int[data.length];
        for (int i = 0; i < data.length; i++) {
            intData[i] = data[i] & 0xFF; // Convert bytes to positive int values
        }

        int[] interpolatedIntData = new int[newLength];
        float step = (float) (intData.length - 1) / (newLength - 1);

        for (int i = 0; i < newLength; i++) {
            int index = (int) (i * step);
            float fraction = i * step - index;

            if (index == intData.length - 1) {
                interpolatedIntData[i] = intData[index];
            } else {
                int interpolatedValue = Math.round((1 - fraction) * intData[index] + fraction * intData[index + 1]);
                interpolatedIntData[i] = interpolatedValue;
            }
        }

        byte[] interpolatedByteData = new byte[newLength];
        for (int i = 0; i < newLength; i++) {
            interpolatedByteData[i] = (byte) interpolatedIntData[i];
        }

        return interpolatedByteData;
    }

}