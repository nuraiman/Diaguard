package com.faltenreich.diaguard.ui.fragment;

import android.os.Bundle;
import android.view.View;

import com.faltenreich.diaguard.R;
import com.faltenreich.diaguard.data.event.Events;
import com.faltenreich.diaguard.data.event.preference.CategoryOrderChangedEvent;
import com.faltenreich.diaguard.ui.list.adapter.CategoryListAdapter;
import com.faltenreich.diaguard.ui.list.helper.DragDropItemTouchHelperCallback;
import com.faltenreich.diaguard.data.PreferenceHelper;
import com.faltenreich.diaguard.data.entity.Measurement;
import com.faltenreich.diaguard.util.CategoryComparatorFactory;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;

public class CategoriesFragment extends BaseFragment implements CategoryListAdapter.ReorderListener {

    @BindView(R.id.listView) RecyclerView list;

    private CategoryListAdapter listAdapter;
    private ItemTouchHelper itemTouchHelper;
    private boolean hasChanged;

    public CategoriesFragment() {
        super(R.layout.fragment_categories, R.string.categories);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initLayout();
        setCategories();
    }

    @Override
    public void onDestroy() {
        if (hasChanged) {
            Events.post(new CategoryOrderChangedEvent());
        }
        super.onDestroy();
    }

    private void initLayout() {
        list.setLayoutManager(new LinearLayoutManager(getContext()));
        listAdapter = new CategoryListAdapter(getContext(), this);
        list.setAdapter(listAdapter);
        itemTouchHelper = new ItemTouchHelper(new DragDropItemTouchHelperCallback(listAdapter));
        itemTouchHelper.attachToRecyclerView(list);
    }

    private void setCategories() {
        listAdapter.addItems(PreferenceHelper.getInstance().getSortedCategories());
        listAdapter.notifyDataSetChanged();
    }

    @Override
    public void onReorderStart(RecyclerView.ViewHolder viewHolder) {
        itemTouchHelper.startDrag(viewHolder);
    }

    @Override
    public void onReorderEnd() {
        List<Measurement.Category> categories = listAdapter.getItems();
        for (int sortIndex = 0; sortIndex < categories.size(); sortIndex++) {
            PreferenceHelper.getInstance().setCategorySortIndex(categories.get(sortIndex), sortIndex);
        }
        CategoryComparatorFactory.getInstance().invalidate();
        hasChanged = true;
    }
}
