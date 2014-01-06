package me.drton.flightplot.export;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * Created by ada on 23.12.13.
 */
public class KmlTrackExporter {
    protected final TrackReader trackReader;

    public KmlTrackExporter(TrackReader trackReader) {
        this.trackReader = trackReader;
    }

    public void exportToFile(File file, String title) throws IOException {
        Writer fileWriter = new FileWriter(file);
        KmlTrackExportWriter writer = new KmlTrackExportWriter(fileWriter, title);
        try {
            int count = 0;
            writer.writeStart();
            writer.startTrackPart();
            while (true) {
                count ++;
                TrackPoint point = trackReader.readNextPoint();
                if (point == null)
                    break;
                writer.writePoint(point);

                if(count > 500){
                    writer.endTrackPart();
                    writer.startTrackPart("red");
                    count = 0;
                }
            }
            writer.endTrackPart();
            writer.writeEnd();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            fileWriter.close();
        }
    }
}
