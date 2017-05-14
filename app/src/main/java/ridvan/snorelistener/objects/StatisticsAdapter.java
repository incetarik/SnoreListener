package ridvan.snorelistener.objects;

import android.annotation.SuppressLint;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

import ridvan.snorelistener.R;
import ridvan.snorelistener.helpers.Timer;

public class StatisticsAdapter extends RecyclerView.Adapter<StatisticsAdapter.StatisticsViewHolder> {
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    private long                 maximumValue;
    private double               totalRecordedLength;
    private ArrayList<Statistic> statistics;

    public StatisticsAdapter() {
        statistics = new ArrayList<>();
    }

    public StatisticsAdapter(ArrayList<Statistic> statistics) {
        setStatistics(statistics);
    }

    public StatisticsAdapter addStatistic(Statistic statistic) {
        if (statistics.contains(statistic)) return this;
        statistics.add(statistic);

        if (maximumValue < statistic.getTotalSecondsSnored()) {
            maximumValue = statistic.getTotalSecondsSnored();
        }

        totalRecordedLength += statistic.getTotalSecondsSnored();
        notifyItemInserted(statistics.size());

        return this;
    }

    public ArrayList<Statistic> getStatistics() {
        return statistics;
    }

    public void setStatistics(ArrayList<Statistic> statistics) {
        this.statistics = statistics;

        for (Statistic statistic : statistics) {
            if (statistic.getTotalSecondsSnored() > maximumValue) {
                maximumValue = statistic.getTotalSecondsSnored();
            }

            totalRecordedLength += statistic.getTotalSecondsSnored();
        }

        for (Statistic statistic : statistics) {
            statistic.setPercentage(statistic.getTotalSecondsSnored() * 100.0 / maximumValue);
        }

        notifyDataSetChanged();
    }

    @Override
    public StatisticsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.li_statistic, parent, false);
        return new StatisticsViewHolder(view);
    }

    @SuppressLint({ "SetTextI18n", "DefaultLocale" })
    @Override
    public void onBindViewHolder(final StatisticsViewHolder holder, final int position) {
        final int       adapterPos = holder.getAdapterPosition();
        final Statistic statistic  = statistics.get(adapterPos);

        holder.ivDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (adapterPos < 0 || adapterPos >= statistics.size()) return;
                double length = statistics.remove(adapterPos).getTotalSecondsSnored();
                notifyItemRemoved(adapterPos);

                if (statistic.getTotalSecondsSnored() == maximumValue) {
                    maximumValue = getMaximumValue();
                }

                totalRecordedLength -= length;
                notifyDataSetChanged();
            }
        });

        String[] totalTime = Timer.prettify(statistic.getTotalSecondsSnored()).split(":");
        int      hours     = Integer.parseInt(totalTime[0]);
        int      minutes   = Integer.parseInt(totalTime[1]);

        if (hours > 0) holder.tvTotalDuration.setText(totalTime[0] + "h");
        else if (minutes > 0) holder.tvTotalDuration.setText(totalTime[1] + "m");
        else holder.tvTotalDuration.setText(totalTime[2] + "s");

        double percentageInAll = (statistic.getTotalSecondsSnored() * 100) / totalRecordedLength;
        holder.tvStartDate.setText(
                String.format(
                        "%s | %05.2f %% in Total",
                        dateFormat.format(statistic.getDateTime()),
                        percentageInAll
                )
        );

        holder.progressPercentage.setMax((int) maximumValue);
        holder.progressPercentage.setProgress((int) statistic.getTotalSecondsSnored());
        holder.tvPercentage.setText(String.format("%.2f %%", statistic.getTotalSecondsSnored() * 100.0 / maximumValue));
    }

    private long getMaximumValue() {
        long max = -1;
        for (Statistic statistic : statistics) {
            long totalSeconds = statistic.getTotalSecondsSnored();
            if (totalSeconds > max) {
                max = totalSeconds;
            }
        }

        return max;
    }

    @Override
    public int getItemCount() {
        return statistics.size();
    }

    class StatisticsViewHolder extends RecyclerView.ViewHolder {
        ImageView   ivDelete;
        TextView    tvStartDate;
        TextView    tvPercentage;
        TextView    tvTotalDuration;
        ProgressBar progressPercentage;

        public StatisticsViewHolder(View itemView) {
            super(itemView);

            ivDelete = (ImageView) itemView.findViewById(R.id.ivDelete);
            tvStartDate = (TextView) itemView.findViewById(R.id.tvStartDate);
            tvPercentage = (TextView) itemView.findViewById(R.id.tvPercentage);
            tvTotalDuration = (TextView) itemView.findViewById(R.id.tvTotalDuration);
            progressPercentage = (ProgressBar) itemView.findViewById(R.id.progressPercentage);
        }
    }
}
