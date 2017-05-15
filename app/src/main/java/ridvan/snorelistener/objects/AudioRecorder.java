package ridvan.snorelistener.objects;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.util.ArrayList;

import ridvan.snorelistener.helpers.SoundLevelListener;

public class AudioRecorder {
    public static final int    SAMPLE_RATE          = 44100;
    public static final double BASE_VALUE           = 6.8;
    public static final double DB_LEVEL_TO_VIBRATE  = 40;
    public static final int    MINIMUM_AUDIO_LENGTH = 5;
    public static final int    BUFFER_SIZE          = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

    private short[] buffer;
    private boolean isStarted;
    private boolean isListening;
    private int     latencyMillis;
    private long recordingStartDate = -1;

    private SoundLevelListener listener;
    private AudioRecord        audioRecord;
    private Thread             listenerThread;
    private ArrayList<Byte>    recordingBytes;

    public AudioRecorder() {
        init();
    }

    private void init() {
        recordingBytes = new ArrayList<>(8192);
        buffer = new short[BUFFER_SIZE];
        setLatencyMillis(10);
    }

    /**
     * Short array to byte array converter.
     * <p>
     * This code is taken from internet. Such converters could be found anywhere
     * and this converting is required for audio recorder.
     *
     * @param array Short array
     *
     * @return Byte array of given short array
     */
    public static byte[] short2byte(short[] array) {
        int    shortArrSize = array.length;
        byte[] bytes        = new byte[shortArrSize * 2];
        for (int i = 0; i < shortArrSize; i++) {
            bytes[i * 2] = (byte) (array[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (array[i] >> 8);
            array[i] = 0;
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

    public void stopListening() {
        setListening(false);
        if (audioRecord != null && isStarted()) {
            try {
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        setRecordingStartDate(-1);
        resetRecordingBytes();
        setStarted(false);
        init();

        if (listenerThread != null) {
            listenerThread.interrupt();
            listenerThread = null;
        }
    }

    public void resetRecordingBytes() {
        recordingBytes = new ArrayList<>();
    }

    public boolean isStarted() {
        return isStarted;
    }

    public void setStarted(boolean started) {
        isStarted = started;
    }

    public Thread startListening() {
        getOrCreateAudioRecord().startRecording();

        listenerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (isListening()) {
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
                        Thread.sleep(getLatencyMillis());
                    }
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        setListening(true);
        setStarted(true);
        listenerThread.start();
        return listenerThread;
    }

    public int getLatencyMillis() {
        return latencyMillis;
    }

    public void setLatencyMillis(int latencyMillis) {
        this.latencyMillis = latencyMillis;
    }

    public boolean isListening() {
        return isListening;
    }

    public void setListening(boolean listening) {
        isListening = listening;
        Log.d("AudioRecorder", "Listening state changed to " + listening);
    }

    private AudioRecord getOrCreateAudioRecord() {
        return (audioRecord == null ? (audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE)) : audioRecord);
    }
}
