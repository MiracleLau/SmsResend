package com.sodsec.smsresend;

import android.app.Activity;
import android.database.ContentObservable;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SMSContentObserver extends ContentObserver {
    //所有短信
    public static final String SMS_URI_ALL = "content://sms/";
    //收件箱短信
    public static final String SMS_URI_INBOX = "content://sms/inbox";
    //发件箱短信
    public static final String SMS_URI_SEND = "content://sms/sent";
    //草稿箱短信
    public static final String SMS_URI_DRAFT = "content://sms/draft";
    private Activity mActivity;
    private Map<String,String> smsMap;
    private MessageListener mMessageListener;

    public SMSContentObserver(Handler handler, Activity activity) {
        super(handler);
        this.mActivity = activity;
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);
        Uri uri = Uri.parse(SMS_URI_INBOX);
        smsMap = this.getSmsInfo(uri,mActivity);
        mMessageListener.OnReceived(smsMap);
    }

    /**
     * 注意:
     * 该处只用按照时间降序取出第一条即可
     * 这条当然是最新收到的消息
     */
    private Map<String,String> getSmsInfo(Uri uri,Activity activity){
        // String[] projection = new String[] { "_id", "address", "person","body", "date", "type" };
        String[] projection = new String[] { "address", "date","body" };
        Cursor cusor = activity.getApplication().getContentResolver().query(uri, projection, null, null,"date desc limit 1");
        // int nameColumn = cusor.getColumnIndex("person");
        int phoneNumberColumn = cusor.getColumnIndex("address");
        int smsbodyColumn = cusor.getColumnIndex("body");
        // int dateColumn = cusor.getColumnIndex("date");
        // int typeColumn = cusor.getColumnIndex("type");
        Map<String,String> msg = new HashMap<>();
        if (cusor != null) {
            while (cusor.moveToNext()) {
                msg.put("mobile",cusor.getString(phoneNumberColumn));    //获取手机号
                msg.put("content",cusor.getString(smsbodyColumn));
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date date = new Date(System.currentTimeMillis());
                String time = format.format(date);
                msg.put("time",time);
                msg.put("type","0");  //固定为0，为以后的功能做准备，0为短信，1为来电
            }
            cusor.close();
        }
        return msg;
    }



    // 回调接口
    public interface MessageListener {
        public void OnReceived(Map<String,String> message);
    }

    public void setOnReceivedMessageListener(
            MessageListener messageListener) {
        this.mMessageListener=messageListener;
    }
}
