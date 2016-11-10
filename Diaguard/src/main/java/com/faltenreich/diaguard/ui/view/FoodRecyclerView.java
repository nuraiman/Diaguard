package com.faltenreich.diaguard.ui.view;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

import com.faltenreich.diaguard.adapter.EndlessRecyclerViewScrollListener;
import com.faltenreich.diaguard.adapter.FoodAdapter;
import com.faltenreich.diaguard.adapter.SimpleDividerItemDecoration;
import com.faltenreich.diaguard.adapter.list.ListItemFood;
import com.faltenreich.diaguard.data.dao.FoodDao;
import com.faltenreich.diaguard.data.entity.Food;
import com.faltenreich.diaguard.event.Events;
import com.faltenreich.diaguard.event.networking.FoodSearchFailedEvent;
import com.faltenreich.diaguard.event.networking.FoodSearchSucceededEvent;
import com.faltenreich.diaguard.networking.openfoodfacts.OpenFoodFactsManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Faltenreich on 10.11.2016.
 */

public class FoodRecyclerView extends RecyclerView {

    private FoodAdapter adapter;
    private String query;

    private int offlinePage;
    private int onlinePage;

    public FoodRecyclerView(Context context) {
        super(context);
        init();
    }

    public FoodRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Events.register(this);
        search(null);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Events.unregister(this);
    }

    private void init() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        setLayoutManager(layoutManager);

        addItemDecoration(new SimpleDividerItemDecoration(getContext()));

        adapter = new FoodAdapter(getContext());
        setAdapter(adapter);

        EndlessRecyclerViewScrollListener listener = new EndlessRecyclerViewScrollListener(layoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                searchOffline();
            }
        };
        addOnScrollListener(listener);
    }

    public void clear() {
        int oldCount = adapter.getItemCount();
        adapter.clear();
        adapter.notifyItemRangeRemoved(0, oldCount);
    }

    public void search(String query) {
        this.query = query;
        this.offlinePage = 0;
        this.onlinePage = 0;

        clear();
        searchOffline();
    }

    private void searchOffline() {
        List<Food> foodList = FoodDao.getInstance().search(query, offlinePage);
        if (foodList.size() > 0) {
            offlinePage++;
            addFood(foodList);
        } else {
            searchOnline();
        }
    }

    private void searchOnline() {
        OpenFoodFactsManager.getInstance().search(query, onlinePage);
    }

    private void addItems(List<ListItemFood> foodList) {
        if (foodList.size() > 0) {
            int oldSize = adapter.getItemCount();
            adapter.addItems(foodList);
            adapter.notifyItemRangeInserted(oldSize, oldSize + foodList.size());
        }
    }

    private void addFood(List<Food> foodList) {
        List<ListItemFood> foodItemList = new ArrayList<>();
        for (Food food : foodList) {
            foodItemList.add(new ListItemFood(food));
        }
        addItems(foodItemList);
    }

    @SuppressWarnings("unused")
    public void onEvent(FoodSearchSucceededEvent event) {
        onlinePage++;
        addFood(event.context);
    }

    @SuppressWarnings("unused")
    public void onEvent(FoodSearchFailedEvent event) {
        // TODO
    }
}
