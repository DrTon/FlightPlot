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
public class PX4TrackReader implements TrackReader {
    private static final String GPS_TIME = "GPS.GPSTime";
    private static final String GPS_LON = "GPS.Lon";
    private static final String GPS_LAT = "GPS.Lat";
    private static final String GPS_ALT = "GPS.Alt";
    private static final String GPS_FIXTYPE = "GPS.FixType";
    private static final String STAT_MAINSTATE = "STAT.MainState";
    private static final int REQUIRED_FIXTYPE = 3;

    private final PX4LogReader reader;
    private long nextMinTime = 0;
    private long timeGap = 0;
    private FlightMode lastFlightMode = null;
    private ReaderConfiguration configuration = new ReaderConfiguration();

    public PX4TrackReader(PX4LogReader reader) throws IOException, FormatErrorException {
        this.reader = reader;
        reset();
        initFromConfig();
    }

    public void reset() throws IOException, FormatErrorException {
        reader.seek(0);
    }

    @Override
    public TrackPoint readNextPoint() throws IOException, FormatErrorException {
        Map<String, Object> data = new HashMap<String, Object>();
        while (true) {
            try {
                reader.readUpdate(data);
            } catch (EOFException e) {
                break;  // End of file
            }
            Long timeGPS = (Long) data.get(GPS_TIME);
            FlightMode currentFlightMode = extractFlightMode(data);
            if(null != currentFlightMode){
                lastFlightMode = currentFlightMode;
            }
            if (timeGPS != null) {
                long time = timeGPS / 1000;
                Integer fixType = (Integer) data.get(GPS_FIXTYPE);
                Double lat = (Double) data.get(GPS_LAT);
                Double lon = (Double) data.get(GPS_LON);
                Float alt = (Float) data.get(GPS_ALT);
                if (time >= this.nextMinTime && fixType != null && fixType >= REQUIRED_FIXTYPE &&
                        lat != null && lon != null && alt != null) {
                    if(0 == this.nextMinTime){
                        this.nextMinTime = time;
                    }
                    this.nextMinTime += this.timeGap;
                    TrackPoint point = new TrackPoint(lat, lon, alt, time);
                    point.flightMode = lastFlightMode;
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

    @Override
    public void setConfiguration(ReaderConfiguration configuration) {
        this.configuration = configuration;
        initFromConfig();
    }

    private void initFromConfig(){
        this.timeGap = (long)Math.floor(1000 / this.configuration.getSamplesPerSecond());
    }
}
