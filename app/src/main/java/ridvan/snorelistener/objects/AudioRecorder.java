package ridvan.snorelistener.objects;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import java.util.ArrayList;

import ridvan.snorelistener.helpers.SoundLevelListener;

public class AudioRecorder {
    public static final int    SAMPLE_RATE          = 44100;
    public static final double BASE_VALUE           = 6.8;
    public static final double DB_LEVEL_TO_VIBRATE  = 15;
    public static final int    MINIMUM_AUDIO_LENGTH = 5;
    public static final int    BUFFER_SIZE          = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

    private ArrayList<Byte> recordingBytes;
    private long recordingStartDate = -1;
    private short[]            buffer;
    private SoundLevelListener listener;
    private AudioRecord        audioRecord;

    public AudioRecorder() {
        recordingBytes = new ArrayList<>(8192);
        buffer = new short[BUFFER_SIZE];
    }

    public static byte[] short2byte(short[] sData) {
        int    shortArrSize = sData.length;
        byte[] bytes        = new byte[shortArrSize * 2];
        for (int i = 0; i < shortArrSize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;

    }

    public short[] getBuffer() {
        return buffer;
    }

    public long getRecordingStartDate() {
        return recordingStartDate;
    }

    public void setRecordingStartDate(long recordingStartDate) {
        this.recordingStartDate = recordingStartDate;
    }

    public AudioRecord getAudioRecord() {
        return audioRecord;
    }

    public SoundLevelListener getListener() {
        return listener;
    }

    public void setListener(SoundLevelListener listener) {
        this.listener = listener;
    }

    public ArrayList<Byte> getRecordingBytes() {
        return recordingBytes;
    }

    public Thread startListening() {
        getOrCreateAudioRecord().startRecording();

        Thread listenerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        // Listen the record and save into buffer array
                        audioRecord.read(buffer, 0, BUFFER_SIZE);
                        int maxValue = 0;

                        // Get the maximum (loudest) voice as number
                        for (short number : buffer) maxValue = Math.max(maxValue, number);

                        // Convert into dB
                        double dB = 20 * Math.log10(maxValue / BASE_VALUE);

                        // Invoke listeners with measured dB
                        listener.onMeasure(dB);

                        // Sleep for next loop
                        Thread.sleep(80);
                    }
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        listenerThread.start();
        return listenerThread;
    }

    private AudioRecord getOrCreateAudioRecord() {
        return (audioRecord == null ? (audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE)) : audioRecord);
    }
}
