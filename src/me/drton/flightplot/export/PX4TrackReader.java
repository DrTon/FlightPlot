package me.drton.flightplot.export;

import me.drton.jmavlib.log.FormatErrorException;
import me.drton.jmavlib.log.PX4LogReader;

import java.io.EOFException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ada on 23.12.13.
 */
public class PX4TrackReader extends AbstractTrackReader {
    private static final String GPOS_LAT = "GPOS.Lat";
    private static final String GPOS_LON = "GPOS.Lon";
    private static final String GPOS_ALT = "GPOS.Alt";
    private static final String STAT_MAINSTATE = "STAT.MainState";

    private String floghtMode = null;

    public PX4TrackReader(PX4LogReader reader, TrackReaderConfiguration config) throws IOException, FormatErrorException {
        super(reader, config);
    }

    @Override
    public TrackPoint readNextPoint() throws IOException, FormatErrorException {
        Map<String, Object> data = new HashMap<String, Object>();
        while (true) {
            data.clear();
            long t;
            try {
                t = readUpdate(data);
            } catch (EOFException e) {
                break;  // End of file
            }
            String currentFlightMode = getFlightMode(data);
            if (currentFlightMode != null) {
                floghtMode = currentFlightMode;
            }
            Number lat = (Number) data.get(GPOS_LAT);
            Number lon = (Number) data.get(GPOS_LON);
            Number alt = (Number) data.get(GPOS_ALT);
            if (lat != null && lon != null && alt != null) {
                return new TrackPoint(lat.doubleValue(), lon.doubleValue(), alt.doubleValue() + config.getAltitudeOffset(),
                        t + reader.getUTCTimeReferenceMicroseconds(), floghtMode);
            }
        }
        return null;
    }

    private String getFlightMode(Map<String, Object> data) {
        Number flightMode = (Number) data.get(STAT_MAINSTATE);
        if (flightMode != null) {
            switch (flightMode.intValue()) {
                case 0:
                    return "MANUAL";
                case 1:
                    return "ALTCTL";
                case 2:
                    return "POSCTL";
                case 3:
                    return "AUTO_MISSION";
                case 4:
                    return "AUTO_LOITER";
                case 5:
                    return "AUTO_RTL";
                case 6:
                    return "AUTO_ACRO";
                case 7:
                    return "AUTO_OFFBOARD";
                default:
                    return String.format("UNKNOWN(%s)", flightMode.intValue());
            }
        }
        return null;    // Not supported
    }
}
