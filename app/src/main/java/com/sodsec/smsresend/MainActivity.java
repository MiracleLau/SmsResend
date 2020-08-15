package com.sodsec.smsresend;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private EditText phoneEdit;
    private EditText apiEdit;
    private TextView statusLabel;
    private ScrollView scrollView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        phoneEdit = (EditText)findViewById(R.id.phoneNum);
        apiEdit = (EditText)findViewById(R.id.apiUrl);
        statusLabel = (TextView)findViewById(R.id.serviceStatus);
        scrollView = (ScrollView)findViewById(R.id.scrollText);
        try{
            SharedPreferences config = Config.getProperties(getApplicationContext());
            String phone = config.getString("phone","");
            String api = config.getString("api","");
            phoneEdit.setText(phone);
            apiEdit.setText(api);
        }catch (Exception ex){
            showToast(ex.getMessage());
        }

    }

    // 保存配置按钮点击事件
    public void saveConfigClick(View v) {
        String phone = phoneEdit.getText().toString();
        String api = apiEdit.getText().toString();
        if(api.isEmpty() || phone.isEmpty()){
            showToast("请确保手机号和api不为空");
            return;
        } else {
            Map<String, String> conf = new HashMap<String,String>();
            conf.put("phone",phone);
            conf.put("api",api);
            try{
                if(Config.setPropertiesMap(getApplicationContext(),conf)) {
                    showToast("保存成功");
                } else {
                    showToast("保存失败");
                }
            } catch (Exception ex){
                showToast("保存失败！"+ex.getMessage());
            }

        }
    }

    public void testApiClick(View v) {
        Tools.SendMsg("15254095511","test",statusLabel,scrollView);
    }

//    public void startListenClick(View v) {
//    }

    public void showToast(String text) {
        Toast.makeText(getApplicationContext(),text,Toast.LENGTH_SHORT).show();
        statusLabel.append(text);
        scrollView.fullScroll(ScrollView.FOCUS_DOWN);
    }

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

}