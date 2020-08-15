package com.sodsec.smsresend;

import android.telephony.SmsManager;
import android.widget.ScrollView;
import android.widget.TextView;

public class Tools {
    public static void SendMsg(String phone, String text, TextView textView, ScrollView scrollView) {
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(phone, null, text,null,null);
        textView.append("转发到手机号成功\n");
        scrollView.fullScroll(ScrollView.FOCUS_DOWN);
    }
}
