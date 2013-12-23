package me.drton.flightplot.export;

import me.drton.flightplot.FormatErrorException;

import java.io.IOException;

/**
 * Created by ada on 24.12.13.
 */
public interface TrackReader {

    /**
     * Reads next track point from LogReader.
     * @return returns KmlTrackPoint or null no more points can be read.
     * @throws IOException
     * @throws FormatErrorException
     */
    KmlTrackPoint readNextPoint() throws IOException, FormatErrorException;
}
