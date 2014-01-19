package me.drton.flightplot.export;

import java.util.List;

/**
 * Created by ada on 14.01.14.
 */
public class FlightModeReader implements TrackAnalyzer{

    private FlightMode currentFlighMode = null;

    private FlightModeChangeListener listener;

    public FlightModeReader(FlightModeChangeListener listener){
        this.listener = listener;
    }

    public void inputTrackPoint(TrackPoint point){
        if(null != point.flightMode && null == this.currentFlighMode){
            this.currentFlighMode = point.flightMode;
        }
        else if(null != point.flightMode && this.currentFlighMode != point.flightMode){
            this.currentFlighMode = point.flightMode;
            listener.flightModeChanged(this.currentFlighMode);
        }
    }
}
