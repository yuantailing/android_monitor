package com.github.yuantailing.android_monitor;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.app.Activity;
import android.app.Application.ActivityLifecycleCallbacks;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity {

    private int mVoltage = 0;
    private String mainText;
    private final ArrayList<Double> history = new ArrayList<Double>();
    private int mHistoryOffset = 0;
    private boolean isAppInForeground = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final BatteryManager batteryManager = (BatteryManager)getSystemService(Context.BATTERY_SERVICE);
        final TextView textView = (TextView)findViewById(R.id.mainText);
        final SeekBar seekBar = (SeekBar)findViewById(R.id.seekBar);
        final TextView seekBarText = (TextView)findViewById(R.id.seekBarText);
        final SeekBar seekBarHistory = (SeekBar)findViewById(R.id.seekBarHistory);
        final LineChart chart = (LineChart)findViewById(R.id.lineChart);
        chart.getDescription().setText("");
        final Locale locale = Locale.getDefault();
        final Executor mainExecutor = ContextCompat.getMainExecutor(this);
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mVoltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
            }
        }, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        final Runnable updateView = new Runnable() {
            @Override
            public void run() {
                textView.setText(mainText);
                LineDataSet dataSet = new LineDataSet(new ArrayList<Entry>(), "Power (Watt)");
                int drawLimit = (int)Math.round(Math.pow(10, 1.5 + (double)seekBar.getProgress() * .001)) + 1;
                String s = "Width " + (drawLimit - 1);
                seekBarText.setText(s);
                float minP = 0;
                float maxP = 0;
                int history_dropout = Math.max(0, (int)Math.round((history.size() - drawLimit) * (1 - seekBarHistory.getProgress() / 1000.)));
                int x0 = Math.max(0, history.size() - history_dropout - drawLimit);
                for (int i = x0; i < history.size() - history_dropout; i++) {
                    float P = history.get(i).floatValue();
                    minP = Math.min(minP, P);
                    maxP = Math.max(maxP, P);
                    dataSet.addEntry(new Entry(i + mHistoryOffset, P));
                }
                dataSet.setDrawValues(drawLimit <= 100);
                dataSet.setDrawCircles(drawLimit <= 500);
                LineData lineData = new LineData(dataSet);
                chart.setData(lineData);
                chart.getXAxis().setAxisMinimum(history.size() + mHistoryOffset - history_dropout - drawLimit);
                chart.getXAxis().setAxisMaximum(history.size() + mHistoryOffset - history_dropout - 1);
                chart.setScaleXEnabled(false);
                chart.getAxisLeft().setAxisMinimum((float)Math.floor((double)minP) * 1.05f);
                chart.getAxisLeft().setAxisMaximum((float)Math.ceil((double)maxP) * 1.05f);
                chart.getAxisRight().setAxisMinimum((float)Math.floor((double)minP) * 1.05f);
                chart.getAxisRight().setAxisMaximum((float)Math.ceil((double)maxP) * 1.05f);
                chart.invalidate();
            }
        };
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) { }
            @Override
            public void onActivityStarted(Activity activity) { }
            @Override
            public void onActivityResumed(Activity activity) {
                // Activity 变为前台时调用
                isAppInForeground = true;
            }
            @Override
            public void onActivityPaused(Activity activity) {
                // Activity 变为后台时调用
                isAppInForeground = false;
            }
            @Override
            public void onActivityStopped(Activity activity) { }
            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) { }
            @Override
            public void onActivityDestroyed(Activity activity) { }
        });
        SeekBar.OnSeekBarChangeListener seekBawrListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mainExecutor.execute(updateView);
            }
        };
        seekBar.setOnSeekBarChangeListener(seekBawrListener);
        seekBarHistory.setOnSeekBarChangeListener(seekBawrListener);
        class UpdateTask extends TimerTask {
            @Override
            public void run() {
                boolean extraPrint = false;
                double current = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) / 1000. / 1000.;
                double P = mVoltage / 1000. * current;
                String s = "";
                s += String.format(locale,"%.3f", mVoltage / 1000.) + " V\n";
                s += String.format(locale,"%.3f", current) + " A\n";
                s += String.format(locale,"%.3f", P) + " W\n\n";
                s += "CHARGE_COUNTER\n" + batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER) + "\n\n";
                s += "CURRENT_NOW\n" + batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) + "\n\n";
                if (extraPrint) {
                    s += "CURRENT_AVERAGE\n" + batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE) + "\n\n";
                    s += "CAPACITY\n" + batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) + "\n\n";
                    s += "ENERGY_COUNTER\n" + batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER) + "\n\n";
                }
                s += "EXTRA_VOLTAGE\n" + mVoltage;
                mainText = s;
                if (mVoltage != 0) {
                    if (history.size() >= 1100000) {
                        history.subList(0, 100000).clear();
                        mHistoryOffset += 100000;
                    }
                    history.add(P);
                }
                if (isAppInForeground)
                    mainExecutor.execute(updateView);
            }
        };
        (new Timer()).scheduleAtFixedRate(new UpdateTask(), 0, 1000);
    }
}