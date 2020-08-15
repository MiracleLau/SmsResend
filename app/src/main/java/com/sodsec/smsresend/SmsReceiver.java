package com.sodsec.smsresend;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SmsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("test","收到短信");
        Bundle bundle = intent.getExtras();
        Object[] pdus = (Object[]) bundle.get("pdus");
        SmsMessage[] msgs = new SmsMessage[pdus.length];
        String format1 = intent.getStringExtra("format");
        for (int i = 0; i < pdus.length; i++) {
            msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i],format1);
        }
        List<Map<String,String>> json = new ArrayList<Map<String,String>>();
        for (SmsMessage msg : msgs) {
            Map<String,String> msgMap = new HashMap<String,String>();
            msgMap.put("mobile",msg.getDisplayOriginatingAddress());
            msgMap.put("content",msg.getDisplayMessageBody());
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = new Date(msg.getTimestampMillis());
            String time = format.format(date);
            msgMap.put("type","短信");
            msgMap.put("time",time);
            json.add(msgMap);
        }
        Gson gson = new Gson();
        String jsonString = gson.toJson(json);
        Toast.makeText(context, jsonString, Toast.LENGTH_LONG).show();
    }
}
