package com.faltenreich.diaguard.adapters.recycler;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.faltenreich.diaguard.R;
import com.faltenreich.diaguard.database.DatabaseFacade;
import com.faltenreich.diaguard.database.Entry;
import com.faltenreich.diaguard.database.measurements.BloodSugar;
import com.faltenreich.diaguard.database.measurements.Measurement;
import com.faltenreich.diaguard.helpers.PreferenceHelper;

import org.joda.time.DateTime;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Filip on 04.11.13.
 */
public class LogAdapter extends BaseAdapter<Measurement, RecyclerView.ViewHolder> {

    private static final int PAGE_SIZE = 20;

    private enum ViewType {
        SECTION,
        ENTRY,
        EMPTY
    }

    private Context context;
    private RecyclerView recyclerView;
    private DateTime currentDay;

    public LogAdapter(Context context, RecyclerView recyclerView) {
        this.context = context;
        this.items = new ArrayList<>();
        this.recyclerView = recyclerView;
        this.recyclerView.addOnScrollListener(new EndlessScrollListener((LinearLayoutManager) recyclerView.getLayoutManager()) {
            @Override
            public void onLoadMore(Direction direction) {
                appendRows(direction);
            }
        });
        this.currentDay = DateTime.now();
        appendRows(EndlessScrollListener.Direction.DOWN);
    }

    private void appendRows(EndlessScrollListener.Direction direction) {

        for (int additionalDay = 0; additionalDay < PAGE_SIZE; additionalDay++) {
            boolean addAtEnd = direction == EndlessScrollListener.Direction.DOWN;
            int listInsertPosition = addAtEnd && items.size() > 0 ? items.size() - 1 : 0;
            currentDay = addAtEnd? currentDay.minusDays(1) : currentDay.plusDays(1);

            items.add(listInsertPosition, new RecyclerEntry(currentDay, fetchData()));

            if (currentDay.dayOfMonth().get() == 1) {
                int monthInsertPosition = addAtEnd ? listInsertPosition += 1 : 0;
                items.add(monthInsertPosition, new RecyclerSection(currentDay.minusDays(1)));
            }
        }
        notifyDataSetChanged();
        if (direction == EndlessScrollListener.Direction.UP) {
            recyclerView.scrollToPosition(PAGE_SIZE);
        }
    }

    private List<Entry> fetchData() {
        List<Entry> entriesOfDay = DatabaseFacade.getInstance().getEntriesOfDay(currentDay);
        return entriesOfDay;
    }

    @Override
    public int getItemViewType(int position) {
        if (items.size() == 0) {
            return ViewType.EMPTY.ordinal();
        } else {
            RecyclerItem item = items.get(position);
            if (item instanceof RecyclerSection) {
                return ViewType.SECTION.ordinal();
            } else if (item instanceof RecyclerEntry) {
                return ViewType.ENTRY.ordinal();
            }
        }
        return super.getItemViewType(position);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewTypeInt) {
        ViewType viewType = ViewType.values()[viewTypeInt];
        switch (viewType) {
            case SECTION:
                return new ViewHolderMonth(LayoutInflater.from(context).inflate(R.layout.recycler_log_row_section, parent, false));
            case ENTRY:
                return new ViewHolderEntry(LayoutInflater.from(context).inflate(R.layout.recycler_log_row_entry, parent, false));
            default:
                return new ViewHolderEmpty(LayoutInflater.from(context).inflate(R.layout.listview_row_entry, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ViewType viewType = ViewType.values()[holder.getItemViewType()];
        switch (viewType) {
            case SECTION:
                bindMonth((ViewHolderMonth) holder, (RecyclerSection) items.get(position));
                break;
            case ENTRY:
                bindDay((ViewHolderEntry) holder, (RecyclerEntry) items.get(position));
                break;
        }
    }

    private void bindMonth(ViewHolderMonth vh, RecyclerSection recyclerSection) {
        vh.month.setText(recyclerSection.getDateTime().toString("MMM YYYY"));
    }

    private void bindDay(ViewHolderEntry vh, RecyclerEntry recyclerEntry) {
        vh.day.setText(recyclerEntry.getDay().toString("dd"));
        vh.weekDay.setText(recyclerEntry.getDay().dayOfWeek().getAsShortText());

        if (recyclerEntry.hasEntries()) {
            vh.entries.setVisibility(View.VISIBLE);
            vh.emptyView.setVisibility(View.GONE);
            LayoutInflater inflate = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            for (Entry entry : recyclerEntry.getEntries()) {
                View view = inflate.inflate(R.layout.recycler_log_entry, vh.entries, false);
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                    }
                });
                TextView time = (TextView) view.findViewById(R.id.time);
                time.setText(entry.getDate().toString("HH:mm"));
                ViewGroup layoutEntries = (ViewGroup) view.findViewById(R.id.measurements);
                try {
                    List<Measurement> measurements = DatabaseFacade.getInstance().getMeasurements(entry);
                    for (Measurement measurement : measurements) {
                        if (measurement instanceof BloodSugar) {
                            BloodSugar bloodSugar = (BloodSugar) measurement;
                            TextView textView = new TextView(context);
                            textView.setText(bloodSugar.getMgDl() + " " + PreferenceHelper.getInstance().getUnitName(Measurement.Category.BloodSugar));
                            layoutEntries.addView(textView);
                        }
                    }
                } catch (SQLException exception) {

                }
                vh.entries.addView(view);
            }
        } else {
            vh.entries.setVisibility(View.GONE);
            vh.emptyView.setVisibility(View.VISIBLE);
        }
    }

    private static class ViewHolderMonth extends RecyclerView.ViewHolder {
        TextView month;
        public ViewHolderMonth(View view) {
            super(view);
            this.month = (TextView) view.findViewById(R.id.month);
        }
    }

    private static class ViewHolderEntry extends RecyclerView.ViewHolder {
        TextView day;
        TextView weekDay;
        ViewGroup entries;
        TextView emptyView;
        public ViewHolderEntry(View view) {
            super(view);
            this.day = (TextView) view.findViewById(R.id.day);
            this.weekDay = (TextView) view.findViewById(R.id.weekday);
            this.entries = (ViewGroup) view.findViewById(R.id.entries);
            this.emptyView = (TextView) view.findViewById(R.id.empty_view);
        }
    }

    private static class ViewHolderEmpty extends RecyclerView.ViewHolder {
        public ViewHolderEmpty(View view) {
            super(view);
        }
    }
}