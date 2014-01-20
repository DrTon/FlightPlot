package me.drton.flightplot.export;

import me.drton.flightplot.FormatErrorException;

import javax.swing.*;
import java.io.IOException;

/**
 * Created by ada on 24.12.13.
 */
public interface TrackReader {

    /**
     * Reads next track point from LogReader.
     * @return returns TrackPoint or null if no more points can be read.
     * @throws IOException
     * @throws FormatErrorException
     */
    TrackPoint readNextPoint() throws IOException, FormatErrorException;

    /**
     * Reset reader to start of log.
     */
    void reset() throws IOException, FormatErrorException;

    /**
     * Configures reader, usually through user input by showing a dialog.
     * @return true if process should continue, false otherwise
     */
    boolean configureReader();

}
