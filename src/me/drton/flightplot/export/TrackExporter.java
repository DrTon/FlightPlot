package me.drton.flightplot.export;

import java.io.File;
import java.io.IOException;

/**
 * Created by ada on 19.01.14.
 */
public interface TrackExporter {
    String getName();

    String getDescription();

    String getFileExtension();

    /**
     * Exports track to specified file, uses title if possible for current export format.
     *
     * @param file output file
     * @param title track title
     * @throws IOException
     */
    void export(TrackReader trackReader, TrackExporterConfiguration config, File file, String title) throws IOException;
}
