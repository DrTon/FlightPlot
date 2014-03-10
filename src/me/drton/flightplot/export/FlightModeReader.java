package me.drton.flightplot.export;

/**
 * Created by ada on 14.01.14.
 */
public class FlightModeReader implements TrackAnalyzer {

    private FlightMode currentFlightMode = null;

    private FlightModeChangeListener listener;

    public FlightModeReader(FlightModeChangeListener listener) {
        this.listener = listener;
    }

    public void inputTrackPoint(TrackPoint point) {
        if (null != point.flightMode && null == this.currentFlightMode) {
            this.currentFlightMode = point.flightMode;
        } else if (null != point.flightMode && this.currentFlightMode != point.flightMode) {
            this.currentFlightMode = point.flightMode;
            listener.flightModeChanged(this.currentFlightMode);
        }
    }
}
