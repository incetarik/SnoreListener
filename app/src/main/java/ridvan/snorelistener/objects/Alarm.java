package ridvan.snorelistener.objects;

import android.net.Uri;

import java.io.Serializable;
import java.util.Date;

public class Alarm implements Serializable {
    private Date    date;
    private String  title;
    private boolean isEnabled;
    private boolean repeatedWeekly;
    private boolean isVibrationEnabled;
    private Uri     ringtoneUri;

    public Alarm(Date date, String title, boolean isEnabled, boolean repeatedWeekly, boolean isVibrationEnabled, Uri ringtoneUri) {
        this.date = date;
        this.title = title;
        this.isEnabled = isEnabled;
        this.repeatedWeekly = repeatedWeekly;
        this.isVibrationEnabled = isVibrationEnabled;
        this.ringtoneUri = ringtoneUri;
    }

    public Alarm(Date date, String title, boolean isEnabled, boolean repeatedWeekly, boolean isVibrationEnabled) {
        this.date = date;
        this.title = title;
        this.isEnabled = isEnabled;
        this.repeatedWeekly = repeatedWeekly;
        this.isVibrationEnabled = isVibrationEnabled;
    }

    public Alarm(Date date, String title, boolean isEnabled, boolean repeatedWeekly) {

        this.date = date;
        this.title = title;
        this.isEnabled = isEnabled;
        this.repeatedWeekly = repeatedWeekly;
    }

    public Alarm(String title) {
        this();
        this.title = title;
    }

    public Alarm() {
        isEnabled = true;
    }

    public Alarm(Date date) {
        this();
        this.date = date;
    }

    public Alarm(Date date, String title) {
        this();
        this.date = date;
        this.title = title;
    }

    @Override
    public int hashCode() {
        int result = date != null ? date.hashCode() : 0;
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (isEnabled ? 1 : 0);
        result = 31 * result + (repeatedWeekly ? 1 : 0);
        result = 31 * result + (isVibrationEnabled ? 1 : 0);
        result = 31 * result + (ringtoneUri != null ? ringtoneUri.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Alarm alarm = (Alarm) o;

        if (isEnabled != alarm.isEnabled) return false;
        if (repeatedWeekly != alarm.repeatedWeekly) return false;
        if (isVibrationEnabled != alarm.isVibrationEnabled) return false;
        if (date != null ? !date.equals(alarm.date) : alarm.date != null) return false;
        if (title != null ? !title.equals(alarm.title) : alarm.title != null) return false;
        return ringtoneUri != null ? ringtoneUri.equals(alarm.ringtoneUri) : alarm.ringtoneUri == null;
    }

    public Uri getRingtoneUri() {
        return ringtoneUri;
    }

    public Alarm setRingtoneUri(Uri ringtoneUri) {
        this.ringtoneUri = ringtoneUri;
        return this;
    }

    public boolean isRepeatedWeekly() {

        return repeatedWeekly;
    }

    public Alarm setRepeatedWeekly(boolean repeatedWeekly) {
        this.repeatedWeekly = repeatedWeekly;
        return this;
    }

    public boolean isVibrationEnabled() {
        return isVibrationEnabled;
    }

    public Alarm setVibrationEnabled(boolean vibrationEnabled) {
        isVibrationEnabled = vibrationEnabled;
        return this;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public Alarm setEnabled(boolean enabled) {
        isEnabled = enabled;
        return this;
    }

    public Date getDate() {
        return date;
    }

    public Alarm setDate(Date date) {
        this.date = date;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public Alarm setTitle(String title) {
        this.title = title;
        return this;
    }
}
