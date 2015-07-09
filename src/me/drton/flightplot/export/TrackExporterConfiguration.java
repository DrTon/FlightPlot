package me.drton.flightplot.export;

import java.util.prefs.Preferences;

/**
 * Created by ada on 19.01.14.
 */
public class TrackExporterConfiguration {
    private boolean splitTracksByFlightMode;
    private final static String SPLIT_TRACK_BY_FLIGHT_MODE_SETTING = "splitTracksByFlightMode";
    private String exportFormat;
    private final static String EXPORT_FORMAT_TYPE_SETTING = "exportFormat";

    public void saveConfiguration(Preferences preferences) {
        preferences.putBoolean(SPLIT_TRACK_BY_FLIGHT_MODE_SETTING, this.splitTracksByFlightMode);
        if (exportFormat != null) {
            preferences.put(EXPORT_FORMAT_TYPE_SETTING, exportFormat);
        }
    }

    public void loadConfiguration(Preferences preferences) {
        splitTracksByFlightMode = preferences.getBoolean(SPLIT_TRACK_BY_FLIGHT_MODE_SETTING, false);
        exportFormat = preferences.get(EXPORT_FORMAT_TYPE_SETTING, null);
    }

    public boolean isSplitTracksByFlightMode() {
        return splitTracksByFlightMode;
    }

    public void setSplitTracksByFlightMode(boolean splitTracksByFlightMode) {
        splitTracksByFlightMode = splitTracksByFlightMode;
    }

    public String getExportFormat() {
        return exportFormat;
    }

    public void setExportFormat(String exportFormat) {
        this.exportFormat = exportFormat;
    }
}
