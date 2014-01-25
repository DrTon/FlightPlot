package me.drton.flightplot.export;

/**
 * Created by ada on 19.01.14.
 */
public class ExporterConfiguration {
    private boolean splitTracksByFlightMode = false;

    public boolean isSplitTracksByFlightMode() {
        return splitTracksByFlightMode;
    }

    public void setSplitTracksByFlightMode(boolean splitTracksByFlightMode) {
        this.splitTracksByFlightMode = splitTracksByFlightMode;
    }
}
