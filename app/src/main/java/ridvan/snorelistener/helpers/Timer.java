package ridvan.snorelistener.helpers;

import java.util.ArrayList;
import java.util.TimerTask;

public class Timer implements Runnable {
    private          int                 seconds;
    private          int                 minutes;
    private          int                 hours;
    private          long                delay;
    private volatile boolean             isWorking;
    private          boolean             isStarted;
    private          java.util.Timer     handler;
    private          ArrayList<Runnable> onTickEvents;
    private          Action<Runnable>    onUiThread;
    private          StringBuilder       sb;

    public Timer(Action<Runnable> onUiThread) {
        this.onUiThread = onUiThread;
        this.sb = new StringBuilder();
    }

    public static String prettify(int totalSeconds) {
        int seconds = totalSeconds;
        int minutes = totalSeconds / 60;
        seconds -= minutes * 60;

        int hours = minutes / 60;
        minutes -= hours * 60;

        String strHours = "0", strMinutes = "0", strSeconds = "0";
        if (hours >= 10) strHours = String.valueOf(hours);
        else strHours += hours;

        if (minutes >= 10) strMinutes = String.valueOf(minutes);
        else strMinutes += minutes;

        if (seconds >= 10) strSeconds = String.valueOf(seconds);
        else strSeconds += strSeconds;

        return String.format("%s:%s:%s", strHours, strMinutes, strSeconds);
    }

    public int getSeconds() {
        return seconds;
    }

    public int getMinutes() {
        return minutes;
    }

    public int getHours() {
        return hours;
    }

    public int getTotalSeconds() {
        return seconds + minutes * 60 + hours * 60 * 60;
    }

    @Override
    public String toString() {
        if (hours < 10) sb.append('0');
        sb.append(hours).append(':');

        if (minutes < 10) sb.append('0');
        sb.append(minutes).append(':');

        if (seconds < 10) sb.append('0');
        sb.append(seconds);

        String str = sb.toString();
        sb.setLength(0);
        return str;
    }

    public Timer addOnTickListener(Runnable runnable) {
        (onTickEvents == null ? (onTickEvents = new ArrayList<>()) : onTickEvents).add(runnable);
        return this;
    }

    public void pause() {
        isWorking = false;
    }

    public boolean isStarted() {
        return isStarted;
    }

    public boolean isWorking() {
        return isWorking;
    }

    public void reset() {
        reset(0);
    }

    public void reset(long resetDelay) {
        stop();
        delay = resetDelay;
        run();
    }

    @Override
    public void run() {
        if (isWorking) return;
        if (isStarted) isWorking = true;
        else {
            isStarted = true;
            isWorking = true;
            if (handler != null) {
                handler.purge();
            }

            seconds = minutes = hours = 0;

            handler = new java.util.Timer();
            handler.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if (!isWorking) return;

                    if (++seconds == 60) {
                        if (++minutes == 60) {
                            hours++;

                            minutes = 0;
                        }

                        seconds = 0;
                    }

                    for (Runnable runnable : onTickEvents) {
                        onUiThread.call(runnable);
                    }
                }
            }, delay, 1000);
        }
    }

    public void stop() {
        isStarted = false;
        isWorking = false;
        if (handler != null) handler.purge();
    }
}
