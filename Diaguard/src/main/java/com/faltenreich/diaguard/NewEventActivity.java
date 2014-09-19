package com.faltenreich.diaguard;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;

import com.faltenreich.diaguard.database.DatabaseDataSource;
import com.faltenreich.diaguard.database.DatabaseHelper;
import com.faltenreich.diaguard.database.Entry;
import com.faltenreich.diaguard.database.Measurement;
import com.faltenreich.diaguard.database.Model;
import com.faltenreich.diaguard.fragments.DatePickerFragment;
import com.faltenreich.diaguard.fragments.TimePickerFragment;
import com.faltenreich.diaguard.helpers.Helper;
import com.faltenreich.diaguard.helpers.PreferenceHelper;
import com.faltenreich.diaguard.helpers.Validator;
import com.faltenreich.diaguard.helpers.ViewHelper;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Filip on 19.10.13.
 */
public class NewEventActivity extends ActionBarActivity {

    public static final String EXTRA_ENTRY = "Entry";
    public static final String EXTRA_MEASUREMENT = "Measurement";
    public static final String EXTRA_DATE = "Date";

    DatabaseDataSource dataSource;
    PreferenceHelper preferenceHelper;

    Entry entry;

    DateTime time;

    LinearLayout linearLayoutValues;
    EditText editTextNotes;
    Button buttonDate;
    Button buttonTime;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_newevent);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle(getString(R.string.entry_new));
        initialize();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.formular, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Show delete button only if an entry is available = editing mode
        menu.findItem(R.id.action_delete).setVisible(entry != null);
        return true;
    }

    public void initialize() {
        dataSource = new DatabaseDataSource(this);
        preferenceHelper = new PreferenceHelper(this);
        time = new DateTime();

        getComponents();
        setCategories();
        checkIntents();
        setDate();
        setTime();
    }

    public void getComponents() {
        linearLayoutValues = (LinearLayout) findViewById(R.id.content_dynamic);
        editTextNotes = (EditText) findViewById(R.id.edittext_notes);
        buttonDate = (Button) findViewById(R.id.button_date);
        buttonTime = (Button) findViewById(R.id.button_time);
    }

    private void checkIntents() {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            if(extras.getLong(EXTRA_ENTRY) != 0L || extras.getLong(EXTRA_MEASUREMENT) != 0L) {
                setTitle(getString(R.string.entry_edit));

                dataSource.open();

                // Get entry
                if(extras.getLong(EXTRA_ENTRY) != 0L) {
                    entry = (Entry) dataSource.get(DatabaseHelper.ENTRY, extras.getLong(EXTRA_ENTRY));
                }
                else {
                    Measurement measurement = (Measurement)dataSource.get(DatabaseHelper.MEASUREMENT, extras.getLong("ID"));
                    entry = (Entry)dataSource.get(DatabaseHelper.ENTRY, measurement.getEntryId());
                }

                // and all of its measurements
                List<Model> measurements = dataSource.get(DatabaseHelper.MEASUREMENT, null,
                        DatabaseHelper.ENTRY_ID + "=?", new String[]{ Long.toString(entry.getId()) },
                        null, null, null, null);
                dataSource.close();

                time = entry.getDate();
                editTextNotes.setText(entry.getNote());

                for(Model model : measurements) {
                    Measurement measurement = (Measurement) model;
                    entry.getMeasurements().add(measurement);
                    for(int position = 0; position < linearLayoutValues.getChildCount(); position++) {
                        View view = linearLayoutValues.getChildAt(position);
                        Measurement.Category category = (Measurement.Category)view.getTag();
                        if(category == measurement.getCategory()) {
                            EditText editTextValue = (EditText) view.findViewById(R.id.value);
                            float customValue = preferenceHelper.formatDefaultToCustomUnit(category, measurement.getValue());
                            editTextValue.setText(Helper.getDecimalFormat().format(customValue));
                        }
                    }
                }
            }
            else if(extras.getSerializable(EXTRA_DATE) != null) {
                time = (DateTime) extras.getSerializable(EXTRA_DATE);
            }
        }
    }

    private void setDate() {
        buttonDate.setText(preferenceHelper.getDateFormat().print(time));
    }

    private void setTime() {
        buttonTime.setText(Helper.getTimeFormat().print(time));
    }

    private void setCategories() {
        for(Measurement.Category category : preferenceHelper.getActiveCategories())
            addValue(category);
    }

    private void submit() {
        boolean inputIsValid = true;

        // Validate date
        DateTime now = new DateTime();
        if (time.isAfter(now)) {
            ViewHelper.showAlert(this, getString(R.string.validator_value_infuture));
            return;
        }

        List<Measurement> measurements = new ArrayList<Measurement>();
        // Iterate through all views and validate
        for (int position = 0; position < linearLayoutValues.getChildCount(); position++) {
            View view = linearLayoutValues.getChildAt(position);
            if(view != null && view.getTag() != null) {
                if(view.getTag() instanceof Measurement.Category) {
                    EditText editTextValue = (EditText) view.findViewById(R.id.value);
                    String editTextText = editTextValue.getText().toString();

                    if(editTextText.length() > 0) {
                        Measurement.Category category = (Measurement.Category) view.getTag();

                        if (!Validator.containsNumber(editTextText)) {
                            editTextValue.setError(getString(R.string.validator_value_empty));
                            inputIsValid = false;
                        }
                        else if (!preferenceHelper.validateEventValue(
                                category, preferenceHelper.formatCustomToDefaultUnit(category,
                                        Float.parseFloat(editTextText)))) {
                            editTextValue.setError(getString(R.string.validator_value_unrealistic));
                            inputIsValid = false;
                        }
                        else {
                            editTextValue.setError(null);
                            Measurement measurement = new Measurement();
                            float value = preferenceHelper.formatCustomToDefaultUnit(category, Float.parseFloat(editTextText));
                            measurement.setValue(value);
                            measurement.setCategory(category);
                            measurements.add(measurement);
                        }
                    }
                }
            }
        }

        // Check whether there are values to submit
        if(measurements.size() == 0) {
            // Show alert only if everything else was valid to reduce clutter
            if(inputIsValid)
                ViewHelper.showAlert(this, getString(R.string.validator_value_none));
            inputIsValid = false;
        }

        if(inputIsValid) {
            dataSource.open();

            // Update existing entry
            if(entry != null) {
                entry.setDate(time);
                entry.setNote(editTextNotes.getText().toString());
                dataSource.update(entry);

                // Step through measurements and compare
                List<Measurement> measurementsToDelete = new ArrayList<Measurement>(entry.getMeasurements());
                for(Measurement measurement : measurements) {
                    // Case 1: Measurement is new and old --> Update
                    boolean updatedExistingMeasurement = false;
                    for(Measurement oldMeasurement : entry.getMeasurements()) {
                        if (measurement.getCategory() == oldMeasurement.getCategory()) {
                            oldMeasurement.setValue(measurement.getValue());
                            updatedExistingMeasurement = true;
                            measurementsToDelete.remove(oldMeasurement);
                            dataSource.update(oldMeasurement);
                        }
                    }
                    // Case 2: Measurement is new but not old --> Insert
                    if(!updatedExistingMeasurement) {
                        measurement.setEntryId(entry.getId());
                        dataSource.insert(measurement);
                    }
                }
                // Case 3: Measurement is old but not new --> Delete
                for(Measurement measurement : measurementsToDelete) {
                    dataSource.delete(measurement);
                }
            }

            // Insert new entry
            else {
                entry = new Entry();
                entry.setDate(time);
                if(editTextNotes.length() > 0)
                    entry.setNote(editTextNotes.getText().toString());
                long entryId = dataSource.insert(entry);

                // Connect measurements with entry
                for(Measurement measurement : measurements) {
                    measurement.setEntryId(entryId);
                    dataSource.insert(measurement);
                }
            }
            dataSource.close();

            // Tell MainActivity that Events have been created
            Intent intent = new Intent();
            intent.putExtra(MainActivity.ENTRY_CREATED, measurements.size());
            setResult(Activity.RESULT_OK, intent);

            finish();
        }
    }

    private void addValue(final Measurement.Category category) {
        // Add view
        final LayoutInflater inflater = getLayoutInflater();
        final View view = inflater.inflate(R.layout.fragment_newvalue, linearLayoutValues, false);
        view.setTag(category);

        // Category name
        TextView textViewCategory = (TextView) view.findViewById(R.id.category);
        textViewCategory.setText(preferenceHelper.getCategoryName(category));

        // Status image
        final View viewStatus = view.findViewById(R.id.status);

        // Value
        final EditText editTextValue = (EditText) view.findViewById(R.id.value);
        editTextValue.setHint(preferenceHelper.getUnitAcronym(category));
        if(category == Measurement.Category.BloodSugar)
            editTextValue.requestFocus();

        // OnChangeListener
        TextWatcher textChangedListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if(editTextValue.getText().length() == 0) {
                    viewStatus.setBackgroundColor(getResources().getColor(R.color.gray));
                }
                else {
                    if(!preferenceHelper.validateEventValue(
                            category, preferenceHelper.formatCustomToDefaultUnit(category,
                                    Float.parseFloat(editTextValue.getText().toString())))) {
                        viewStatus.setBackgroundColor(getResources().getColor(R.color.red));
                    }
                    else
                        viewStatus.setBackgroundColor(getResources().getColor(R.color.green));

                    /*
                    // Show an additional View for food information
                    if(category == Measurement.Category.Meal && !mealInfoIsVisible) {
                        View viewMealInfo = inflater.inflate(R.layout.fragment_meal_info, linearLayoutValues, false);
                        viewMealInfo.setTag(DatabaseHelper.FOOD);
                        linearLayoutValues.addView(viewMealInfo, 3);

                        // AutoComplete
                        dataSource.open();
                        List<Model> foodList = dataSource.get(DatabaseHelper.FOOD, null, null, null, null, null, null, null);
                        dataSource.close();
                        String[] foodNames = new String[foodList.size()];
                        for(int foodPosition = 0; foodPosition < foodList.size(); foodPosition++) {
                            Food food = (Food)foodList.get(foodPosition);
                            foodNames[foodPosition] = food.getName();
                        }
                        AutoCompleteTextView editTextFood = (AutoCompleteTextView)viewMealInfo.findViewById(R.id.food);
                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(NewEventActivity.this, android.R.layout.simple_dropdown_item_1line, foodNames);
                        editTextFood.setAdapter(adapter);

                        ViewHelper.expand(viewMealInfo);
                        mealInfoIsVisible = true;
                    }
                    */
                }
            }
        };
        editTextValue.addTextChangedListener(textChangedListener);

        linearLayoutValues.addView(view, linearLayoutValues.getChildCount());
    }

    private void deleteEvent() {
        if (entry != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.entry_delete);
            builder.setMessage(R.string.entry_delete_desc);
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dataSource.open();
                    dataSource.delete(entry);
                    dataSource.close();

                    // Tell MainActivity that entry has been deleted
                    Intent intent = new Intent();
                    intent.putExtra(MainActivity.ENTRY_DELETED, true);
                    setResult(Activity.RESULT_OK, intent);

                    finish();
                }
            });
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User cancelled the dialog
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    // LISTENERS

    public void onClickShowDatePicker (View view) {
        DialogFragment fragment = new DatePickerFragment() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int day) {
                time = time.withYear(year).withMonthOfYear(month+1).withDayOfMonth(day);
                setDate();
            }
        };
        Bundle bundle = new Bundle(1);
        bundle.putSerializable(DatePickerFragment.DATE, time);
        fragment.setArguments(bundle);
        fragment.show(getSupportFragmentManager(), "DatePicker");
    }

    public void onClickShowTimePicker (View view) {
        DialogFragment fragment = new TimePickerFragment() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                time = time.withHourOfDay(hourOfDay).withMinuteOfHour(minute);
                setTime();
            }
        };
        Bundle bundle = new Bundle(1);
        bundle.putSerializable(TimePickerFragment.TIME, time);
        fragment.setArguments(bundle);
        fragment.show(getSupportFragmentManager(), "TimePicker");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_delete:
                deleteEvent();
                return true;
            case R.id.action_done:
                submit();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}