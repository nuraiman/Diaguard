package com.faltenreich.diaguard.ui.fragment;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.LayoutRes;
import android.support.annotation.StringRes;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.faltenreich.diaguard.DiaguardApplication;
import com.faltenreich.diaguard.R;
import com.faltenreich.diaguard.data.dao.EntryDao;
import com.faltenreich.diaguard.data.dao.MeasurementDao;
import com.faltenreich.diaguard.data.entity.Entry;
import com.faltenreich.diaguard.data.entity.Measurement;
import com.faltenreich.diaguard.event.Events;
import com.faltenreich.diaguard.event.data.EntryAddedEvent;
import com.faltenreich.diaguard.event.data.EntryDeletedEvent;
import com.faltenreich.diaguard.ui.activity.BaseActivity;
import com.faltenreich.diaguard.ui.activity.EntryActivity;
import com.faltenreich.diaguard.util.ViewHelper;

import butterknife.ButterKnife;

/**
 * Created by Filip on 26.06.2015.
 */
public abstract class BaseFragment extends Fragment {

    private int layoutResourceId;
    private String title;

    private BaseFragment() {
        // Forbidden
    }

    public BaseFragment(@LayoutRes int layoutResourceId, @StringRes int titleResourceId) {
        this();
        this.layoutResourceId = layoutResourceId;
        this.title = DiaguardApplication.getContext().getString(titleResourceId);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(layoutResourceId, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated (View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getActivity() instanceof BaseActivity) {
            View actionView = getActionView();
            if (actionView != null) {
                if (this instanceof ToolbarCallback) {
                    actionView.setVisibility(View.VISIBLE);
                    actionView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ((ToolbarCallback) BaseFragment.this).action();
                        }
                    });
                } else {
                    actionView.setVisibility(View.GONE);
                }
            }
        }
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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_newevent:
                startActivity(new Intent(getContext(), EntryActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public TextView getActionView() {
        return ((BaseActivity) getActivity()).getActionView();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        getActivity().setTitle(title);
    }

    public void setTitle(@StringRes int titleResId) {
        setTitle(getString(titleResId));
    }

    public void startActivity(Intent intent, ActivityOptionsCompat options) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            getActivity().startActivity(intent, options.toBundle());
        } else {
            startActivity(intent);
        }
    }

    protected void finish() {
        getActivity().finish();
    }

    interface ToolbarCallback {
        void action();
    }

    @CallSuper
    @SuppressWarnings("unused")
    public void onEvent(final EntryDeletedEvent event) {
        ViewHelper.showSnackbar(getView(), getString(R.string.entry_deleted), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Entry entry = event.context;
                EntryDao.getInstance().createOrUpdate(entry);
                for (Measurement measurement : entry.getMeasurementCache()) {
                    measurement.setEntry(entry);
                    MeasurementDao.getInstance(measurement.getClass()).createOrUpdate(measurement);
                }
                Events.post(new EntryAddedEvent(entry));
            }
        });
    }
}
