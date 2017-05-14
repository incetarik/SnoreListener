package ridvan.snorelistener.objects;

import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import ridvan.snorelistener.R;
import ridvan.snorelistener.helpers.Action;
import ridvan.snorelistener.helpers.Timer;

public class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.RecordItemHolder> {
    private static final int PLAY_ID  = android.R.drawable.ic_media_play;
    private static final int PAUSE_ID = android.R.drawable.ic_media_pause;

    private int lastPlayingPosition        = -1;
    private int currentPlayingBytePosition = -1;

    private RecyclerView      rv;
    private AudioTrack        track;
    private ArrayList<Record> records;
    private Action<Boolean>   stateAction;

    public RecordAdapter(final RecyclerView rv, Action<Boolean> stateAction) {
        // Initializing audio record list
        records = new ArrayList<>();

        // RecyclerView reference to find a view by its position
        this.rv = rv;
        this.stateAction = stateAction;
    }

    public RecordAdapter addRecord(Record record) {
        // Add the record
        records.add(record);

        // Update and show the newly inserted record
        notifyItemInserted(getItemCount());
        notifyDataSetChanged();

        // Return itself to allow chaining
        return this;
    }

    public RecordAdapter removeRecord(Record record) {
        int index = records.indexOf(record);
        if (index < 0) return this;

        records.remove(index);

        notifyItemRemoved(index);
        notifyDataSetChanged();
        removeStatistic(record);

        // Return itself to allow chaining
        return this;
    }

    private void removeStatistic(Record record) {
        Log.d("RecordAdapter", String.format("Removing statistic for Record(%s)", record.getFileName()));

        ArrayList<Statistic> statistics = Statistic.getStatistics();
        if (statistics == null) return;

        for (int i = statistics.size() - 1; i >= 0; i--) {
            Statistic statistic = statistics.get(i);
            if (statistic.getRecordId() == record.getRecordDate().getTime()) {
                statistics.remove(i);
                break;
            }
        }
    }

    @Override
    public RecordItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.li_record, parent, false);
        return new RecordItemHolder(view);
    }

    @Override
    public void onBindViewHolder(final RecordItemHolder holder, int position) {
        // Get the related record of currently viewing holder by its position
        final Record  record    = records.get(holder.getAdapterPosition());
        final boolean isPlaying = lastPlayingPosition == holder.getAdapterPosition() && currentPlayingBytePosition >= 0;

        // Set the information about the record
        holder.tvRecordDate.setText(record.getRecordDate().toString());
        holder.tvDuration.setText(Timer.prettify((long) record.getDurationSeconds()));
        holder.ivPlayPause.setImageResource(isPlaying ? PAUSE_ID : PLAY_ID);
        holder.tvDuration.setTextColor(isPlaying ? Color.RED : Color.BLACK);

        // Whenever we click play/pause button
        holder.ivPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (isPlaying()) {
                    if (lastPlayingPosition == holder.getAdapterPosition()) {
                        // Pause currently playing audio and update information
                        track.pause();

                        // Set currently playing audio holder's play/pause button to play
                        holder.ivPlayPause.setImageResource(PLAY_ID);

                        if (stateAction != null) stateAction.call(false);

                        return;
                    }
                    else if (lastPlayingPosition >= 0) {
                        // We have previously playing audio and clicked another new one
                        // then get the old holder for previously playing audio
                        RecordItemHolder oldHolder = ((RecordItemHolder) (rv.findViewHolderForAdapterPosition(lastPlayingPosition)));

                        // Set its play/pause state to play
                        // NOTE: If old played placeholder is out of the screen, oldHolder will
                        // return null, and also when it becomes re-visible, then its play state
                        // will automatically set by above (holder.ivPlayPause.setImageResource)
                        if (oldHolder != null) {
                            oldHolder.ivPlayPause.setImageResource(PLAY_ID);
                        }

                        // Stop the player of the previous audio and release sources
                        track.stop();
                        track.release();
                        track = null;

                        if (stateAction != null) stateAction.call(true);

                        currentPlayingBytePosition = -1;
                    }
                }
                else if (isPaused()) {
                    // Play the paused audio and update its play/pause icon and information
                    holder.ivPlayPause.setImageResource(PAUSE_ID);
                    playTrack(record);

                    if (stateAction != null) stateAction.call(true);

                    // ONLY!
                    return;
                }

                // Update last playing position
                lastPlayingPosition = holder.getAdapterPosition();

                // If we can set audio data successfully,
                if (!tryAssignTrack(holder.getAdapterPosition(), record)) return;

                // then set play/pause icon to pause
                holder.ivPlayPause.setImageResource(PAUSE_ID);

                if (stateAction != null) stateAction.call(true);

                // play the track, and when it ends
                playTrack(record, new Runnable() {
                    @Override
                    public void run() {
                        // Get current holder which started audio
                        RecordItemHolder currentHolder = (RecordItemHolder) rv.findViewHolderForAdapterPosition(lastPlayingPosition);

                        //  if it is on the screen, set play/pause icon to play
                        if (currentHolder != null)
                            currentHolder.ivPlayPause.setImageResource(PLAY_ID);
                        if (stateAction != null) stateAction.call(false);
                    }
                });
            }
        });

        // Whenever we click delete button
        holder.ivDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String fileName = record.getFileName();

                // Get the file recorded
                File file = new File(fileName);

                if (!file.exists() && fileName.contains(".00.")) {
                    fileName = fileName.replace(".00.", ".");
                    file = new File(fileName);
                }

                if (file.exists()) {
                    // then delete it
                    boolean isDeleted = file.delete();

                    // show info whether it is deleted successfully or not
                    Toast.makeText(rv.getContext(), isDeleted ? "Successfully deleted!" : "Failed to delete", 0).show();

                    if (!isDeleted) return;
                }
                else {
                    // show info that file was not found
                    Toast.makeText(rv.getContext(), "File was not found!", 1).show();
                }

                // remove the item and update information
                int adapterPos = holder.getAdapterPosition();
                if (adapterPos < 0) return;

                removeRecord(record);

                if (adapterPos < lastPlayingPosition) lastPlayingPosition--;
                if (adapterPos != lastPlayingPosition) return;

                try {
                    track.stop();
                    track.release();

                    track = null;
                }
                catch (Exception e) {
                    // ignored
                }

                lastPlayingPosition = -1;
                currentPlayingBytePosition = -1;
            }
        });
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    private void playTrack(final Record record) {
        playTrack(record, null);
    }

    /**
     * Plays the track of given record
     *
     * @param record         Record to get track from
     * @param onUiOnTrackEnd Runnable for the action after the playing ends, may be null
     */
    private void playTrack(final Record record, final Runnable onUiOnTrackEnd) {
        // If we have started before, (then we have paused and re-playing)
        if (currentPlayingBytePosition >= 0) {
            // Continue listening new output bytes...
            track.play();

            // Nothing to do anymore
            return;
        }

        // Prepare a non-blocking output stream (as Thread)
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[]               buffer              = new byte[AudioRecorder.BUFFER_SIZE];
                    byte[]               currentPlayingAudio = record.getAudioData();
                    ByteArrayInputStream in                  = new ByteArrayInputStream(currentPlayingAudio);

                    // Open output channel (listen will-come output audio bytes)
                    track.play();

                    // While current offset-position is not reached to the end of the audio
                    while (currentPlayingBytePosition < currentPlayingAudio.length) {

                        // But if it is paused
                        if (isPaused()) {
                            // Wait 20 millis
                            Thread.sleep(20);
                            continue;
                        }

                        // If it was being reading but interrupted, return from here
                        if (isStopped()) return;

                        // Read to from audio to the buffer
                        int read = in.read(buffer, 0, buffer.length);

                        // If it is not the end
                        if (read != -1) {
                            // Send new bytes to the output channel
                            track.write(buffer, 0, buffer.length);

                            // Update current position
                            currentPlayingBytePosition += read;
                        }
                        else {
                            // At the end, update current position to -1
                            currentPlayingBytePosition = -1;
                            break;
                        }
                    }

                    // Reading and sending is done, flush and close the channels.
                    in.close();
                    track.flush();
                    track.stop();
                    track = null;

                    // For memory recover, set current record audio data to null, it will be
                    // re-initialized on re-click
                    record.setAudioData(null);

                    // Invoke if there is track-ended runnable
                    if (onUiOnTrackEnd != null) rv.post(onUiOnTrackEnd);
                }
                catch (InterruptedException e) {
                    // ignored
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * Indicates whether current audio exists, and is playing
     *
     * @return True if current audio is playing
     */
    private boolean isPlaying() {
        return track != null && track.getPlayState() == AudioTrack.PLAYSTATE_PLAYING;
    }

    /**
     * Indicates whether currently playing audio exists, and is started
     *
     * @return True if any record is started, even if it is started and paused
     */
    private boolean isStarted() {
        return track != null && track.getPlayState() != AudioTrack.PLAYSTATE_STOPPED;
    }

    /**
     * Indicates whether current playing audio is stopped or not exists
     *
     * @return True if there is no playing audio
     */
    private boolean isStopped() {
        return track == null || track.getPlayState() == AudioTrack.PLAYSTATE_STOPPED;
    }

    /**
     * Indicates whether currently playing audio is paused
     *
     * @return True if currently playing audio exists, and paused
     */
    private boolean isPaused() {
        return track != null && track.getPlayState() == AudioTrack.PLAYSTATE_PAUSED;
    }

    /**
     * Prepares 'track' from given record parameter.
     *
     * @param adapterPosition Adapter position of holder, in case removing is needed
     * @param record          Record to assign track from.
     *
     * @return True if successfully assigned
     */
    private boolean tryAssignTrack(final int adapterPosition, Record record) {
        if (track != null) return true;

        String fileName = record.getFileName();
        File   file     = new File(record.getFileName());

        if (!file.exists() && fileName.contains(".00.")) {
            fileName = fileName.replace(".00.", ".");
            file = new File(fileName);
        }

        byte[]  data      = new byte[(int) file.length()];
        boolean canAssign = true;

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            fis.read(data);
        }
        catch (FileNotFoundException e) {
            // File not found, remove the record and update the list
            getRecords().remove(adapterPosition);
            notifyItemRemoved(adapterPosition);

            // Notify user
            Toast.makeText(rv.getContext(), "File not found, record is removed", 0).show();
            canAssign = false;
        }
        catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(rv.getContext(), "I/O Exception, please be sure permissions are granted", 1).show();
            canAssign = false;
        }
        finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
                canAssign = false;
            }
        }

        if (!canAssign) return false;

        int size = AudioTrack.getMinBufferSize(AudioRecorder.SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        track = new AudioTrack(AudioManager.STREAM_MUSIC, AudioRecorder.SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, size, AudioTrack.MODE_STREAM);
        record.setAudioData(data);
        return true;
    }

    public ArrayList<Record> getRecords() {
        return records;
    }

    class RecordItemHolder extends RecyclerView.ViewHolder {
        ImageView ivDelete;
        TextView  tvDuration;
        ImageView ivPlayPause;
        TextView  tvRecordDate;

        RecordItemHolder(View itemView) {
            super(itemView);

            ivDelete = (ImageView) itemView.findViewById(R.id.ivDelete);
            tvDuration = (TextView) itemView.findViewById(R.id.tvDuration);
            ivPlayPause = (ImageView) itemView.findViewById(R.id.ivPlayPause);
            tvRecordDate = (TextView) itemView.findViewById(R.id.tvRecordedDate);
        }
    }
}
