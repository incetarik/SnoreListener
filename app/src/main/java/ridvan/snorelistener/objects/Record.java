package ridvan.snorelistener.objects;

import android.os.Environment;

import java.util.Date;

/**
 * Project: SnoreListener
 * <p>
 * Date: 15 Apr 2017
 * Author: Tarık İNCE <incetarik@hotmail.com>
 */
public class Record {
    private String fileName;
    private Date   recordDate;
    private int    durationSeconds;
    private byte[] audioData;

    public Record() {
        setFileName("");
        setRecordDate(new Date());
    }

    public Record(String fileName, Date recordDate) {
        this(fileName, recordDate, 0);
    }

    public Record(String fileName, Date recordDate, int durationSeconds) {
        setFileName(fileName);
        setRecordDate(recordDate);
        setDurationSeconds(durationSeconds);
    }

    public Record(String fileName) {
        setFileName(fileName);
    }

    public byte[] getAudioData() {
        return audioData;
    }

    public void setAudioData(byte[] audioData) {
        this.audioData = audioData;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
        int index = fileName.indexOf("/record");
        if (index < 0)
            setFileName(String.format("%s/record-%d-%d.aud", Environment.getExternalStorageDirectory().getPath(), recordDate.getTime(), durationSeconds));
        else {
            String prefix = fileName.substring(0, index);
            setFileName(String.format("%s/record-%d-%d.aud", prefix, recordDate.getTime(), durationSeconds));
        }
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Date getRecordDate() {
        return recordDate;
    }

    public void setRecordDate(Date recordDate) {
        this.recordDate = recordDate;
        int index = fileName.indexOf("/record");
        if (index < 0)
            setFileName(String.format("%s/record-%d-%d.aud", Environment.getExternalStorageDirectory().getPath(), recordDate.getTime(), durationSeconds));
        else {
            String prefix = fileName.substring(0, index);
            setFileName(String.format("%s/record-%d-%d.aud", prefix, recordDate.getTime(), durationSeconds));
        }
    }
}
