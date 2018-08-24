package com.faltenreich.diaguard.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import com.faltenreich.diaguard.R;
import com.faltenreich.diaguard.data.entity.Tag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TagAutoCompleteAdapter extends ArrayAdapter<Tag> {

    private HashMap<Tag, Boolean> tags;

    public TagAutoCompleteAdapter(@NonNull Context context) {
        super(context, -1);
        this.tags = new HashMap<>();
    }

    private List<Tag> getResults() {
        List<Tag> results = new ArrayList<>();
        for (Map.Entry<Tag, Boolean> entry : tags.entrySet()) {
            if (entry.getValue()) {
                results.add(entry.getKey());
            }
        }
        Collections.sort(results, new Comparator<Tag>() {
            @Override
            public int compare(Tag lhs, Tag rhs) {
                // Sort descending by updatedAt
                return rhs.getUpdatedAt().compareTo(lhs.getUpdatedAt());
            }
        });
        return results;
    }

    @Override
    public int getCount() {
        return getResults().size();
    }

    @Nullable
    @Override
    public Tag getItem(int position) {
        return getResults().get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View view, @NonNull ViewGroup parent) {
        if (view == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.list_item_tag, parent, false);
        }
        TextView characterView = view.findViewById(R.id.tag_character);
        TextView nameView = view.findViewById(R.id.tag_name);

        Tag tag = getItem(position);
        if (tag != null && !TextUtils.isEmpty(tag.getName())) {
            characterView.setText(tag.getName().substring(0, 1).toUpperCase());
            nameView.setText(tag.getName());
        }
        return view;
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults filterResults = new FilterResults();
                List<Tag> suggestions = new ArrayList<>();
                if (!TextUtils.isEmpty(constraint)) {
                    for (Map.Entry<Tag, Boolean> entry : tags.entrySet()) {
                        Tag tag = entry.getKey();
                        if (tag != null && tag.getName().toLowerCase().contains(constraint.toString().toLowerCase())) {
                            suggestions.add(tag);
                        }
                    }
                }
                filterResults.values = suggestions;
                filterResults.count = suggestions.size();
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults filterResults) {
                notifyDataSetChanged();
            }
        };
    }

    @Override
    public void add(@Nullable Tag tag) {
        set(tag, !tags.containsKey(tag) || tags.get(tag));
    }

    @Override
    public void addAll(@NonNull Collection<? extends Tag> collection) {
        for (Tag tag : collection) {
            add(tag);
        }
    }

    @Override
    public void addAll(Tag... items) {
        addAll(Arrays.asList(items));
    }

    public void set(Tag tag, boolean enable) {
        tags.put(tag, enable);
    }

    public Tag find(String name) {
        for (Tag tag : tags.keySet()) {
            if (tag.getName().equalsIgnoreCase(name)) {
                return tag;
            }
        }
        return null;
    }
}
