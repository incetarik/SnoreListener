package ridvan.snorelistener.objects;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

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
import java.util.HashMap;

import ridvan.snorelistener.R;
import ridvan.snorelistener.helpers.Action;
import ridvan.snorelistener.helpers.Function;

public class AlarmManager {
    private static final String                                        ALARMS_FILE  = "Alarms";
    private static final ArrayList<Alarm>                              ALARMS       = new ArrayList<>();
    private static final HashMap<AlarmEvent, ArrayList<Action<Alarm>>> ALARM_EVENTS = new HashMap<>();
    public static Function<Context> ContextGetter;
    public static ridvan.snorelistener.helpers.Timer Timer = null;
    private static boolean  timerInitialized;
    private static Ringtone currentRingtone;
    private static boolean playingAllowed = true;

    public static Ringtone getCurrentRingtone() {
        return currentRingtone;
    }

    public static void registerListener(Action<Alarm> listener, AlarmEvent type) {
        ArrayList<Action<Alarm>> eventList;

        if (ALARM_EVENTS.containsKey(type)) {
            eventList = ALARM_EVENTS.get(type);

            if (eventList.contains(listener)) return;
            eventList.add(listener);
        }
        else {
            eventList = new ArrayList<>();
            eventList.add(listener);

            ALARM_EVENTS.put(type, eventList);
        }
    }

    public static int getAlarmCount() {
        return ALARMS.size();
    }

    public static void setPlayingAllowed(boolean state) {
        playingAllowed = state;

        Log.d("AlarmManager", "Playing state changed to " + state);
    }

    public static void addAlarm(Alarm alarm) {
        ALARMS.add(alarm);

        sort();
        initTimer();
        fireAlarmAddedEvent(alarm);
    }

    public static void initTimer() {
        if (!timerInitialized && Timer != null) {
            Timer.addOnTickListener(new Runnable() {
                Alarm nextAlarm = null;
                long remainingSeconds = -1;

                @Override
                public void run() {
                    if (nextAlarm == null) {
                        if ((nextAlarm = getNextAlarm()) == null) return;

                        remainingSeconds = (nextAlarm.getDate().getTime() - new Date().getTime()) / 1000;
                    }
                    else if (--remainingSeconds <= 0) {
                        startAlarm(nextAlarm);
                        remainingSeconds = -1;
                        nextAlarm = null;
                    }
                }
            });

            timerInitialized = true;

            if (Timer.isStarted()) return;
            Timer.run();
        }
    }

    public static void startAlarm(Alarm alarm) {
        updateAlarmDates(alarm);

        if (!alarm.isEnabled()) return;
        if (ContextGetter == null) return;
        if (currentRingtone != null && currentRingtone.isPlaying()) currentRingtone.stop();

        Log.d("AlarmManager", String.format("Alarm(%s) is starting...", alarm.getTitle()));

        Context context = ContextGetter.call();

        if (playingAllowed) {
            currentRingtone = RingtoneManager.getRingtone(context, alarm.getRingtoneUri());
            currentRingtone.play();
        }

        fireAlarmStartedEvent(alarm);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.logo)
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_VIBRATE)
                .setContentTitle(alarm.getTitle());

        if (alarm.isVibrationEnabled())
            builder.setVibrate(new long[] { 0, 1000, 200, 1000, 200, 1000 });

        Notification notification = builder.build();

        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(alarm.hashCode(), notification);
    }

    private static void fireAlarmStartedEvent(Alarm alarm) {
        Log.d("AlarmManager", String.format("Alarm(%s) is started", alarm.getTitle()));
        if (!ALARM_EVENTS.containsKey(AlarmEvent.STARTED)) return;

        for (Action<Alarm> event : ALARM_EVENTS.get(AlarmEvent.STARTED)) {
            event.call(alarm);
        }
    }

    /**
     * Updates saved alarm dates and fires related events if needed.
     * Firstly, current playing alarm will be handled to prevent collision in both
     * for-loop and timer events
     *
     * @param currentlyPlayingAlarm Currently Playing Alarm
     */
    public static void updateAlarmDates(Alarm currentlyPlayingAlarm) {
        Log.d("AlarmManager", "Updating alarm dates...");
        Date     now      = new Date();
        Calendar calendar = Calendar.getInstance();

        if (currentlyPlayingAlarm != null) {
            if (currentlyPlayingAlarm.isRepeatedWeekly()) {
                calendar.setTime(currentlyPlayingAlarm.getDate());
                calendar.add(Calendar.DAY_OF_MONTH, 7);
                currentlyPlayingAlarm.setDate(calendar.getTime());
                fireAlarmUpdatedEvent(currentlyPlayingAlarm);
            }
            else {
                ALARMS.remove(currentlyPlayingAlarm);
                fireAlarmFinishedEvent(currentlyPlayingAlarm);
            }
        }

        for (int i = ALARMS.size() - 1; i >= 0; i--) {
            Alarm alarm = ALARMS.get(i);
            if (!alarm.getDate().before(now)) continue;

            if (alarm.isRepeatedWeekly()) {
                calendar.setTime(alarm.getDate());
                calendar.add(Calendar.DAY_OF_MONTH, 7);
                alarm.setDate(calendar.getTime());

                fireAlarmUpdatedEvent(alarm);
                continue;
            }

            ALARMS.remove(i);
            fireAlarmFinishedEvent(alarm);
        }
    }

    private static void fireAlarmFinishedEvent(Alarm alarm) {
        Log.d("AlarmManager", String.format("Alarm(%s) is finished", alarm.getTitle()));
        if (!ALARM_EVENTS.containsKey(AlarmEvent.FINISHED)) return;

        for (Action<Alarm> event : ALARM_EVENTS.get(AlarmEvent.FINISHED)) {
            event.call(alarm);
        }
    }

    private static void fireAlarmUpdatedEvent(Alarm alarm) {
        Log.d("AlarmManager", String.format("Alarm(%s) is updated", alarm.getTitle()));
        if (!ALARM_EVENTS.containsKey(AlarmEvent.UPDATED)) return;

        for (Action<Alarm> event : ALARM_EVENTS.get(AlarmEvent.UPDATED)) {
            event.call(alarm);
        }
    }

    public static Alarm getNextAlarm() {
        if (ALARMS.isEmpty()) return null;

        Alarm nextAlarm = ALARMS.get(0);
        Log.d("AlarmManager", String.format("Next alarm set as Alarm(%s)", nextAlarm.getTitle()));

        return nextAlarm;
    }

    private static void fireAlarmAddedEvent(Alarm alarm) {
        Log.d("AlarmManager", String.format("Alarm(%s) is added", alarm.getTitle()));
        if (!ALARM_EVENTS.containsKey(AlarmEvent.ADDED)) return;

        for (Action<Alarm> event : ALARM_EVENTS.get(AlarmEvent.ADDED)) {
            event.call(alarm);
        }
    }

    public static void sort() {
        Collections.sort(ALARMS, new Comparator<Alarm>() {
            @Override
            public int compare(Alarm o1, Alarm o2) {
                return o1.getDate().compareTo(o2.getDate());
            }
        });
    }

    @SuppressLint("DefaultLocale")
    public static void saveAlarms(Context context) {
        Log.d("AlarmManager", "Saving alarms...");
        if (ALARMS.isEmpty()) return;

        FileOutputStream fos = null;
        try {
            fos = context.openFileOutput(ALARMS_FILE, Context.MODE_PRIVATE);
            FileWriter fw = new FileWriter(fos.getFD());
            for (Alarm alarm : ALARMS) {
                fw.write(String.format(
                        "%s¬%s¬%d¬%s¬%s¬%s\n",
                        alarm.getTitle(),
                        alarm.getRingtoneUri().toString(),
                        alarm.getDate().getTime(),
                        (alarm.isEnabled() ? "E" : "F"),
                        (alarm.isRepeatedWeekly() ? "E" : "F"),
                        (alarm.isVibrationEnabled() ? "E" : "F")));
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

    /**
     * Updates saved alarm dates and fires related events if needed.
     * This method intentionally created to update all dates registered.
     */
    public static void updateAlarmDates() {
        updateAlarmDates(null);
    }

    public static void loadSavedAlarms(Context context) {
        Log.d("AlarmManager", "Loading alarms...");
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

                Alarm alarm = new Alarm()
                        .setTitle(parts[0])
                        .setRingtoneUri(Uri.parse(parts[1]))
                        .setDate(new Date(Long.parseLong(parts[2])))
                        .setEnabled(parts[3].equals("E"))
                        .setRepeatedWeekly(parts[4].equals("E"))
                        .setVibrationEnabled(parts[5].equals("E"));

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

    public enum AlarmEvent {
        ADDED, STARTED, FINISHED, UPDATED
    }
}
