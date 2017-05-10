package ridvan.snorelistener.activities;

import android.animation.Animator;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.DataSetObserver;
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

import com.mariux.teleport.lib.TeleportClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import ridvan.snorelistener.R;
import ridvan.snorelistener.helpers.Action;
import ridvan.snorelistener.helpers.Function;
import ridvan.snorelistener.helpers.SoundLevelListener;
import ridvan.snorelistener.helpers.Timer;
import ridvan.snorelistener.objects.AlarmAdapter;
import ridvan.snorelistener.objects.AlarmManager;
import ridvan.snorelistener.objects.AudioRecorder;
import ridvan.snorelistener.objects.Record;
import ridvan.snorelistener.objects.RecordAdapter;
import ridvan.snorelistener.objects.Statistic;
import ridvan.snorelistener.objects.StatisticsAdapter;
import ridvan.snorelistener.views.SoundMeterView;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity {
    private boolean isVibrating;
    private boolean isRecording;
    private boolean isAudioPlaying;
    private boolean isVibrationEnabled;
    private boolean isWearVibrationEnabled;

    private ViewPager viewPager;
    private TextView  tvAlarmsInfo;
    private ImageView ivRecordButton;
    private Switch    switchVibration;
    private Switch    switchVibrationForWear;
    private TextView  tvRecordDuration;

    private RecyclerView rvAlarms;
    private RecyclerView rvRecords;
    private RecyclerView rvStatistics;

    private AlarmAdapter      alarmAdapter;
    private RecordAdapter     recordAdapter;
    private StatisticsAdapter statisticsAdapter;

    private Timer    timer;
    private Vibrator vibrator;

    private AudioRecorder  recorder;
    private SoundMeterView soundMeter;

    private ArrayList<Statistic> statistics;
    private TeleportClient       teleportClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity);

        // If we do not have taken permissions from the user, request them
        if (!checkPermission()) requestPermission();

        // Init essential components and variables
        initEssentials();

        (viewPager = (ViewPager) findViewById(R.id.viewPager)).setAdapter(new PagerAdapter() {
            private ArrayList<View> views = new ArrayList<>();

            @Override
            public int getCount() {
                return 4;
            }

            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                LayoutInflater inflater = LayoutInflater.from(getBaseContext());
                ViewGroup      layout   = null;

                // Depending to position, inflate the related layout and invoke the related function

                // NOTE: If a layout is being created first time, it will be added to views list.
                // For next usages of the views, views list will be used to prevent reinitialization
                // problems for some devices.

                switch (position) {
                    case 0:
                        if (views.size() > 0) {
                            layout = (ViewGroup) views.get(0);
                            break;
                        }

                        layout = (ViewGroup) inflater.inflate(R.layout.page_main, container, false);
                        initMainPage(layout);
                        views.add(layout);
                        break;

                    case 1:
                        if (views.size() > 1) {
                            layout = (ViewGroup) views.get(1);
                            break;
                        }

                        layout = (ViewGroup) inflater.inflate(R.layout.page_alarms, container, false);
                        initAlarmsPage(layout);
                        views.add(layout);
                        break;

                    case 2:
                        if (views.size() > 2) {
                            layout = (ViewGroup) views.get(2);
                            break;
                        }

                        layout = (ViewGroup) inflater.inflate(R.layout.page_records, container, false);
                        initRecordsPage(layout);
                        views.add(layout);
                        break;

                    case 3:
                        if (views.size() > 3) {
                            layout = (ViewGroup) views.get(3);
                            break;
                        }

                        layout = (ViewGroup) inflater.inflate(R.layout.page_statictics, container, false);
                        initStatisticsPage(layout);
                        views.add(layout);
                        break;
                }

                // Add the drawn layout to current container
                viewPager.addView(layout);

                return layout;
            }

            @Override
            public void destroyItem(ViewGroup container, int position, Object object) {
                viewPager.removeView((View) object);
            }

            @Override
            public boolean isViewFromObject(View view, Object object) {
                return view == object;
            }

            @Override
            public void unregisterDataSetObserver(DataSetObserver observer) {
                if (observer != null) {
                    super.unregisterDataSetObserver(observer);
                }
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
                    case 3:
                        return "Statistics";
                    default:
                        return "";
                }
            }
        });
    }

    /**
     * Initializes the main page here
     *
     * @param view Inflated view of the main page
     */
    private void initMainPage(View view) {
        ivRecordButton = (ImageView) view.findViewById(R.id.ivRecordButton);
        tvRecordDuration = (TextView) view.findViewById(R.id.tvRecordDuration);
        soundMeter = (SoundMeterView) view.findViewById(R.id.soundMeter);
        switchVibration = (Switch) view.findViewById(R.id.switchVibration);
        switchVibrationForWear = (Switch) view.findViewById(R.id.switchWearVibration);
        tvAlarmsInfo = (TextView) view.findViewById(R.id.tvAlarmsInfo);

        ivRecordButton.post(new Runnable() {
            @Override
            public void run() {
                soundMeter.setMinLevel(ivRecordButton.getWidth());
            }
        });

        // NOTE: We may face a situation that, when recording started and microphone icon
        // started blinking, after changing page from the main page and return back,
        // due to the function 'destroyItem' of view pager, we have to reinitialize all views
        // as seen here but to prevent overlaps and repeating / resetting views, we have to
        // block reinitialization of some components and their properties such as drawable of
        // ivRecordButton
        if (!isRecording) {
            ivRecordButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.mic));
        }

        tvAlarmsInfo.setText(String.format(getString(R.string.alarms_count), AlarmManager.getAlarmCount()));

        ivRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRecording) {
                    recorder.stopListening();
                    trySaveRecord();
                    Toast.makeText(MainActivity.this, "Recording stopped, statistics will be generated", 1).show();
                    isRecording = false;

                    timer.stop(true);
                    timer = null;

                    ivRecordButton.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.mic));
                    tvRecordDuration.setText(getString(R.string.duration_initial));
                    return;
                }

                // TODO: Implement this
                Toast.makeText(MainActivity.this, "Listening will start after 30 minutes", 1).show();

                isRecording = true;
                initTimer();
                timer.run();
                recorder.startListening();
            }
        });

        initTimer();
        final int duration = 200;

        switchVibration.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isVibrationEnabled = isChecked;
                vibrator.cancel();
            }
        });

        switchVibrationForWear.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isWearVibrationEnabled = isChecked;

                if (isChecked) {
                    buttonView.animate().translationY(-buttonView.getHeight()).setDuration(duration).start();
                    switchVibration.animate().alpha(1f).translationY(-buttonView.getHeight()).setListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            switchVibration.setVisibility(View.VISIBLE);
                            switchVibration.setAlpha(0f);
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {
                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) {
                        }
                    }).setDuration(duration).start();
                }
                else {
                    buttonView.animate().translationY(0).setDuration(duration).start();
                    switchVibration.animate().alpha(0f).translationY(0).setListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            switchVibration.setVisibility(View.VISIBLE);
                            switchVibration.setAlpha(1f);
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            switchVibration.setVisibility(View.GONE);
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {

                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) {

                        }
                    }).setDuration(duration).start();
                }
            }
        });

        switchVibration.setVisibility(View.GONE);

        switchVibration.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isWearVibrationEnabled = isChecked;
            }
        });

        // Due to the further initialization of this page, we may have switched the button before
        // and should re-draw it
        switchVibrationForWear.setChecked(isWearVibrationEnabled);
        switchVibration.setChecked(isVibrationEnabled);
    }

    private void initTimer() {
        if (timer == null) {
            // Init and let an action that allows timer to run any runnable on ui
            timer = new Timer(new Action<Runnable>() {
                @Override
                public void call(Runnable obj) {
                    runOnUiThread(obj);
                }
            }).addOnTickListener(new Runnable() {
                // Drawable field for further replacement of the ivRecordButton content
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

            // Let and allow alarm manager to use this timer
            AlarmManager.Timer = timer;
        }
    }

    /**
     * Initializes the alarm page
     *
     * @param view Inflated view of the alarm page
     */
    private void initAlarmsPage(View view) {
        // If we have initialized before, return
        if (rvAlarms != null) {
            if (alarmAdapter == null) {
                if (rvAlarms.getAdapter() != null)
                    alarmAdapter = (AlarmAdapter) rvAlarms.getAdapter();
                else
                    rvAlarms.setAdapter(alarmAdapter = new AlarmAdapter(rvAlarms, getFragmentManager()));
            }

            return;
        }

        rvAlarms = (RecyclerView) view.findViewById(R.id.rvAlarms);
        rvAlarms.setLayoutManager(new LinearLayoutManager(this));
        rvAlarms.setAdapter(new AlarmAdapter(rvAlarms, getFragmentManager()));

        // No need to add pre-saved files here for alarms, since it is handled by AlarmManager by
        // initEssentials() function
    }

    /**
     * Initializes the records page
     *
     * @param view Inflated view of the records page
     */
    private void initRecordsPage(View view) {
        Action<Boolean> onAudioStateChanged = new Action<Boolean>() {
            @Override
            public void call(Boolean obj) {
                isAudioPlaying = obj;
            }
        };

        // If we have initialized before, return
        if (rvRecords != null) {
            if (recordAdapter == null) {
                if (rvRecords.getAdapter() != null)
                    recordAdapter = (RecordAdapter) rvRecords.getAdapter();
                else
                    rvRecords.setAdapter(recordAdapter = new RecordAdapter(rvRecords, onAudioStateChanged));
            }

            return;
        }

        rvRecords = (RecyclerView) view.findViewById(R.id.rvRecords);
        rvRecords.setLayoutManager(new LinearLayoutManager(this));
        rvRecords.setAdapter(recordAdapter = new RecordAdapter(rvRecords, onAudioStateChanged));

        addPreSavedFiles();
    }

    /**
     * Adds saved files of records here
     */
    private void addPreSavedFiles() {
        // Get the directory
        File directory = new File(Environment.getExternalStorageDirectory().getPath());
        if (directory.listFiles() == null) return;

        // And if it is not null (then we are here), list the files inside
        for (File file : directory.listFiles()) {
            // For each 'file's absolute path
            String filePath = file.getAbsolutePath();

            // If it does NOT contain 'record-' prefix and '.aud' suffix, continue with next loop
            if (!filePath.contains("record-") || !filePath.endsWith(".aud")) continue;

            String[] info = filePath.split("-");
            if (info.length < 3) {
                file.delete();
                continue;
            }

            // Create Record and its information here
            Date   recordDate   = new Date(Long.parseLong(info[1]));
            double recordLength = Double.parseDouble(info[2].substring(0, info[2].lastIndexOf('.')));

            Record record = new Record(filePath, recordDate, recordLength);
            recordAdapter.addRecord(record);
        }
    }

    /**
     * Initializes the statistics page
     *
     * @param view Inflated view of the statistics page
     */
    private void initStatisticsPage(View view) {
        // No need to load statistics list here, it will be loaded in initEssentials() function
        if (rvStatistics != null) {
            if (statisticsAdapter == null) {
                if (rvStatistics.getAdapter() != null)
                    statisticsAdapter = (StatisticsAdapter) rvStatistics.getAdapter();
                else rvStatistics.setAdapter(statisticsAdapter = new StatisticsAdapter(statistics));
            }

            return;
        }

        rvStatistics = (RecyclerView) view.findViewById(R.id.rvStatistics);
        rvStatistics.setLayoutManager(new LinearLayoutManager(this));
        rvStatistics.setAdapter(statisticsAdapter = new StatisticsAdapter(statistics));
    }

    /**
     * Initializes the essential components and variables of the class:
     * - AudioRecorder
     * - Vibrator Service
     * - Alarm Manager
     * - Sound Level Listener
     */
    private void initEssentials() {
        teleportClient = new TeleportClient(this);

        recorder = new AudioRecorder();
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        statistics = Statistic.loadStatistics(this);

        // Let AlarmManager get context whenever it needs
        AlarmManager.ContextGetter = new Function<Context>() {
            @Override
            public Context call() {
                return getApplicationContext();
            }
        };

        // Create binding for alarms that, when added or removed, update tvAlarmsInfo view
        AlarmManager.setOnAlarmAddedListener(new Runnable() {
            @Override
            public void run() {
                tvAlarmsInfo.setText(String.format(getString(R.string.alarms_count), AlarmManager.getAlarmCount()));
            }
        });

        // Let AlarmManager load previously saved alarms
        AlarmManager.loadSavedAlarms(this);

        // Sound level listener
        recorder.setListener(new SoundLevelListener() {
            private int remainingStepCount = 0;

            @Override
            public void onMeasure(final double db) {
                Log.d("dB", String.valueOf(db));

                // While audio is playing, wait until it ends, and do not listen sound
                if (isAudioPlaying) {
                    if (!soundMeter.clearDraw()) return;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Cannot listen while audio is playing!", 1).show();
                            syncVibration(false);
                        }
                    });

                    return;
                }

                // Send dB information to the soundMeter view
                soundMeter.post(new Runnable() {
                    @Override
                    public void run() {
                        soundMeter.setCurrentLevel((int) db);
                    }
                });

                // If current dB is not enough to vibrate
                if (db < AudioRecorder.DB_LEVEL_TO_VIBRATE && --remainingStepCount < 0) {
                    trySaveRecord();

                    if (isVibrationEnabled) {
                        isVibrating = false;
                        vibrator.cancel();
                    }

                    if (isWearVibrationEnabled) {
                        syncVibration(false);
                    }

                    // -1 is an indicator that when it is, recorder will set 'NOW' as currently
                    // recording Record object start date
                    recorder.setRecordingStartDate(-1);
                }
                else {
                    if (recorder.getRecordingStartDate() < 0) {
                        recorder.setRecordingStartDate(new Date().getTime());
                    }

                    // For each collected (short type as) bytes
                    for (byte currentByte : AudioRecorder.short2byte(recorder.getBuffer())) {
                        // save them temporarily
                        recorder.getRecordingBytes().add(currentByte);
                    }

                    refreshRemainingSteps();

                    if (isWearVibrationEnabled) syncVibration(true);
                    if (!isVibrationEnabled || isVibrating) return;

                    vibrator.vibrate(new long[] { 0, 100, 0 }, 0);
                    isVibrating = true;
                }
            }

            private void refreshRemainingSteps() {
                int secondsAllowedAsLow = 5;
                remainingStepCount = (secondsAllowedAsLow * 1000 / recorder.getLatencyMillis());
            }
        });
    }

    /**
     * Tries saving current record if it is possible, by these conditions:
     * 1) Recorder should be started recording and should set recordingStartDate
     * 2) Recorded length in seconds should be greater than (or equal to) {@link
     * AudioRecorder#MINIMUM_AUDIO_LENGTH}
     */
    private void trySaveRecord() {
        if (recorder.getRecordingStartDate() < 0) return;

        Date                  endDate        = new Date();
        final Date            startDate      = new Date(recorder.getRecordingStartDate());
        final double          secondDiff     = ((endDate.getTime() - startDate.getTime()) / 1000.0);
        final ArrayList<Byte> recordingBytes = new ArrayList<>(recorder.getRecordingBytes());

        if (secondDiff < AudioRecorder.MINIMUM_AUDIO_LENGTH) {
            recorder.resetRecordingBytes();
            return;
        }

        recorder.resetRecordingBytes();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final Statistic statistic = new Statistic(startDate);
                statistic.setTotalSecondsSnored((long) secondDiff);

                final Record record = new Record();
                record.setRecordDate(startDate);
                record.setDurationSeconds(secondDiff);

                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(record.getFileName());

                    for (Byte currentByte : recordingBytes) {
                        fos.write(currentByte.intValue());
                    }

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
                        if (recordAdapter == null) {
                            if (rvRecords != null) {
                                if (rvRecords.getAdapter() == null) {
                                    rvRecords.setAdapter(recordAdapter = new RecordAdapter(rvRecords, new Action<Boolean>() {
                                        @Override
                                        public void call(Boolean obj) {
                                            isAudioPlaying = obj;
                                        }
                                    }));
                                }
                                else {
                                    recordAdapter = (RecordAdapter) rvRecords.getAdapter();
                                }
                            }
                        }
                        else {
                            recordAdapter.addRecord(record);
                        }

                        if (statisticsAdapter == null) {
                            statistics.add(statistic);
                        }
                        else {
                            statisticsAdapter.addStatistic(statistic);
                        }

                        Toast.makeText(MainActivity.this, "Successfully written", 1).show();
                    }
                });

                recordingBytes.clear();
            }
        }).start();
    }

    private void syncVibration(boolean vibrationState) {
        if (!isWearVibrationEnabled) return;
        teleportClient.syncBoolean(Context.VIBRATOR_SERVICE, vibrationState);
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
    protected void onStart() {
        super.onStart();
        teleportClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        teleportClient.disconnect();
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

    @Override
    protected void onPause() {
        super.onPause();
        AlarmManager.saveAlarms(this);
        Statistic.saveStatistics(this, statistics);
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
}
