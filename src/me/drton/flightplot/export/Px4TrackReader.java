package me.drton.flightplot.export;

import me.drton.flightplot.FormatErrorException;
import me.drton.flightplot.PX4LogReader;

import java.io.EOFException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ada on 23.12.13.
 */
public class Px4TrackReader implements TrackReader {
    private static final String GPS_LON = "GPS.Lon";
    private static final String GPS_LAT = "GPS.Lat";
    private static final String GPS_ALT = "GPS.Alt";
    private static final String GPS_FIXTYPE = "GPS.FixType";

    // TODO: make this configurable
    private static final int REQUIRED_FIXTYPE = 3;

    private final PX4LogReader reader;

    private long start;
    private long end;
    private long current;
    private int lastSecond;
    private int currentSecond;

    public Px4TrackReader(PX4LogReader reader) throws IOException, FormatErrorException{
        this.reader = reader;
        init();
    }

    private void init() throws IOException, FormatErrorException{
        start = reader.getStartMicroseconds();
        end = start + reader.getSizeMicroseconds();
        reader.seek(start);
        current = start;
    }

    @Override
    public KmlTrackPoint readNextPoint() throws IOException, FormatErrorException{
        KmlTrackPoint result = null;
        // TODO: improve EOF detection
        while(current < end){
            Map<String, Object> data = new HashMap<String, Object>();
            try{
                current = reader.readUpdate(data);
            }catch (EOFException e){
                break;
            }
            currentSecond = (int)current/1000000;

            if(lastSecond < currentSecond && hasGps(data)){
                result = new KmlTrackPoint();

                if(dataContainsPosition(data)){
                    result.setAlt((Float)data.get(GPS_ALT));
                    result.setLat((Double)data.get(GPS_LAT));
                    result.setLon((Double)data.get(GPS_LON));
                }

                result.setTimeInSeconds(currentSecond);
                lastSecond = currentSecond;
                break;
            }
        }
        return result;
    }

    private boolean hasGps(Map<String, Object> data){
        Integer fixType = (Integer)data.get(GPS_FIXTYPE);
        return null != fixType && fixType == REQUIRED_FIXTYPE;
    }

    private boolean dataContainsPosition(Map<String, Object> data){
        return data.containsKey(GPS_LON) && data.containsKey(GPS_LAT) && data.containsKey(GPS_ALT);
    }
}
