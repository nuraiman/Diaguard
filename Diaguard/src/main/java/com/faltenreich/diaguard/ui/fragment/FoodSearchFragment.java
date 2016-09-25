package com.faltenreich.diaguard.ui.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;

import com.faltenreich.diaguard.R;
import com.faltenreich.diaguard.adapter.FoodAdapter;
import com.faltenreich.diaguard.adapter.SimpleDividerItemDecoration;
import com.faltenreich.diaguard.data.dao.FoodDao;
import com.faltenreich.diaguard.data.entity.Food;
import com.faltenreich.diaguard.event.Events;
import com.faltenreich.diaguard.event.networking.FoodSearchFailedEvent;
import com.faltenreich.diaguard.event.networking.FoodSearchSucceededEvent;
import com.faltenreich.diaguard.networking.openfoodfacts.OpenFoodFactsManager;
import com.faltenreich.diaguard.ui.activity.FoodSearchActivity;
import com.miguelcatalan.materialsearchview.MaterialSearchView;

import java.util.List;

import butterknife.BindView;

/**
 * Created by Faltenreich on 11.09.2016.
 */
public class FoodSearchFragment extends BaseFragment implements MaterialSearchView.OnQueryTextListener {

    public static final String EXTRA_MODE = "EXTRA_MODE";

    public enum Mode {
        READ,
        SELECT
    }

    @BindView(R.id.food_search_list) RecyclerView list;
    @BindView(R.id.search_view) MaterialSearchView searchView;

    private Mode mode;
    private FoodAdapter adapter;

    public FoodSearchFragment() {
        super(R.layout.fragment_food_search, R.string.food);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        this.mode = Mode.READ;
        checkIntents();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initList();
        initSearch();
    }

    @Override
    public void onResume() {
        super.onResume();
        Events.register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Events.unregister(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.food, menu);
        super.onCreateOptionsMenu(menu, inflater);
        searchView.setMenuItem(menu.findItem(R.id.action_search));
    }

    private void checkIntents() {
        if (getActivity() instanceof FoodSearchActivity && getActivity().getIntent() != null && getActivity().getIntent().getExtras() != null) {
            Bundle extras = getActivity().getIntent().getExtras();
            if (extras.get(EXTRA_MODE) != null && extras.get(EXTRA_MODE) instanceof Mode) {
                this.mode = (Mode) extras.get(EXTRA_MODE);
            }
        }
    }

    private void initList() {
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        list.setLayoutManager(layoutManager);
        list.addItemDecoration(new SimpleDividerItemDecoration(getContext()));
        adapter = new FoodAdapter(getContext());
        list.setAdapter(adapter);
        updateList(FoodDao.getInstance().getAll());
    }

    private void initSearch() {
        searchView.setVoiceSearch(true);
        searchView.setOnQueryTextListener(this);
        searchView.clearFocus();
        List<Food> foodList = FoodDao.getInstance().getAll();
        String[] suggestions = new String[foodList.size()];
        for (int position = 0; position < foodList.size(); position++) {
            Food food = foodList.get(position);
            suggestions[position] = food.getName();
        }
        searchView.setSuggestions(suggestions);
    }

    private void updateList(List<Food> foodList) {
        adapter.clear();
        adapter.addItems(foodList);
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        OpenFoodFactsManager.getInstance().search(query);
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    @SuppressWarnings("unused")
    public void onEvent(FoodSearchSucceededEvent event) {
        updateList(event.context);
    }

    @SuppressWarnings("unused")
    public void onEvent(FoodSearchFailedEvent event) {

    }
}