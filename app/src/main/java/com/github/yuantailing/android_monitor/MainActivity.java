package com.github.yuantailing.android_monitor;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    static class MyHandler extends Handler {
        private int mVoltage;
        private final BatteryManager mBatteryManager;
        private final TextView mTextView;
        private final Locale mLocale;
        public MyHandler(Context context, TextView textView) {
            super();
            mBatteryManager = (BatteryManager)context.getSystemService(Context.BATTERY_SERVICE);
            MyHandler handler = this;
            context.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    handler.mVoltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
                }
            }, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            mTextView = textView;
            mLocale = Locale.getDefault();
        }
        @Override
        public void handleMessage(Message msg) {
            double current = mBatteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) / 1000. / 1000.;
            String s = "";
            s += String.format(mLocale,"%.3f", mVoltage / 1000.) + " V\n";
            s += String.format(mLocale,"%.3f", current) + " A\n";
            s += String.format(mLocale,"%.3f", mVoltage / 1000. * current) + " W\n\n";
            s += "CHARGE_COUNTER\n" + mBatteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER) + "\n\n";
            s += "CURRENT_NOW\n" + mBatteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) + "\n\n";
            s += "CURRENT_AVERAGE\n" + mBatteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE) + "\n\n";
            s += "CAPACITY\n" + mBatteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) + "\n\n";
            s += "ENERGY_COUNTER\n" + mBatteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER) + "\n\n";
            s += "EXTRA_VOLTAGE\n" + mVoltage + "\n\n";
            mTextView.setText(s);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Handler handler = new MyHandler(this, (TextView)findViewById(R.id.main_text));
        class MyTask extends TimerTask {
            @Override
            public void run() {
                handler.sendEmptyMessage(0);
            }
        };
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new MyTask(), 0, 500);
    }
}