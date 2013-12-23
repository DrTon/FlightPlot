package me.drton.flightplot.export;

import me.drton.flightplot.FormatErrorException;
import me.drton.flightplot.LogReader;
import me.drton.flightplot.PX4LogReader;

import java.io.IOException;

/**
 * Created by ada on 24.12.13.
 */
public class KmlTrackExporterFactory {

    public static KmlTrackExporter getKmlTrackExporter(LogReader reader) throws IOException, FormatErrorException {
        if(reader instanceof PX4LogReader){
            return new KmlTrackExporter(new Px4TrackReader((PX4LogReader)reader));
        }
        else {
            throw new UnsupportedOperationException("No track reader available.");
        }
    }

}
