package me.drton.flightplot.export;

import java.util.prefs.Preferences;

/**
 * Created by ada on 19.01.14.
 */
public class ExporterConfiguration {
    private boolean splitTracksByFlightMode;
    private final static String SPLIT_TRACK_BY_FLIGHT_MODE_SETTING = "splitTracksByFlightMode";
    private float altOffset = 0;
    private final static String ADD_ALTITUDE_OFFSET = "addAltitudeOffset";

    private ExportFormatFactory.ExportFormatType exportFormatType;
    private final static String EXPORT_FORMAT_TYPE_SETTING = "exportFormatType";

    public void saveConfiguration(Preferences preferences) {
        preferences.putBoolean(SPLIT_TRACK_BY_FLIGHT_MODE_SETTING, this.splitTracksByFlightMode);
        preferences.putFloat(ADD_ALTITUDE_OFFSET, this.altOffset);
        if (null != this.exportFormatType) {
            preferences.put(EXPORT_FORMAT_TYPE_SETTING, this.exportFormatType.name());
        }
    }

    public void loadConfiguration(Preferences preferences) {
        this.splitTracksByFlightMode = preferences.getBoolean(SPLIT_TRACK_BY_FLIGHT_MODE_SETTING, false);
        this.altOffset = preferences.getFloat(ADD_ALTITUDE_OFFSET, 0);
        String formatType = preferences.get(EXPORT_FORMAT_TYPE_SETTING, null);
        if (null != formatType) {
            try {
                this.exportFormatType = ExportFormatFactory.ExportFormatType.valueOf(formatType);
            } catch (IllegalArgumentException e) {
                this.exportFormatType = null;
            }
        }
    }

    public boolean isSplitTracksByFlightMode() {
        return splitTracksByFlightMode;
    }

    public void setSplitTracksByFlightMode(boolean splitTracksByFlightMode) {
        this.splitTracksByFlightMode = splitTracksByFlightMode;
    }

    public void setAltitudeOffset(float offset) {
        this.altOffset = offset;
    }

    public float getAltOffset() {
        return altOffset;
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
