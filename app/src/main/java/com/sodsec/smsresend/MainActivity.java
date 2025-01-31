package com.sodsec.smsresend;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SmsMessage;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
    private Button startListenBtn;
    private String phone;
    private String api;
    private String pwd;
    public static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
    public static final String URI = "content://sms";
    private SMSContentObserver smsContentObserver;
    private Boolean isListening = false;
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
        startListenBtn = (Button)findViewById(R.id.startListen);
        try{
            SharedPreferences config = Config.getProperties(getApplicationContext());
            phone = config.getString("phone","");
            api = config.getString("api","");
            pwd = config.getString("pwd","");
            phoneEdit.setText(phone);
            apiEdit.setText(api);
            pwdEdit.setText(pwd);
            // 获取权限
            checkPermission();
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

    public void startListenClick(View v) {
        try{
            // 如果正在监听
            if(isListening) {
                getContentResolver().unregisterContentObserver(smsContentObserver);
                startListenBtn.setText("开始监听");
                showToast("停止监听");
                isListening = false;
            } else {
                // 注册观察者
                smsContentObserver = new SMSContentObserver(new Handler(),this);
                this.getContentResolver().registerContentObserver
                        (Uri.parse(URI), true, smsContentObserver);

                //回调
                smsContentObserver.setOnReceivedMessageListener(new SMSContentObserver.MessageListener() {
                    @Override
                    public void OnReceived(Map<String,String> message) {
                        message.put("pwd",pwd);
                        showToast("接收到新消息，开始处理");
                        post(api,message);
                    }
                });
                startListenBtn.setText("停止监听");
                showToast("开始监听");
                isListening = true;
            }
        }catch (Exception ex) {
            showToast("发生错误："+ex.getMessage());
        }

    }

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
    public void post(String url, @NotNull Map<String,String> data) {
        MediaType JSON=MediaType.parse("application/json; charset=utf-8");
        final Boolean isTest = data.containsKey("test");
        String smsContent  = "";
        if (data.containsKey("mobile") && data.containsKey("content")) {
            smsContent = "发送：" + data.getOrDefault("mobile","") + "\n内容：" + data.getOrDefault("content","");
        }
        final String sendContent = smsContent;
        try {
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .retryOnConnectionFailure(true)
                    .readTimeout(5, TimeUnit.SECONDS)
                    .connectTimeout(5,TimeUnit.SECONDS)
                    .build();
            Gson gson = new Gson();
            final String json = gson.toJson(data);
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
                            showToast("请求失败，"+error);
                            if(!isTest){
                                showToast("将短信转发到指定的手机上");
                                Tools.SendMsg(phone, sendContent, statusLabel, scrollView);
                            }
                        }
                    });
                }

                @Override
                public void onResponse(Call call, final Response response) throws IOException {
                    Handler mainHandler = new Handler(Looper.getMainLooper());
                    try {
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
                                    if(!isTest) {
                                        showToast("将短信转发到指定的手机上");
                                        Tools.SendMsg(phone, sendContent, statusLabel, scrollView);
                                    }
                                }
                            });
                        }
                    } catch (Exception ex) {
                        final String error = ex.getMessage();
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                showToast("发生错误：" + error);
                                if(!isTest) {
                                    showToast("将短信转发到指定的手机上");
                                    Tools.SendMsg(phone, sendContent, statusLabel, scrollView);
                                }
                            }
                        });
                    }


                }
            });
        } catch (Exception e) {
            showToast("发生错误："+e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getContentResolver().unregisterContentObserver(smsContentObserver);
    }

    // 获取权限
    private void checkPermission() {
        int canSendSms = ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.SEND_SMS);
        int canReceiveSms = ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.RECEIVE_SMS);
        int canReadSms = ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.READ_SMS);
        String[] permissions = new String[]{Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS};
        List<String> permissionList = new ArrayList<>();
        for (int i = 0; i < permissions.length; i++) {
            if (ContextCompat.checkSelfPermission(this, permissions[i]) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(permissions[i]);
            }
        }
        if (!permissionList.isEmpty()) {
            String[] permissionsArr = permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(this, permissionsArr, 1);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        //通过requestCode来识别是否同一个请求
        if (requestCode == 1){
            if (grantResults.length < 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED){
                showToast("请同意所有权限后才能正常使用本程序");
                this.finish();
            }
        }
    }
}