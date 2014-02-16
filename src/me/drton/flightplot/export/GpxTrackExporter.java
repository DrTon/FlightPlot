package me.drton.flightplot.export;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * Created by ada on 16.02.14.
 */
public class GpxTrackExporter extends AbstractTrackExporter {

    private GpxTrackWriter writer;

    public GpxTrackExporter(TrackReader trackReader) {
        super(trackReader);
    }

    @Override
    public void exportToFile(File file, String title) throws IOException {
        Writer fileWriter = initWriter(file, title);
        try {
            this.writer.writeStart();
            TrackPoint point = readNextPoint();
            this.writer.startTrackPart();
            while (null != point) {
                this.writer.writePoint(point);
                point = readNextPoint();
            }

            this.writer.endTrackPart();
            this.writer.writeEnd();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            fileWriter.close();
        }
    }

    private Writer initWriter(File file, String title) throws IOException {
        Writer fileWriter = new FileWriter(file);
        this.writer = new GpxTrackWriter(fileWriter, title);
        return fileWriter;
    }


}
