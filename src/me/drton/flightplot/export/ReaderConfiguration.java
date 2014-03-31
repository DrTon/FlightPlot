package me.drton.flightplot.export;

import java.util.prefs.Preferences;

/**
 * Created by ada on 25.01.14.
 */
public class ReaderConfiguration {
    private double samplesPerSecond;
    private final static String SAMPLES_PER_SECOND_SETTING = "samplesPerSecond";

    private boolean exportChartRangeOnly;
    private final static String EXPORT_CHART_RANGE_ONLY_SETTING = "exportChartRangeOnly";

    private long timeFrom;
    private long timeTo;

    public void saveConfiguration(Preferences preferences) {
        preferences.putDouble(SAMPLES_PER_SECOND_SETTING, this.samplesPerSecond);
        preferences.putBoolean(EXPORT_CHART_RANGE_ONLY_SETTING, this.exportChartRangeOnly);
    }

    public void loadConfiguration(Preferences preferences) {
        this.samplesPerSecond = preferences.getDouble(SAMPLES_PER_SECOND_SETTING, Double.MAX_VALUE);
        this.exportChartRangeOnly = preferences.getBoolean(EXPORT_CHART_RANGE_ONLY_SETTING, false);
    }

    public double getSamplesPerSecond() {
        return samplesPerSecond;
    }

    public void setSamplesPerSecond(double samplesPerSecond) {
        this.samplesPerSecond = samplesPerSecond;
    }

    public long getTimeFrom() {
        return timeFrom;
    }

    public void setTimeFrom(long timeFrom) {
        this.timeFrom = timeFrom;
    }

    public long getTimeTo() {
        return timeTo;
    }

    public void setTimeTo(long timeTo) {
        this.timeTo = timeTo;
    }

    public boolean isExportChartRangeOnly() {
        return exportChartRangeOnly;
    }

    public void setExportChartRangeOnly(boolean exportChartRangeOnly) {
        this.exportChartRangeOnly = exportChartRangeOnly;
    }
}
