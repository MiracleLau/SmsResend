package com.sodsec.smsresend;

import android.telephony.SmsManager;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

import okhttp3.MediaType;

public class Tools {
    public static final MediaType JSON=MediaType.parse("application/json; charset=utf-8");
    // 发送短信
    public static void SendMsg(String phone, String text, TextView textView, ScrollView scrollView) {
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(phone, null, text,null,null);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date(System.currentTimeMillis());
        String now = simpleDateFormat.format(date);
        textView.append("["+now+"] 转发到手机号成功\n");
        scrollView.fullScroll(ScrollView.FOCUS_DOWN);
    }
}

