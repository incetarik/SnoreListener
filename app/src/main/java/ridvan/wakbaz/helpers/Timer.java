package ridvan.wakbaz.helpers;

import java.util.ArrayList;
import java.util.TimerTask;

public class Timer implements Runnable {
    private          long    delay;
    private          int     hours;
    private          int     seconds;
    private          int     minutes;
    private volatile boolean isWorking;
    private          boolean isStarted;

    private StringBuilder       sb;
    private java.util.Timer     handler;
    private Action<Runnable>    onUiThread;
    private ArrayList<Runnable> onTickEvents;

    public Timer(Action<Runnable> onUiThread) {
        this.onUiThread = onUiThread;
        this.sb = new StringBuilder();
    }

    /**
     * Formats the total seconds in "HH:MM:SS" format
     *
     * @param totalSeconds Total second to be converted
     *
     * @return Formatted string of total seconds
     */
    public static String prettify(long totalSeconds) {
        long seconds = totalSeconds;
        long minutes = totalSeconds / 60;
        seconds -= minutes * 60;

        long hours = minutes / 60;
        minutes -= hours * 60;

        String strHours = "0", strMinutes = "0", strSeconds = "0";
        if (hours >= 10) strHours = String.valueOf(hours);
        else strHours += hours;

        if (minutes >= 10) strMinutes = String.valueOf(minutes);
        else strMinutes += minutes;

        if (seconds >= 10) strSeconds = String.valueOf(seconds);
        else strSeconds += seconds;

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

    /**
     * Resets current timer after given milliseconds
     * @param resetDelay Reset delay of milliseconds
     */
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
                    // If it is working
                    if (!isWorking) return;

                    // And currently increased seconds is 60
                    if (++seconds == 60) {
                        // And currently increased minutes is 60
                        if (++minutes == 60) {
                            // Increase hours by 1
                            hours++;

                            // Set current minutes as 0
                            minutes = 0;
                        }

                        // Set current seconds as 0
                        seconds = 0;
                    }

                    // For each registered tick events
                    for (Runnable runnable : onTickEvents) {
                        // Invoke on ui thread
                        onUiThread.call(runnable);
                    }
                }
            }, delay, 1000);
        }
    }

    /**
     * Stops current timer
     */
    public void stop() {
        isStarted = false;
        isWorking = false;
        if (handler != null) handler.purge();
    }

    public void stop(boolean resetAll) {
        stop();
        if (!resetAll) return;

        seconds = 0;
        minutes = 0;
        hours = 0;
    }
}
