package com.faltenreich.diaguard;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;

import com.faltenreich.diaguard.database.DatabaseDataSource;
import com.faltenreich.diaguard.database.DatabaseHelper;
import com.faltenreich.diaguard.database.Entry;
import com.faltenreich.diaguard.database.Food;
import com.faltenreich.diaguard.database.Measurement;
import com.faltenreich.diaguard.database.Model;
import com.faltenreich.diaguard.fragments.DatePickerFragment;
import com.faltenreich.diaguard.fragments.TimePickerFragment;
import com.faltenreich.diaguard.helpers.FileHelper;
import com.faltenreich.diaguard.helpers.PreferenceHelper;
import com.faltenreich.diaguard.helpers.Validator;
import com.faltenreich.diaguard.helpers.ViewHelper;

import org.joda.time.DateTime;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.keyboardsurfer.android.widget.crouton.Crouton;

/**
 * Created by Filip on 19.10.13.
 */
public class NewEventActivity extends ActionBarActivity {

    public static final String EXTRA_ID = "ID";
    public static final String EXTRA_DATE = "Date";
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_TAKE_PHOTO = 1;

    String currentPhotoPath;

    DatabaseDataSource dataSource;
    PreferenceHelper preferenceHelper;

    DateTime time;
    boolean inputWasMade;
    Bitmap imageTemp;
    boolean mealInfoIsVisible;

    LinearLayout linearLayoutValues;
    EditText editTextNotes;
    Button buttonDate;
    Button buttonTime;
    ImageView imageViewCamera;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_newevent);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle(getString(R.string.newevent));
        initialize();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Crouton.cancelAllCroutons();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.formular, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.item, menu);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            imageTemp = (Bitmap) extras.get("data");
            imageViewCamera.setImageBitmap(imageTemp);
            imageViewCamera.setScaleType(ImageView.ScaleType.CENTER_CROP);
        }
    }

    public void initialize() {
        dataSource = new DatabaseDataSource(this);
        preferenceHelper = new PreferenceHelper(this);
        time = new DateTime();
        inputWasMade = false;

        getComponents();
        checkIntents();
        setDate();
        setTime();
        setCategories();
    }

    public void getComponents() {
        linearLayoutValues = (LinearLayout) findViewById(R.id.content_dynamic);
        editTextNotes = (EditText) findViewById(R.id.edittext_notes);
        buttonDate = (Button) findViewById(R.id.button_date);
        buttonTime = (Button) findViewById(R.id.button_time);
        imageViewCamera = (ImageView) findViewById(R.id.button_camera);
    }

    private void checkIntents() {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            if (extras.getLong(EXTRA_ID) != 0L) {
                dataSource.open();
                Measurement measurement = (Measurement)dataSource.get(DatabaseHelper.MEASUREMENT, extras.getLong("ID"));
                Entry entry = (Entry)dataSource.get(DatabaseHelper.ENTRY, measurement.getEntryId());
                dataSource.close();

                time = entry.getDate();
                float value = preferenceHelper.
                        formatDefaultToCustomUnit(measurement.getCategory(), measurement.getValue());
                editTextNotes.setText(entry.getNote());
                addValue(measurement.getCategory(), preferenceHelper.getDecimalFormat(
                        measurement.getCategory()).format(value));
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
        buttonTime.setText(preferenceHelper.getTimeFormat().print(time));
    }

    private void setCategories() {
        for(Measurement.Category category : preferenceHelper.getActiveCategories()) {
            addValue(category, null);
        }
    }

    private void submit() {

        Food food = null;
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

                else if(view.getTag() instanceof String) {
                    String tag = (String)view.getTag();
                    if(tag.equals(DatabaseHelper.FOOD)) {
                        AutoCompleteTextView editTextFood = (AutoCompleteTextView) view.findViewById(R.id.food);

                        // Check if a Meal has been entered and get its values
                        boolean mealIsAvailable = false;
                        int eventPosition = 0;
                        while(!mealIsAvailable && eventPosition < measurements.size()) {
                            if(measurements.get(eventPosition).getCategory() == Measurement.Category.Meal)
                                mealIsAvailable = true;
                            eventPosition++;
                        }

                        if(mealIsAvailable) {
                            food = new Food();
                            // TODO handle position better
                            food.setCarbohydrates(measurements.get(eventPosition-1).getValue());
                            food.setName(editTextFood.getText().toString());
                            food.setDate(time);
                            // eventId is set later
                        }
                    }
                }
            }
        }

        // Check whether there are values to submit
        if(measurements.size() == 0) {
            ViewHelper.showAlert(this, getString(R.string.validator_value_none));
            inputIsValid = false;
        }

        if(inputIsValid) {
            dataSource.open();

            // Entry
            Entry entry = new Entry();
            entry.setDate(time);
            entry.setNote(editTextNotes.getText().toString());
            long entryId = dataSource.insert(entry);

            // Events
            long[] ids;
            Bundle extras = getIntent().getExtras();
            // Update existing
            if (extras != null && extras.getLong(EXTRA_ID) != 0L) {
                measurements.get(0).setId(extras.getLong(EXTRA_ID));
                ids = new long[1];
                ids[0] = dataSource.update(measurements.get(0));
            }
            // Insert new
            else {
                ids = dataSource.insert(measurements);
            }

            // Food
            if(food != null) {
                for(int position = 0; position < measurements.size(); position++) {
                    if(measurements.get(position).getCategory() == Measurement.Category.Meal) {
                        food.setEventId(ids[position]);
                        dataSource.insert(food);
                    }
                }
            }

            dataSource.close();

            // Tell MainActivity that Events have been created
            Intent intent = new Intent();
            intent.putExtra(MainActivity.EVENT_CREATED, measurements.size());
            setResult(Activity.RESULT_OK, intent);

            finish();
        }
    }

    private void addValue(final Measurement.Category category, String value) {
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
        if(value != null) {
            editTextValue.setText(value);
        }
        if(category == Measurement.Category.BloodSugar)
            editTextValue.requestFocus();

        // OnChangeListener
        TextWatcher textChangedListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                inputWasMade = true;
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
                }
            }
        };
        editTextValue.addTextChangedListener(textChangedListener);

        linearLayoutValues.addView(view, linearLayoutValues.getChildCount());
    }

    private void deleteEvent() {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            long id = extras.getLong("ID");
            if (id != 0L) {
                dataSource.open();
                //Event event = dataSource.getEventById(id);
                //dataSource.deleteEvent(event);
                dataSource.close();
                finish();
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String imageFileName = "test";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        currentPhotoPath = "file:" + image.getAbsolutePath();
        return image;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    // LISTENERS

    public void onClickShowDatePicker (View view) {
        DialogFragment fragment = new DatePickerFragment() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int day) {
                time.withYear(year);
                time.withMonthOfYear(month);
                time.withDayOfMonth(day);
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
                time.withHourOfDay(hourOfDay);
                time.withMinuteOfHour(minute);
                setTime();
            }
        };
        Bundle bundle = new Bundle(1);
        bundle.putSerializable(TimePickerFragment.TIME, time);
        fragment.setArguments(bundle);
        fragment.show(getSupportFragmentManager(), "TimePicker");
    }

    public void onClickCamera(View view) {
        // Check if an image has already been shot
        if(imageTemp == null) {
            // Open camera app
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            Uri uri = Uri.parse(FileHelper.PATH_STORAGE + "/image");
            if (intent.resolveActivity(getPackageManager()) != null) {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
            }
        }
        else {
            // Open context menu
            registerForContextMenu(imageViewCamera);
            openContextMenu(imageViewCamera);
            unregisterForContextMenu(imageViewCamera);
        }
    }

    @Override
    public void onBackPressed() {
        if(inputWasMade) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.confirmation_exit))
                    .setMessage(getString(R.string.confirmation_exit_desc))
                    .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }

                    })
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show();
        }
        else
            finish();
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

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.view:
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse("file://" + "/sdcard/test.jpg"), "image/*");
                startActivity(intent);
                return true;
            case R.id.remove:
                imageViewCamera.setImageDrawable(getResources().getDrawable(R.drawable.camera));
                imageViewCamera.setScaleType(ImageView.ScaleType.FIT_CENTER);
                imageTemp = null;
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }
}