package me.drton.flightplot.export;

import java.util.prefs.Preferences;

/**
 * Created by ada on 19.01.14.
 */
public class ExporterConfiguration {
    private boolean splitTracksByFlightMode;
    private final static String SPLIT_TRACK_BY_FLIGHT_MODE_SETTING = "splitTracksByFlightMode";

    private ExportFormatFactory.ExportFormatType exportFormatType;
    private final static String EXPORT_FORMAT_TYPE_SETTING = "exportFormatType";

    public void saveConfiguration(Preferences preferences) {
        preferences.putBoolean(SPLIT_TRACK_BY_FLIGHT_MODE_SETTING, this.splitTracksByFlightMode);
        if (null != this.exportFormatType) {
            preferences.put(EXPORT_FORMAT_TYPE_SETTING, this.exportFormatType.name());
        }
    }

    public void loadConfiguration(Preferences preferences) {
        this.splitTracksByFlightMode = preferences.getBoolean(SPLIT_TRACK_BY_FLIGHT_MODE_SETTING, false);
        String formatType = preferences.get(EXPORT_FORMAT_TYPE_SETTING, null);
        if (null != formatType) {
            this.exportFormatType = ExportFormatFactory.ExportFormatType.valueOf(formatType);
        }
    }

    public boolean isSplitTracksByFlightMode() {
        return splitTracksByFlightMode;
    }

    public void setSplitTracksByFlightMode(boolean splitTracksByFlightMode) {
        this.splitTracksByFlightMode = splitTracksByFlightMode;
    }

    public ExportFormat getExportFormat() {
        return this.exportFormatType.getExportFormat();
    }

    public ExportFormatFactory.ExportFormatType getExportFormatType() {
        return exportFormatType;
    }

    public void setExportFormatType(ExportFormatFactory.ExportFormatType exportFormatType) {
        this.exportFormatType = exportFormatType;
    }
}
