package me.drton.flightplot.export;

import me.drton.jmavlib.log.FormatErrorException;
import me.drton.jmavlib.log.ulog.ULogReader;

import java.io.EOFException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ada on 23.12.13.
 */
public class ULogTrackReader extends AbstractTrackReader {
    private static final String POS_VALID = "ATTITUDE_POSITION.valid_pos";
    private static final String POS_LAT = "ATTITUDE_POSITION.lat";
    private static final String POS_LON = "ATTITUDE_POSITION.lon";
    private static final String POS_ALT = "ATTITUDE_POSITION.alt_msl";
    private static final String MODE = "SYSTEM_STATUS.mode";

    private String flightMode = null;

    public ULogTrackReader(ULogReader reader, TrackReaderConfiguration config) throws IOException, FormatErrorException {
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
                flightMode = currentFlightMode;
            }
            Number valid = (Number) data.get(POS_VALID);
            Number lat = (Number) data.get(POS_LAT);
            Number lon = (Number) data.get(POS_LON);
            Number alt = (Number) data.get(POS_ALT);
            if (valid != null && lat != null && lon != null && alt != null && valid.intValue() != 0) {
                return new TrackPoint(lat.doubleValue(), lon.doubleValue(), alt.doubleValue() + config.getAltitudeOffset(),
                        t + reader.getUTCTimeReferenceMicroseconds(), flightMode);
            }
        }
        return null;
    }

    private String getFlightMode(Map<String, Object> data) {
        Number flightMode = (Number) data.get(MODE);
        if (flightMode != null) {
            switch (flightMode.intValue()) {
                case 1:
                    return "MANUAL";
                case 2:
                    return "ALTCTL";
                case 3:
                    return "POSCTL";
                case 4:
                    return "RTH";
                default:
                    return String.format("UNKNOWN(%s)", flightMode.intValue());
            }
        }
        return null;    // Not supported
    }
}
