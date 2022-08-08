package com.github.yuantailing.android_monitor;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.widget.TextView;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity {

    private int mVoltage = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final BatteryManager batteryManager = (BatteryManager)getSystemService(Context.BATTERY_SERVICE);
        final TextView textView = (TextView)findViewById(R.id.main_text);
        final Locale locale = Locale.getDefault();
        final Executor mainExecutor = ContextCompat.getMainExecutor(this);
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mVoltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
            }
        }, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        class MyTask extends TimerTask {
            @Override
            public void run() {
                mainExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        double current = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) / 1000. / 1000.;
                        String s = "";
                        s += String.format(locale,"%.3f", mVoltage / 1000.) + " V\n";
                        s += String.format(locale,"%.3f", current) + " A\n";
                        s += String.format(locale,"%.3f", mVoltage / 1000. * current) + " W\n\n";
                        s += "CHARGE_COUNTER\n" + batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER) + "\n\n";
                        s += "CURRENT_NOW\n" + batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) + "\n\n";
                        s += "CURRENT_AVERAGE\n" + batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE) + "\n\n";
                        s += "CAPACITY\n" + batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) + "\n\n";
                        s += "ENERGY_COUNTER\n" + batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER) + "\n\n";
                        s += "EXTRA_VOLTAGE\n" + mVoltage + "\n\n";
                        textView.setText(s);
                    }
                });
            }
        };
        (new Timer()).scheduleAtFixedRate(new MyTask(), 0, 500);
    }
}