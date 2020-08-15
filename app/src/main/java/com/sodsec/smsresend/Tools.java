package com.sodsec.smsresend;

import android.content.Context;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Tools {
    public static final MediaType JSON=MediaType.parse("application/json; charset=utf-8");
    // 发送短信
    public static void SendMsg(String phone, String text, TextView textView, ScrollView scrollView) {
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(phone, null, text,null,null);
        textView.append("转发到手机号成功\n");
        scrollView.fullScroll(ScrollView.FOCUS_DOWN);
    }

    public static String post(String url, List<Map<String,String>> data) {
        try {
            OkHttpClient okHttpClient = new OkHttpClient();
            Gson gson = new Gson();
            String json = gson.toJson(data);
            RequestBody requestBody = RequestBody.create(json, JSON);
            Request request = new Request.Builder()
                    .url(url + "/saveMsg")
                    .post(requestBody)
                    .build();
            Response response = okHttpClient.newCall(request).execute();
            // 判断请求是否成功
            if (response.isSuccessful()) {
                Result result = gson.fromJson(response.body().toString(), Result.class);
                if(result.code == 0) {
                    return "";
                } else {
                    return result.msg;
                }
            } else {
                return "请求未成功";
            }
        } catch (Exception e) {
            return e.getMessage();
        }
    }
}

