package me.drton.flightplot.export;

import java.util.prefs.Preferences;

/**
 * Created by ada on 25.01.14.
 */
public class TrackReaderConfiguration {
    private long timeInterval;      /// Min time interval between generated points, [us]
    private final static String TIME_INTERVAL_SETTING = "timeInterval";
    private long timeStart;         /// Start time, [us]
    private long timeEnd;           /// End time, [us]
    private double altitudeOffset;  /// Altitude offset, [m]
    private final static String ALTITUDE_OFFSET_SETTING = "altitudeOffset";

    public void saveConfiguration(Preferences preferences) {
        preferences.putLong(TIME_INTERVAL_SETTING, timeInterval);
        preferences.putDouble(ALTITUDE_OFFSET_SETTING, altitudeOffset);
    }

    public void loadConfiguration(Preferences preferences) {
        timeInterval = preferences.getLong(TIME_INTERVAL_SETTING, 0);
        altitudeOffset = preferences.getDouble(ALTITUDE_OFFSET_SETTING, 0.0);
    }

    public long getTimeInterval() {
        return timeInterval;
    }

    public void setTimeInterval(long timeInterval) {
        this.timeInterval = timeInterval;
    }

    public long getTimeStart() {
        return timeStart;
    }

    public void setTimeStart(long timeStart) {
        this.timeStart = timeStart;
    }

    public long getTimeEnd() {
        return timeEnd;
    }

    public void setTimeEnd(long timeEnd) {
        this.timeEnd = timeEnd;
    }

    public double getAltitudeOffset() {
        return altitudeOffset;
    }

    public void setAltitudeOffset(double altitudeOffset) {
        this.altitudeOffset = altitudeOffset;
    }
}
