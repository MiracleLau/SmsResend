package com.sodsec.smsresend;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SmsMessage;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private EditText phoneEdit;
    private EditText apiEdit;
    private EditText pwdEdit;
    private TextView statusLabel;
    private ScrollView scrollView;
    private String phone;
    private String api;
    private String pwd;
    public static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
    SmsReceiver broadcastReceiver;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 初始化程序
        init();
    }

    private void init(){
        phoneEdit = (EditText)findViewById(R.id.phoneNum);
        apiEdit = (EditText)findViewById(R.id.apiUrl);
        statusLabel = (TextView)findViewById(R.id.serviceStatus);
        scrollView = (ScrollView)findViewById(R.id.scrollText);
        pwdEdit = (EditText)findViewById(R.id.apiPwd);
        try{
            SharedPreferences config = Config.getProperties(getApplicationContext());
            phone = config.getString("phone","");
            api = config.getString("api","");
            pwd = config.getString("pwd","");
            phoneEdit.setText(phone);
            apiEdit.setText(api);
            pwdEdit.setText(pwd);
            IntentFilter filter = new IntentFilter();
            filter.addAction(SMS_RECEIVED);
            broadcastReceiver = new SmsReceiver(MainActivity.this);
            registerReceiver(broadcastReceiver, filter);

            // 启动服务
//            Intent intent = new Intent(this, ServiceUpdateUI.class);
//            startService(intent);
        }catch (Exception ex){
            showToast(ex.getMessage());
        }
    }
    // 保存配置按钮点击事件
    public void saveConfigClick(View v) {
        phone = phoneEdit.getText().toString();
        api = apiEdit.getText().toString();
        pwd = pwdEdit.getText().toString();
        if(api.isEmpty() || phone.isEmpty() || pwd.isEmpty()){
            showToast("请确保每一项都不为空");
            return;
        } else {
            Map<String, String> conf = new HashMap<String,String>();
            conf.put("phone",phone);
            conf.put("api",api);
            conf.put("pwd",pwd);
            try{
                if(Config.setPropertiesMap(getApplicationContext(),conf)) {
                    showToast("配置保存成功");
                } else {
                    showToast("配置保存失败");
                }
            } catch (Exception ex){
                showToast("配置保存失败！"+ex.getMessage());
            }

        }
    }

    public void testApiClick(View v) {
//        Tools.SendMsg("15254095511","test",statusLabel,scrollView);
        // 测试数据
        Map<String,String> testMap = new HashMap<String,String>();
        testMap.put("test","测试内容");
        testMap.put("pwd",pwd);
        post(api,testMap);
    }

//    public void startListenClick(View v) {
//    }

    public void showToast(String text) {
        if (text == null) {
            text = "NULL";
        }
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date(System.currentTimeMillis());
        String now = simpleDateFormat.format(date);
        text = "["+now+"] " + text;
        Toast.makeText(getApplicationContext(),text,Toast.LENGTH_SHORT).show();
        statusLabel.append(text+"\n");
        scrollView.fullScroll(ScrollView.FOCUS_DOWN);
    }

    // 退出确认事件
    public void onBackPressed() {
        new AlertDialog.Builder(this).setTitle("确认退出吗？")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 点击“确认”后的操作
                        MainActivity.this.finish();

                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 点击“返回”后的操作,这里不设置没有任何操作
                    }
                }).show();
    }

    // 发送POST请求
    public void post(String url, Map<String,String> data) {
        MediaType JSON=MediaType.parse("application/json; charset=utf-8");
        try {
            OkHttpClient okHttpClient = new OkHttpClient();
            Gson gson = new Gson();
            String json = gson.toJson(data);
            Log.d("POST_JSON",json);
            RequestBody requestBody = RequestBody.create(json, JSON);
            Request request = new Request.Builder()
                    .url(url + "/saveMsg")
                    .post(requestBody)
                    .build();
            Call call = okHttpClient.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Handler mainHandler = new Handler(Looper.getMainLooper());
                    final String error = e.getMessage();
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            showToast(error);
                        }
                    });
                }

                @Override
                public void onResponse(Call call, final Response response) throws IOException {
                    Handler mainHandler = new Handler(Looper.getMainLooper());
                    try{
                        if (response.isSuccessful()) {
                            Gson gson = new Gson();
                            final Result result = gson.fromJson(response.body().string(), Result.class);
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (result.code == 0) {
                                        showToast("保存成功");
                                    } else {
                                        showToast(result.msg);
                                    }
                                }
                            });
                        } else {
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    showToast("请求接口失败,状态码：" + response.code());
                                }
                            });
                        }
                    } catch (Exception ex) {
                        final String error = ex.getMessage();
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                showToast("发生错误："+error);
                            }
                        });
                    }


                }
            });
        } catch (Exception e) {
            showToast(e.getMessage());
        }
    }

    public class SmsReceiver extends BroadcastReceiver {
        private final MainActivity activity;

        public SmsReceiver(MainActivity activity) {
            this.activity = activity;
        }
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("MainActivity","收到短信");
            Bundle bundle = intent.getExtras();
            Object[] pdus = (Object[]) bundle.get("pdus");
            SmsMessage[] msgs = new SmsMessage[pdus.length];
            String format1 = intent.getStringExtra("format");
            for (int i = 0; i < pdus.length; i++) {
                msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i],format1);
            }
            Map<String,String> msgMap = new HashMap<String,String>();
            for (SmsMessage msg : msgs) {
                msgMap.put("mobile",msg.getDisplayOriginatingAddress());
                msgMap.put("content",msg.getDisplayMessageBody());
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date date = new Date(msg.getTimestampMillis());
                String time = format.format(date);
                msgMap.put("type","0");
                msgMap.put("time",time);
            }
            msgMap.put("pwd",activity.pwd);
            activity.post(activity.api,msgMap);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 注销广播
        unregisterReceiver(broadcastReceiver);
    }

}