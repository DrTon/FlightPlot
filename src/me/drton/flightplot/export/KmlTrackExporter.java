package me.drton.flightplot.export;

import me.drton.flightplot.FormatErrorException;
import me.drton.flightplot.LogReader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * Created by ada on 23.12.13.
 */
public class KmlTrackExporter {

    protected final TrackReader trackReader;

    public KmlTrackExporter(TrackReader trackReader){
        this.trackReader = trackReader;
    }

    public void exportToFile(File file) throws IOException {
        Writer fileWriter = new FileWriter(file);
        KmlTrackExportWriter writer = new KmlTrackExportWriter(fileWriter);

        try {
            writer.writeStart();
            KmlTrackPoint currentPoint = trackReader.readNextPoint();
            while(null != currentPoint){
                writer.writePoint(currentPoint);
                currentPoint = trackReader.readNextPoint();
            }
            writer.writeEnd();
        }
        catch(IOException e){
            e.printStackTrace();
        }
        catch (FormatErrorException e){
            e.printStackTrace();
        }
        finally{
            fileWriter.close();
        }
    }

}
