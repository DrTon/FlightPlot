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
    public static final String GPS_LON = "GPS.Lon";
    public static final String GPS_LAT = "GPS.Lat";
    public static final String GPS_ALT = "GPS.Alt";

    private final PX4LogReader reader;

    private long start;
    private long end;
    private long current;
    private int lastSecond;
    private int currentSecond;
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

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

            if(data.containsKey(GPS_LON) && data.containsKey(GPS_LAT) && data.containsKey(GPS_ALT)){
                Float alt = (Float)data.get(GPS_ALT);
                Double lon = (Double)data.get(GPS_LON);
                Double lat = (Double)data.get(GPS_LAT);
                if(null != alt && null != lon && null != lat && lastSecond < currentSecond){
                    result = new KmlTrackPoint();
                    result.setAlt(alt);
                    result.setLat(lat);
                    result.setLon(lon);
                    result.setTimeInSeconds(currentSecond);
                    lastSecond = currentSecond;
                    break;
                }
            }
        }
        return result;
    }
}
