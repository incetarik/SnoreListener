package ridvan.snorelistener.objects;

import android.annotation.SuppressLint;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;

import ridvan.snorelistener.R;
import ridvan.snorelistener.helpers.Statistic;
import ridvan.snorelistener.helpers.Timer;

/**
 * Project: SnoreListener
 * <p>
 * Date: 26 Apr 2017
 * Author: Tarık İNCE <incetarik@hotmail.com>
 */
public class StatisticsAdapter extends RecyclerView.Adapter<StatisticsAdapter.StatisticsViewHolder> {
    private ArrayList<Statistic> statistics;
    private long                 maximumValue;

    public StatisticsAdapter() {
        statistics = new ArrayList<>();
    }

    public StatisticsAdapter(ArrayList<Statistic> statistics) {
        this.statistics = statistics;

        for (Statistic statistic : statistics) {
            if (statistic.getTotalSecondsSnored() > maximumValue) {
                maximumValue = statistic.getTotalSecondsSnored();
            }
        }

        for (Statistic statistic : statistics) {
            statistic.setPercentage(statistic.getTotalSecondsSnored() * 100.0 / maximumValue);
        }
    }

    public StatisticsAdapter addStatistic(Statistic statistic) {
        statistics.add(statistic);

        notifyItemInserted(statistics.size());

        return this;
    }

    public ArrayList<Statistic> getStatistics() {
        return statistics;
    }

    public void setStatistics(ArrayList<Statistic> statistics) {
        this.statistics = statistics;
        notifyDataSetChanged();
    }

    @Override
    public StatisticsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.li_statistic, parent, false);
        return new StatisticsViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(final StatisticsViewHolder holder, final int position) {
        final int adapterPos = holder.getAdapterPosition();

        holder.ivDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (adapterPos < 0 || adapterPos >= statistics.size()) return;
                statistics.remove(adapterPos);
                notifyItemRemoved(adapterPos);
            }
        });

        Statistic statistic = statistics.get(adapterPos);

        String[] totalTime = Timer.prettify(statistic.getTotalSecondsSnored()).split(":");
        int      hours     = Integer.parseInt(totalTime[0]);
        int      minutes   = Integer.parseInt(totalTime[1]);

        if (hours > 0) holder.tvTotalDuration.setText(totalTime[0] + "h");
        else if (minutes > 0) holder.tvTotalDuration.setText(totalTime[1] + "m");
        else holder.tvTotalDuration.setText(totalTime[2] + "s");

        holder.progressPercentage.setMax((int) maximumValue);
        holder.progressPercentage.setProgress((int) statistic.getTotalSecondsSnored());
    }

    @Override
    public int getItemCount() {
        return statistics.size();
    }

    class StatisticsViewHolder extends RecyclerView.ViewHolder {
        TextView    tvTotalDuration;
        ProgressBar progressPercentage;
        ImageView   ivDelete;

        public StatisticsViewHolder(View itemView) {
            super(itemView);

            tvTotalDuration = (TextView) itemView.findViewById(R.id.tvTotalDuration);
            progressPercentage = (ProgressBar) itemView.findViewById(R.id.progressPercentage);
            ivDelete = (ImageView) itemView.findViewById(R.id.ivDelete);
        }
    }
}