package com.faltenreich.diaguard.feature.food.detail;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.ViewPager;

import com.faltenreich.diaguard.R;
import com.faltenreich.diaguard.shared.data.database.entity.Food;
import com.faltenreich.diaguard.feature.food.BaseFoodFragment;
import com.faltenreich.diaguard.feature.entry.edit.EntryEditActivity;
import com.faltenreich.diaguard.feature.food.edit.FoodEditActivity;
import com.google.android.material.tabs.TabLayout;

import butterknife.BindView;

/**
 * Created by Faltenreich on 27.09.2016.
 */

public class FoodDetailFragment extends BaseFoodFragment {

    @BindView(R.id.food_viewpager) ViewPager viewPager;
    @BindView(R.id.food_tablayout) TabLayout tabLayout;

    public FoodDetailFragment() {
        super(R.layout.fragment_food_detail, R.string.food, -1, R.menu.food);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init();
        update();
    }

    @Override
    public void onResume() {
        super.onResume();
        update();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete:
                deleteFoodIfConfirmed();
                return true;
            case R.id.action_edit:
                editFood();
                return true;
            case R.id.action_eat:
                eatFood();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void init() {
        Food food = getFood();
        if (food != null) {
            FoodDetailViewPagerAdapter adapter = new FoodDetailViewPagerAdapter(getFragmentManager(), food);
            viewPager.setAdapter(adapter);
            tabLayout.setupWithViewPager(viewPager);
        }
    }

    private void update() {
        Food food = getFood();
        setTitle(food != null ? food.getName() : null);
    }

    private void eatFood() {
        EntryEditActivity.show(getContext(), getFood());
    }

    private void editFood() {
        Intent intent = new Intent(getActivity(), FoodEditActivity.class);
        intent.putExtra(BaseFoodFragment.EXTRA_FOOD_ID, getFood().getId());
        startActivity(intent);
    }
}
