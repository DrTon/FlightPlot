package me.drton.flightplot.export;

import me.drton.flightplot.FormatErrorException;
import me.drton.flightplot.LogReader;
import me.drton.flightplot.PX4LogReader;

import java.io.IOException;

/**
 * Created by ada on 24.12.13.
 */
public class TrackReaderFactory {
    public static TrackReader getTrackReader(LogReader reader) throws IOException, FormatErrorException {
        if (reader instanceof PX4LogReader) {
            return new PX4TrackReader((PX4LogReader) reader);
        } else {
            throw new UnsupportedOperationException(
                    String.format("No track reader for this %s available.", reader.getClass()));
        }
    }
}
