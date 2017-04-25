package ridvan.snorelistener.objects;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import ridvan.snorelistener.R;
import ridvan.snorelistener.helpers.Timer;

public class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.RecordItemHolder> {
    private static final int PLAY_ID  = android.R.drawable.ic_media_play;
    private static final int PAUSE_ID = android.R.drawable.ic_media_pause;

    private ArrayList<Record> records;
    private RecyclerView      rv;

    private MediaPlayer player = new MediaPlayer();
    private AudioTrack track;
    private int lastPlayingPosition = -1;

    public RecordAdapter(final RecyclerView rv) {
        // Initializing audio record list
        records = new ArrayList<>();

        // RecyclerView reference to find a view by its position
        this.rv = rv;

        // Whenever playing an audio recorded before and it ends,
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                // If we have position information of lastly playing audio
                if (lastPlayingPosition != -1) {
                    // Get the view holder of the playing audio,
                    RecordItemHolder rih = ((RecordItemHolder) (rv.findViewHolderForAdapterPosition(lastPlayingPosition)));

                    // Set its play/pause button to play state
                    rih.ivPlayPause.setImageResource(PLAY_ID);

                    // Reset lastly playing recorded audio position to -1
                    lastPlayingPosition = -1;
                }

                // Release the sources from the memory
                player.release();

                // Re-initialize the player for the next
                player = new MediaPlayer();
            }
        });
    }

    public ArrayList<Record> getRecords() {
        return records;
    }

    public RecordAdapter addRecord(Record record) {
        // Add the record
        records.add(record);

        // Update and show the newly inserted record
        notifyItemInserted(getItemCount());

        // Return itself to allow chaining
        return this;
    }

    @Override
    public RecordItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.li_record, parent, false);
        return new RecordItemHolder(view);
    }

    @Override
    public void onBindViewHolder(final RecordItemHolder holder, int position) {
        // Get the related record of currently viewing holder by its position
        final Record record = records.get(holder.getAdapterPosition());

        // Set the information about the record
        holder.tvRecordDate.setText(record.getRecordDate().toString());
        holder.tvDuration.setText(Timer.prettify(record.getDurationSeconds()));

        // Whenever we click play/pause button
        holder.ivPlayPause.setOnClickListener(new View.OnClickListener() {
            // Keep an information about pause state
            private volatile boolean paused, playing;

            @Override
            public void onClick(View v) {
                if (playing) {
                    if (lastPlayingPosition == holder.getAdapterPosition()) {
                        // Pause currently playing audio and update information
                        track.pause();
                        paused = true;

                        // Set currently playing audio holder's play/pause button to play
                        holder.ivPlayPause.setImageResource(PLAY_ID);

                        return;
                    }
                    else if (lastPlayingPosition != -1) {
                        // We have previously playing audio and clicked another new one
                        // then get the old holder for previously playing audio
                        RecordItemHolder oldHolder = ((RecordItemHolder) (rv.findViewHolderForAdapterPosition(lastPlayingPosition)));

                        // Set its play/pause state to play
                        oldHolder.ivPlayPause.setImageResource(PLAY_ID);

                        // Stop the player of the previous audio and release sources
                        track.stop();
                        track.release();
                        track = null;
                        playing = false;
                    }
                }
                else if (paused) {
                    // Play the paused audio and update its play/pause icon and information
                    track.play();
                    //player.start();
                    holder.ivPlayPause.setImageResource(PAUSE_ID);
                    paused = false;
                    playing = true;

                    // ONLY!
                    return;
                }

                // Set currently playing audio holder's play/pause button to pause
                holder.ivPlayPause.setImageResource(PAUSE_ID);

                assignTrack(record);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        playing = true;
                        track.play();

                        track.write(record.getAudioData(), 0, record.getAudioData().length);
                        //track.release();

                        //track = null;
                        playing = false;
                    }
                }).start();

                rv.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        holder.ivPlayPause.setImageResource(PLAY_ID);
                    }
                }, (record.getAudioData().length / AudioRecorder.SAMPLE_RATE) * 1000);
            }
        });

        // Whenever we click delete button
        holder.ivDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the file recorded
                File file = new File(record.getFileName());

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

                records.remove(adapterPos);
                notifyItemRemoved(adapterPos);

                if (adapterPos != lastPlayingPosition) return;

                lastPlayingPosition = -1;

                if (player == null || !player.isPlaying()) return;

                player.stop();
                player.release();
                player = new MediaPlayer();
            }
        });
    }

    private void assignTrack(Record record) {
        if (track != null) return;

        File   file = new File(record.getFileName());
        byte[] data = new byte[(int) file.length()];

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            fis.read(data);
        }
        catch (IOException e) {
            e.printStackTrace();
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

        int size = AudioTrack.getMinBufferSize(AudioRecorder.SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        track = new AudioTrack(AudioManager.STREAM_MUSIC, AudioRecorder.SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, size, AudioTrack.MODE_STREAM);
        record.setAudioData(data);
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    class RecordItemHolder extends RecyclerView.ViewHolder {
        ImageView ivPlayPause;
        ImageView ivDelete;
        TextView  tvDuration;
        TextView  tvRecordDate;

        RecordItemHolder(View itemView) {
            super(itemView);

            ivPlayPause = (ImageView) itemView.findViewById(R.id.ivPlayPause);
            tvDuration = (TextView) itemView.findViewById(R.id.tvDuration);
            tvRecordDate = (TextView) itemView.findViewById(R.id.tvRecordedDate);
            ivDelete = (ImageView) itemView.findViewById(R.id.ivDelete);
        }
    }
}
