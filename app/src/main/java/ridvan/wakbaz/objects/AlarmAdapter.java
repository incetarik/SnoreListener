package ridvan.wakbaz.objects;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.database.Cursor;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.*;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;
import ridvan.wakbaz.R;
import ridvan.wakbaz.helpers.Action;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class AlarmAdapter extends RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder> {
    private RecyclerView    rvAlarms;
    private FragmentManager fragmentManager;

    public AlarmAdapter(RecyclerView rvAlarms, FragmentManager fragmentManager) {
        this.rvAlarms = rvAlarms;
        this.fragmentManager = fragmentManager;

        Action<Alarm> onChangeAction = new Action<Alarm>() {
            @Override
            public void call(Alarm obj) {
                notifyDataSetChanged();
            }
        };

        AlarmManager.registerListener(onChangeAction, AlarmManager.AlarmEvent.FINISHED);
        AlarmManager.registerListener(onChangeAction, AlarmManager.AlarmEvent.ADDED);
        AlarmManager.registerListener(onChangeAction, AlarmManager.AlarmEvent.UPDATED);
    }

    @Override
    public AlarmViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(viewType == 0 ? R.layout.li_alarm_adder : R.layout.li_alarm, parent, false);
        return new AlarmViewHolder(view, viewType == 0);
    }

    @Override
    public void onBindViewHolder(final AlarmViewHolder holder, final int position) {
        if (position == 0) {
            holder.tvAddAlarm.setOnClickListener(new View.OnClickListener() {
                private Ringtone lastPlayed;
                private Calendar selectedDateCalendar;

                private void stopRingtone() {
                    if (lastPlayed != null && lastPlayed.isPlaying()) lastPlayed.stop();

                    Ringtone playingRingtone = AlarmManager.getCurrentRingtone();
                    if (playingRingtone == null || !playingRingtone.isPlaying()) return;

                    playingRingtone.stop();
                }

                private void askDateTimeFor(final EditText etDateTime) {
                    final Calendar now = Calendar.getInstance();
                    final DatePickerDialog dpd = DatePickerDialog.newInstance(new DatePickerDialog.OnDateSetListener() {

                        @Override
                        public void onDateSet(DatePickerDialog view, final int year, final int monthOfYear, final int dayOfMonth) {
                            TimePickerDialog tpd = TimePickerDialog.newInstance(new TimePickerDialog.OnTimeSetListener() {

                                @SuppressLint("DefaultLocale")
                                @Override
                                public void onTimeSet(TimePickerDialog view, int hourOfDay, int minute, int second) {
                                    String hodStr = String.valueOf(hourOfDay);
                                    if (hourOfDay < 10) hodStr = "0" + hodStr;

                                    String minStr = String.valueOf(minute);
                                    if (minute < 10) minStr = "0" + minute;

                                    selectedDateCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                    selectedDateCalendar.set(Calendar.MINUTE, minute);
                                    selectedDateCalendar.set(Calendar.SECOND, 0);

                                    etDateTime.setText(String.format("%d/%d/%d %s:%s",
                                                                     dayOfMonth,
                                                                     monthOfYear,
                                                                     year,
                                                                     hodStr,
                                                                     minStr));
                                }

                            }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true);

                            selectedDateCalendar = Calendar.getInstance();
                            selectedDateCalendar.set(Calendar.YEAR, year);
                            selectedDateCalendar.set(Calendar.MONTH, monthOfYear);
                            selectedDateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                            tpd.show(fragmentManager, "Select Time");
                        }

                    }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));

                    dpd.autoDismiss(true);
                    dpd.show(fragmentManager, "Select Date");
                }

                @Override
                public void onClick(View v) {
                    AlertDialog.Builder builder          = new AlertDialog.Builder(rvAlarms.getContext());
                    View                adderView        = LayoutInflater.from(rvAlarms.getContext()).inflate(R.layout.alarm_adder, null);
                    final EditText      etTitle          = (EditText) adderView.findViewById(R.id.etTitle);
                    final EditText      etDateTime       = (EditText) adderView.findViewById(R.id.etDateTime);
                    final CheckBox      cbRepeatedWeekly = (CheckBox) adderView.findViewById(R.id.cbRepeatedWeekly);
                    final CheckBox      cbVibrate        = (CheckBox) adderView.findViewById(R.id.cbVibrate);
                    final Spinner       spinRingtones    = (Spinner) adderView.findViewById(R.id.spinRingtones);

                    final Alarm               alarm     = new Alarm();
                    final Map<String, String> ringtones = getAvailableRingtones();
                    final ArrayList<String>   values    = new ArrayList<>(ringtones.values());

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(rvAlarms.getContext(), android.R.layout.simple_list_item_1);
                    adapter.addAll(ringtones.keySet());
                    spinRingtones.setAdapter(adapter);

                    spinRingtones.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        private boolean initialized;

                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            alarm.setRingtoneUri(Uri.parse(values.get(position)));

                            if (initialized) {
                                stopRingtone();
                                lastPlayed = RingtoneManager.getRingtone(rvAlarms.getContext(), Uri.parse(values.get(position)));
                                lastPlayed.play();
                            }

                            initialized = true;
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {

                        }
                    });

                    cbRepeatedWeekly.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            stopRingtone();
                            alarm.setRepeatedWeekly(isChecked);
                        }
                    });

                    cbVibrate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            stopRingtone();
                            alarm.setVibrationEnabled(isChecked);
                        }
                    });

                    etTitle.setText(String.format("Alarm %d", AlarmManager.getAlarmCount() + 1));

                    etDateTime.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            stopRingtone();
                            askDateTimeFor(etDateTime);
                        }
                    });

                    builder.setView(adderView).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            etTitle.setError(null);
                            etDateTime.setError(null);

                            if (etTitle.getText().toString().isEmpty()) {
                                etTitle.setError("Cannot be empty");
                                return;
                            }

                            if (etDateTime.getText().toString().isEmpty()) {
                                etDateTime.setError("Cannot be empty");
                            }
                            else {
                                stopRingtone();
                                dialog.dismiss();

                                alarm.setTitle(etTitle.getText().toString());
                                alarm.setDate(selectedDateCalendar.getTime());

                                AlarmManager.addAlarm(alarm);
                            }
                        }
                    }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            stopRingtone();
                        }
                    }).show().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
                }
            });
        }
        else {
            final Alarm alarm = AlarmManager.getAlarmAt(position);

            holder.ivAlarm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean isEnabled = alarm.isEnabled();

                    alarm.setEnabled(!isEnabled);

                    holder.ivAlarm.setImageDrawable(
                            ContextCompat.getDrawable(
                                    rvAlarms.getContext(),
                                    (isEnabled ? R.drawable.alarm_normal : R.drawable.alarm_disabled)));
                }
            });

            holder.ivDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = holder.getAdapterPosition();
                    if (pos < 0) return;

                    notifyItemRemoved(pos);
                    AlarmManager.removeAlarm(pos);
                }
            });

            holder.tvDate.setText(alarm.getDate().toString());
            holder.tvTitle.setText(alarm.getTitle());
        }
    }

    public Map<String, String> getAvailableRingtones() {
        RingtoneManager manager = new RingtoneManager(rvAlarms.getContext());
        manager.setType(RingtoneManager.TYPE_ALARM);

        Cursor cursor = manager.getCursor();

        Map<String, String> map = new HashMap<>();
        while (cursor.moveToNext()) {
            String title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX);
            String uri   = cursor.getString(RingtoneManager.URI_COLUMN_INDEX);
            String id    = cursor.getString(RingtoneManager.ID_COLUMN_INDEX);

            map.put(title, uri + "/" + id);
        }

        return map;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return AlarmManager.getAlarmCount() + 1;
    }

    class AlarmViewHolder extends RecyclerView.ViewHolder {
        TextView  tvDate;
        ImageView ivAlarm;
        TextView  tvTitle;
        ImageView ivDelete;
        TextView  tvAddAlarm;

        public AlarmViewHolder(View itemView, boolean first) {
            super(itemView);

            if (first) {
                tvAddAlarm = (TextView) itemView.findViewById(R.id.tvAddAlarm);
                return;
            }

            tvDate = (TextView) itemView.findViewById(R.id.tvDate);
            ivAlarm = (ImageView) itemView.findViewById(R.id.ivAlarm);
            tvTitle = (TextView) itemView.findViewById(R.id.tvTitle);
            ivDelete = (ImageView) itemView.findViewById(R.id.ivDelete);
        }
    }
}
