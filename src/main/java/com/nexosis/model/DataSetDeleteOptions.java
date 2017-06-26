package com.nexosis.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

// cascade
public enum DataSetDeleteOptions {
    CASCADE_FORECAST("forecast"),
    CASCADE_SESSION("sessions");
    public static final EnumSet<DataSetDeleteOptions> CASCADE_NONE = EnumSet.noneOf(DataSetDeleteOptions.class);
    public static final EnumSet<DataSetDeleteOptions> CASCASE_BOTH = EnumSet.of(DataSetDeleteOptions.CASCADE_FORECAST, DataSetDeleteOptions.CASCADE_SESSION);

    private final String value;
    private final static Map<String, DataSetDeleteOptions> CONSTANTS = new HashMap<String, DataSetDeleteOptions>();

    static {
        for (DataSetDeleteOptions c: values()) {
            CONSTANTS.put(c.value, c);
        }
    }

    private DataSetDeleteOptions(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }

    @JsonValue
    public String value() {
        return this.value;
    }

    @JsonCreator
    public static DataSetDeleteOptions fromValue(String value) {
        DataSetDeleteOptions constant = CONSTANTS.get(value.toLowerCase(Locale.US));
        if (constant == null) {
            throw new IllegalArgumentException(value);
        } else {
            return constant;
        }
    }
}
