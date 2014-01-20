package me.drton.flightplot.export;

import java.io.File;
import java.io.IOException;

/**
 * Created by ada on 19.01.14.
 */
public interface TrackExporter {

    /**
     * Exports track to specified file, uses title if possible for current export format.
     * @param file
     * @param title
     * @throws IOException
     */
    public void exportToFile(File file, String title) throws IOException;

    /**
     * Configures exporter, usually through user input by showing a dialog.
     * @return true if process should continue, false otherwise
     */
    boolean configureExporter();

}
