package ridvan.snorelistener.objects;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.util.SparseArray;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class Statistic implements Serializable {
    private static final String STATISTICS_FILE = "Statistics";
    private static ArrayList<Statistic> statistics;
    private        long                 recordId;
    private        double               percentage;
    private        long                 totalSecondsSnored;
    private        Date                 dateTime;

    public Statistic() {
    }

    public Statistic(long recordId, Date dateTime, long totalSecondsSnored, double percentage) {
        this(dateTime, totalSecondsSnored, percentage);
        this.recordId = recordId;
    }

    public Statistic(Date dateTime, long totalSecondsSnored, double percentage) {
        this(dateTime, totalSecondsSnored);
        this.percentage = percentage;
    }

    public Statistic(Date dateTime, long totalSecondsSnored) {
        this(dateTime);
        this.totalSecondsSnored = totalSecondsSnored;
    }

    public Statistic(Date dateTime) {
        this.dateTime = dateTime;
    }

    public Statistic(long recordId, Date dateTime, long totalSecondsSnored) {
        this(recordId, dateTime);
        this.totalSecondsSnored = totalSecondsSnored;
    }

    public Statistic(long recordId, Date dateTime) {
        this.recordId = recordId;
        this.dateTime = dateTime;
    }

    public static ArrayList<Statistic> getStatistics() {
        return statistics;
    }

    /**
     * Groups statistics by given calendar field type
     *
     * @param statistics Statistics to group
     * @param type       Calendar.{FIELD_TYPE}
     *
     * @return Grouped statistics by their Calendar.{FIELD_TYPE}'s
     */
    public static SparseArray<ArrayList<Statistic>> groupStatistics(ArrayList<Statistic> statistics, final int type) {
        if (statistics.isEmpty()) return new SparseArray<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(statistics.get(0).getDateTime());

        SparseArray<ArrayList<Statistic>> groupedStatistics = new SparseArray<>();

        for (int i = 1; i < statistics.size(); i++) {
            Statistic            statistic         = statistics.get(i);
            ArrayList<Statistic> currentStatistics = groupedStatistics.get(calendar.get(type));

            if (currentStatistics == null) {
                currentStatistics = new ArrayList<>();
                groupedStatistics.append(type, currentStatistics);
            }

            currentStatistics.add(statistic);
        }

        return groupedStatistics;
    }

    public Date getDateTime() {
        return dateTime;
    }

    public Statistic setDateTime(Date dateTime) {
        this.dateTime = dateTime;
        return this;
    }

    public static boolean saveStatistics(Context context) {
        return saveStatistics(context, statistics);
    }

    /**
     * Saves given statistics list to the storage by given context
     *
     * @param context    Context to save list
     * @param statistics List to save
     *
     * @return true if successfully saved
     */
    public static boolean saveStatistics(Context context, ArrayList<Statistic> statistics) {
        Log.d("Statistic", "Statistics are saving...");
        FileOutputStream fos    = null;
        boolean          retVal = false;

        try {
            fos = context.openFileOutput(STATISTICS_FILE, Context.MODE_PRIVATE);
            FileWriter fw = new FileWriter(fos.getFD());

            for (Statistic statistic : statistics) {
                fw.write(statistic.toString() + "\n");
            }

            fw.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            try {
                if (fos != null) {
                    fos.close();
                    retVal = true;
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        return retVal;
    }

    @SuppressLint("DefaultLocale")
    @Override
    public String toString() {
        return String.format("%d¬%d¬%f", dateTime.getTime(), totalSecondsSnored, percentage);
    }

    /**
     * Loads statistics from storage, if file exists
     *
     * @param context Context to open input stream
     *
     * @return Pre-saved statistics, even not saved before, will return empty array
     */
    public static ArrayList<Statistic> loadStatistics(Context context) {
        Log.d("Statistic", "Loading statistics...");
        FileInputStream      fis        = null;
        ArrayList<Statistic> statistics = new ArrayList<>();

        try {
            fis = context.openFileInput(STATISTICS_FILE);
            FileReader     fr = new FileReader(fis.getFD());
            BufferedReader br = new BufferedReader(fr);

            String line;
            while ((line = br.readLine()) != null) {
                if (line.isEmpty()) continue;
                String[] parts = line.split("¬");

                if (parts.length != 3) continue;

                statistics.add(
                        new Statistic(new Date(Long.parseLong(parts[0])))
                                .setTotalSecondsSnored(Long.parseLong(parts[1]))
                                .setPercentage(Double.parseDouble(parts[2])));
            }
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

        Statistic.statistics = statistics;
        return statistics;
    }

    public long getRecordId() {
        return recordId;
    }

    public void setRecordId(long recordId) {
        this.recordId = recordId;
    }

    public long getTotalSecondsSnored() {
        return totalSecondsSnored;
    }

    public Statistic setTotalSecondsSnored(long totalSecondsSnored) {
        this.totalSecondsSnored = totalSecondsSnored;
        return this;
    }

    public double getPercentage() {
        return percentage;
    }

    public Statistic setPercentage(double percentage) {
        this.percentage = percentage;
        return this;
    }
}
