package me.drton.flightplot.export;

import me.drton.flightplot.FormatErrorException;
import me.drton.flightplot.PX4LogReader;

import java.io.EOFException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ada on 23.12.13.
 */
public class PX4TrackReader extends AbstractTrackReader{
    private static final String GPS_TIME = "GPS.GPSTime";
    private static final String GPS_LON = "GPS.Lon";
    private static final String GPS_LAT = "GPS.Lat";
    private static final String GPS_ALT = "GPS.Alt";
    private static final String GPS_FIXTYPE = "GPS.FixType";
    private static final String STAT_MAINSTATE = "STAT.MainState";
    private static final int REQUIRED_FIXTYPE = 3;

    private FlightMode lastFlightMode = null;

    public PX4TrackReader(PX4LogReader reader) throws IOException, FormatErrorException {
        super(reader);
    }

    @Override
    public TrackPoint readNextPoint() throws IOException, FormatErrorException {
        Map<String, Object> data = new HashMap<String, Object>();
        while (true) {
            try {
                readUpdate(data);
            } catch (EOFException e) {
                break;  // End of file
            }
            Long timeGPS = (Long) data.get(GPS_TIME);
            FlightMode currentFlightMode = extractFlightMode(data);
            /*
            TODO: Is it necessary to hold flight mode here?
            Missing flight mode will be compensated by FlightModeReader already
            */
            if(null != currentFlightMode){
                lastFlightMode = currentFlightMode;
            }
            if (timeGPS != null) {
                long time = timeGPS / 1000;
                Integer fixType = (Integer) data.get(GPS_FIXTYPE);
                Double lat = (Double) data.get(GPS_LAT);
                Double lon = (Double) data.get(GPS_LON);
                Float alt = (Float) data.get(GPS_ALT);
                if (fixType != null && fixType >= REQUIRED_FIXTYPE
                        && lat != null && lon != null && alt != null) {
                    TrackPoint point = new TrackPoint(lat, lon, alt, time);
                    point.flightMode = lastFlightMode;
                    data.clear();
                    return point;
                }
            }
        }
        return null;
    }

    private FlightMode extractFlightMode(Map<String, Object> data) {
        Integer flightMode = (Integer) data.get(STAT_MAINSTATE);
        if(null != flightMode){
            switch(flightMode){
                case 0: // MAIN_STATE_MANUAL
                    return FlightMode.MANUAL;
                case 1: // MAIN_STATE_SEATBELT
                case 2: // MAIN_STATE_EASY
                    return FlightMode.STABILIZED;
                case 3: // MAIN_STATE_AUTO
                    return FlightMode.AUTO;
            }
        }
        return null;
    }


}
