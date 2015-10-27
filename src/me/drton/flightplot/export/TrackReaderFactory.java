package me.drton.flightplot.export;

import me.drton.jmavlib.log.FormatErrorException;
import me.drton.jmavlib.log.LogReader;
import me.drton.jmavlib.log.px4.PX4LogReader;
import me.drton.jmavlib.log.ulog.ULogReader;

import java.io.IOException;

/**
 * Created by ada on 24.12.13.
 */
public class TrackReaderFactory {
    public static TrackReader getTrackReader(LogReader reader, TrackReaderConfiguration config) throws IOException, FormatErrorException {
        if (reader instanceof PX4LogReader) {
            return new PX4TrackReader((PX4LogReader) reader, config);
        } else if (reader instanceof ULogReader) {
            return new ULogTrackReader((ULogReader) reader, config);
        } else {
            throw new UnsupportedOperationException(
                    String.format("No track reader for \"%s\" format available.", reader.getFormat()));
        }
    }
}
