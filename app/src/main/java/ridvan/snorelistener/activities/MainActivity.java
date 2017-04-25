package ridvan.snorelistener.activities;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import ridvan.snorelistener.R;
import ridvan.snorelistener.helpers.Action;
import ridvan.snorelistener.helpers.Function;
import ridvan.snorelistener.helpers.SoundLevelListener;
import ridvan.snorelistener.helpers.Statistic;
import ridvan.snorelistener.helpers.Timer;
import ridvan.snorelistener.objects.AlarmAdapter;
import ridvan.snorelistener.objects.AlarmManager;
import ridvan.snorelistener.objects.AudioRecorder;
import ridvan.snorelistener.objects.Record;
import ridvan.snorelistener.objects.RecordAdapter;
import ridvan.snorelistener.views.SoundMeterView;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity {
    private ImageView ivRecordButton;
    private TextView  tvRecordDuration;
    private Switch    switchVibration;
    private TextView  tvAlarmsInfo;

    private ArrayList<Statistic> statistics;

    private RecyclerView rvRecords;
    private RecyclerView rvAlarms;

    private RecordAdapter recordAdapter;

    private Timer    timer;
    private Vibrator vibrator;

    private boolean isVibrating;
    private boolean isVibrationEnabled;
    private boolean isRecording;

    private SoundMeterView soundMeter;
    private AudioRecorder  recorder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity);

        if (!checkPermission()) requestPermission();

        initEssentials();

        ((ViewPager) findViewById(R.id.viewPager)).setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                return 3;
            }

            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                LayoutInflater inflater = LayoutInflater.from(getBaseContext());
                ViewGroup      layout   = null;

                switch (position) {
                    case 0:
                        layout = (ViewGroup) inflater.inflate(R.layout.page_main, container, false);
                        initMainPage(layout);
                        break;

                    case 1:
                        layout = (ViewGroup) inflater.inflate(R.layout.page_alarms, container, false);
                        initAlarmsPage(layout);
                        break;

                    case 2:
                        layout = (ViewGroup) inflater.inflate(R.layout.page_records, container, false);
                        initRecordsPage(layout);
                        break;

                    case 3:
                        layout = (ViewGroup) inflater.inflate(R.layout.page_statictics, container, false);
                        initStatisticsPage(layout);
                        break;
                }

                container.addView(layout);

                return layout;
            }

            @Override
            public void destroyItem(ViewGroup container, int position, Object object) {
                container.removeView((View) object);
            }

            @Override
            public boolean isViewFromObject(View view, Object object) {
                return view == object;
            }

            @Override
            public CharSequence getPageTitle(int position) {
                switch (position) {
                    case 0:
                        return "Record";
                    case 1:
                        return "Alarms";
                    case 2:
                        return "Records";
                    default:
                        return "";
                }
            }
        });
    }

    private void initMainPage(View view) {
        ivRecordButton = (ImageView) view.findViewById(R.id.ivRecordButton);
        tvRecordDuration = (TextView) view.findViewById(R.id.tvRecordDuration);
        soundMeter = (SoundMeterView) view.findViewById(R.id.soundMeter);
        switchVibration = (Switch) view.findViewById(R.id.switchVibration);
        tvAlarmsInfo = (TextView) view.findViewById(R.id.tvAlarmsInfo);

        ivRecordButton.post(new Runnable() {
            @Override
            public void run() {
                soundMeter.setMinLevel(ivRecordButton.getWidth());
            }
        });

        if (!isRecording) {
            ivRecordButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.mic));
        }

        tvAlarmsInfo.setText(String.format(getString(R.string.alarms_count), AlarmManager.getAlarmCount()));

        ivRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRecording) return;

                Toast.makeText(MainActivity.this, "Listening will start after 30 minutes", 1).show();

                isRecording = true;
                timer.run();
                recorder.startListening();
            }
        });

        if (timer == null) {
            timer = new Timer(new Action<Runnable>() {
                @Override
                public void call(Runnable obj) {
                    runOnUiThread(obj);
                }
            }).addOnTickListener(new Runnable() {
                Drawable drawable;

                @Override
                public void run() {
                    if (!isRecording) return;

                    tvRecordDuration.setText(timer.toString());

                    Drawable currentDrawable = ivRecordButton.getDrawable();
                    if (currentDrawable == null) {
                        ivRecordButton.setImageDrawable(drawable);
                    }
                    else {
                        if (drawable == null) drawable = currentDrawable;
                        ivRecordButton.setImageDrawable(null);
                    }
                }
            });

            AlarmManager.Timer = timer;
        }

        switchVibration.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isVibrationEnabled = isChecked;
                vibrator.cancel();
            }
        });

        switchVibration.setChecked(isVibrationEnabled);
    }

    private void initAlarmsPage(View view) {
        if (rvAlarms != null) return;

        rvAlarms = (RecyclerView) view.findViewById(R.id.rvAlarms);
        rvAlarms.setLayoutManager(new LinearLayoutManager(this));
        rvAlarms.setAdapter(new AlarmAdapter(rvAlarms, getFragmentManager()));
    }

    private void initRecordsPage(View view) {
        if (rvRecords != null) return;

        rvRecords = (RecyclerView) view.findViewById(R.id.rvRecords);
        rvRecords.setLayoutManager(new LinearLayoutManager(this));
        rvRecords.setAdapter(recordAdapter = new RecordAdapter(rvRecords));

        addPreSavedFiles();
    }

    private void addPreSavedFiles() {
        File directory = new File(Environment.getExternalStorageDirectory().getPath());
        if (directory.listFiles() == null) return;

        for (File file : directory.listFiles()) {
            String filePath = file.getAbsolutePath();
            if (!filePath.contains("record-") || !filePath.endsWith(".aud")) continue;

            String[] info = filePath.split("-");
            if (info.length < 3) {
                file.delete();
                continue;
            }

            Date recordDate   = new Date(Long.parseLong(info[1]));
            int  recordLength = Integer.parseInt(info[2].substring(0, info[2].indexOf('.')));

            Record record = new Record(filePath, recordDate, recordLength);
            recordAdapter.addRecord(record);
        }
    }

    private void initStatisticsPage(View view) {

    }

    private void initEssentials() {
        recorder = new AudioRecorder();
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        AlarmManager.ContextGetter = new Function<Context>() {
            @Override
            public Context call() {
                return getApplicationContext();
            }
        };

        AlarmManager.setOnAlarmAddedListener(new Runnable() {
            @Override
            public void run() {
                tvAlarmsInfo.setText(String.format(getString(R.string.alarms_count), AlarmManager.getAlarmCount()));
            }
        });

        AlarmManager.loadSavedAlarms(this);

        recorder.setListener(new SoundLevelListener() {
            @Override
            public void onMeasure(final double db) {
                Log.d("dB", String.valueOf(db));

                soundMeter.post(new Runnable() {
                    @Override
                    public void run() {
                        soundMeter.setCurrentLevel((int) db);
                    }
                });

                if (db < AudioRecorder.DB_LEVEL_TO_VIBRATE) {
                    // trySaveRecord();

                    if (isVibrationEnabled) {
                        isVibrating = false;
                        vibrator.cancel();
                    }

                    recorder.setRecordingStartDate(-1);
                }
                else {
                    if (recorder.getRecordingStartDate() == -1) {
                        recorder.setRecordingStartDate(new Date().getTime());
                    }

                    for (byte currentByte : AudioRecorder.short2byte(recorder.getBuffer())) {
                        recorder.getRecordingBytes().add(currentByte);
                    }

                    if (!isVibrationEnabled || isVibrating) return;

                    vibrator.vibrate(new long[] { 0, 100, 0 }, 0);
                    isVibrating = true;
                }
            }
        });
    }

    /**
     * This method will be called when the application is closed
     */
    @Override
    protected void onDestroy() {
        // Get current event handler of the application and remove the callbacks and messages
        // (to stop vibrator)
        new Handler(Looper.getMainLooper()).removeCallbacksAndMessages(null);

        // Call default action
        super.onDestroy();
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[] { WRITE_EXTERNAL_STORAGE, RECORD_AUDIO }, 1);
    }

    private boolean checkPermission() {
        int perm1 = ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE);
        int perm2 = ContextCompat.checkSelfPermission(this, RECORD_AUDIO);
        return perm1 == PackageManager.PERMISSION_GRANTED && perm2 == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onPause() {
        super.onPause();
        AlarmManager.saveAlarms(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0) {
                    boolean storagePermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean recordPermission  = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    if (storagePermission && recordPermission) {
                        Toast.makeText(this, "Permission Granted", 1).show();
                    }
                    else {
                        Toast.makeText(this, "Permission should be granted", 1).show();
                        finishAffinity();
                    }
                }

                break;
            }
        }
    }

    private void trySaveRecord() {
        if (recorder.getRecordingStartDate() < 0) return;

        Date endDate   = new Date();
        Date startDate = new Date(recorder.getRecordingStartDate());

        long secondDiff = ((endDate.getTime() - startDate.getTime()) / 1000);

        ArrayList<Byte> recordingBytes = recorder.getRecordingBytes();

        if (secondDiff < AudioRecorder.MINIMUM_AUDIO_LENGTH) {
            recordingBytes.clear();
            return;
        }

        final Record record = new Record();
        record.setRecordDate(startDate);
        record.setDurationSeconds((int) secondDiff);

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(record.getFileName());

            byte[] bytesToWrite = new byte[recordingBytes.size()];
            for (int i = 0; i < recordingBytes.size(); i++) {
                bytesToWrite[i] = recordingBytes.get(i);
            }

            fos.write(bytesToWrite);
            fos.flush();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                recordAdapter.addRecord(record);
                Toast.makeText(MainActivity.this, "Successfully written", 1).show();
            }
        });

        recordingBytes.clear();
    }

    private void moveFile(String inputPath, String outputPath) {
        File inputFile = new File(inputPath), outputFile = new File(outputPath);

        if (!inputFile.exists()) return;
        byte[] buffer = new byte[1024];

        FileInputStream  fis = null;
        FileOutputStream fos = null;

        try {
            fis = new FileInputStream(inputFile);
            fos = new FileOutputStream(outputFile);

            int read;
            while ((read = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            try {
                if (fis != null) fis.close();
                if (fos != null) fos.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
