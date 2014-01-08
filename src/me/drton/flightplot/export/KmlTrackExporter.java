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
            writer.writeStart();

            TrackPoint point = trackReader.readNextPoint();
            FlightMode currentFlightMode = point.flightMode;
            writer.startTrackPart(determineStyleByFlightMode(currentFlightMode));

            while (null != point) {
                if(point.flightMode != currentFlightMode){
                    writer.endTrackPart();
                    currentFlightMode = point.flightMode;
                    writer.startTrackPart(determineStyleByFlightMode(currentFlightMode));
                }

                writer.writePoint(point);
                point = trackReader.readNextPoint();
            }

            writer.endTrackPart();
            writer.writeEnd();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            fileWriter.close();
        }
    }

    protected String determineStyleByFlightMode(FlightMode flightMode){
        if(null == flightMode) {
            return KmlTrackExportWriter.LINE_STYLE_YELLOW;
        }

        switch(flightMode){
            case AUTO:
                return KmlTrackExportWriter.LINE_STYLE_RED;
            case STABILIZED:
                return KmlTrackExportWriter.LINE_STYLE_BLUE;
            case MANUAL:
            default:
                return KmlTrackExportWriter.LINE_STYLE_YELLOW;
        }
    }
}
