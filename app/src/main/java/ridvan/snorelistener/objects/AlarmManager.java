package ridvan.snorelistener.objects;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import ridvan.snorelistener.helpers.Function;

public class AlarmManager {
    private static final String                             ALARMS_FILE = "Alarms";
    private static final ArrayList<Alarm>                   ALARMS      = new ArrayList<>();
    public static        ridvan.snorelistener.helpers.Timer Timer       = null;
    public static  Function<Context> ContextGetter;
    private static Ringtone          currentRingtone;
    private static boolean           timerInitialized;
    private static Runnable          onAlarmAddedListener;

    public static int getAlarmCount() {
        return ALARMS.size();
    }

    public static void setOnAlarmAddedListener(Runnable runnable) {
        onAlarmAddedListener = runnable;
    }

    public static void addAlarm(Alarm alarm) {
        ALARMS.add(alarm);

        sort();

        if (!timerInitialized && Timer != null) {
            Timer.addOnTickListener(new Runnable() {
                Alarm nextAlarm = null;
                long remainingSeconds = -1;

                @Override
                public void run() {
                    if (nextAlarm == null) {
                        nextAlarm = getNextAlarm();

                        if (nextAlarm != null) {
                            remainingSeconds = (new Date().getTime() - nextAlarm.getDate().getTime()) / 1000;
                        }
                    }
                    else if (--remainingSeconds <= 0) {
                        startAlarm(nextAlarm);
                        remainingSeconds = -1;
                        nextAlarm = null;
                    }
                }
            });

            timerInitialized = true;
        }

        if (onAlarmAddedListener == null) return;
        onAlarmAddedListener.run();
    }

    public static void startAlarm(Alarm alarm) {
        if (!alarm.isEnabled()) return;
        if (ContextGetter == null) return;
        if (currentRingtone != null && currentRingtone.isPlaying()) currentRingtone.stop();

        currentRingtone = RingtoneManager.getRingtone(ContextGetter.call(), alarm.getRingtoneUri());
        currentRingtone.play();
    }

    public static void sort() {
        Collections.sort(ALARMS, new Comparator<Alarm>() {
            @Override
            public int compare(Alarm o1, Alarm o2) {
                return o1.getDate().compareTo(o2.getDate());
            }
        });
    }

    public static Alarm getNextAlarm() {
        if (ALARMS.isEmpty()) return null;
        return ALARMS.get(0);
    }

    @SuppressLint("DefaultLocale")
    public static void saveAlarms(Context context) {
        if (ALARMS.isEmpty()) return;

        FileOutputStream fos = null;
        try {
            fos = context.openFileOutput(ALARMS_FILE, Context.MODE_PRIVATE);
            FileWriter fw = new FileWriter(fos.getFD());
            for (Alarm alarm : ALARMS) {
                fw.write(String.format("%s¬%s¬%d¬%s¬%s¬%s\n", alarm.getTitle(), alarm.getRingtoneUri().toString(), alarm.getDate().getTime(), (alarm.isEnabled() ? "E" : "F"), (alarm.isRepeatedWeekly() ? "E" : "F"), (alarm.isVibrationEnabled() ? "E" : "F")));
            }

            fw.close();
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
    }

    public static void updateAlarmDates() {
        Date now = new Date();
        for (Alarm alarm : ALARMS) {
            if (!alarm.isRepeatedWeekly()) continue;
            if (!now.after(alarm.getDate())) continue;

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(alarm.getDate());
            calendar.add(Calendar.WEEK_OF_MONTH, 1);
            alarm.setDate(calendar.getTime());
        }
    }

    public static void loadSavedAlarms(Context context) {
        FileInputStream fis = null;
        try {
            fis = context.openFileInput(ALARMS_FILE);
            FileReader     fr = new FileReader(fis.getFD());
            BufferedReader br = new BufferedReader(fr);

            String line;
            while ((line = br.readLine()) != null) {
                if (line.isEmpty()) continue;
                String[] parts = line.split("¬");

                if (parts.length != 6) continue;
                if (parts[0].equals("null")) continue;

                Alarm alarm = new Alarm().setTitle(parts[0]).setRingtoneUri(Uri.parse(parts[1])).setDate(new Date(Long.parseLong(parts[2]))).setEnabled(parts[3].equals("E")).setRepeatedWeekly(parts[4].equals("E")).setVibrationEnabled(parts[5].equals("E"));

                if (ALARMS.contains(alarm)) continue;

                ALARMS.add(alarm);
            }

            fr.close();
            br.close();
        }
        catch (IOException e) {
            // ignored
        }
        finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        sort();
    }

    public static Alarm getAlarmAt(int index) {
        return ALARMS.get(index - 1);
    }

    public static void removeAlarm(int index) {
        ALARMS.remove(index - 1);
        sort();
    }
}
