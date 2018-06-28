package com.faltenreich.diaguard.data.entity;

import com.faltenreich.diaguard.data.PreferenceHelper;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable
public class Activity extends Measurement {

    @SuppressWarnings("WeakerAccess")
    public class Column extends Measurement.Column {
        public static final String MINUTES = "minutes";
        public static final String TYPE = "type";
    }

    public enum Type {
        // TODO: Integrate in future version
    }

    @DatabaseField(columnName = Column.MINUTES)
    private int minutes;

    @DatabaseField(columnName = Column.TYPE)
    private Type type;

    public int getMinutes() {
        return minutes;
    }

    public void setMinutes(int minutes) {
        this.minutes = minutes;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    @Override
    public Category getCategory() {
        return Category.ACTIVITY;
    }

    @Override
    public float[] getValues() {
        return new float[] { minutes };
    }

    @Override
    public void setValues(float... values) {
        if (values.length > 0) {
            minutes = (int) values[0];
        }
    }

    @Override
    public String toString() {
        return PreferenceHelper.getInstance().getMeasurementForUi(getCategory(), minutes);
    }
}
