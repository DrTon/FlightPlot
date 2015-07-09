package me.drton.flightplot.export;

import java.io.*;
import java.lang.reflect.Method;

/**
 * Created by ada on 14.01.14.
 */
public abstract class AbstractTrackExporter implements TrackExporter {
    protected TrackReader trackReader;
    protected TrackExporterConfiguration config;
    protected String title;
    protected Writer writer;
    protected int trackPart = 0;
    protected String flightMode = null;

    @Override
    public void export(TrackReader trackReader, TrackExporterConfiguration config, File file, String title) throws IOException {
        this.trackReader = trackReader;
        this.config = config;
        this.writer = new BufferedWriter(new FileWriter(file));
        this.title = title;
        boolean trackStarted = false;
        try {
            writeStart();
            while (true) {
                TrackPoint point = trackReader.readNextPoint();
                if (point == null) {
                    break;
                }
                if (!trackStarted || (point.flightMode != null && !point.flightMode.equals(flightMode))) {
                    if (trackStarted) {
                        writePoint(point);  // Write this point at the end of previous track to avoid interruption of track
                        writeTrackPartEnd();
                    }
                    flightMode = point.flightMode;
                    String trackPartName;
                    if (point.flightMode != null) {
                        trackPartName = String.format("%s: %s", trackPart, point.flightMode);
                        trackPart++;
                    } else {
                        trackPartName = "Track";
                    }
                    writeTrackPartStart(trackPartName);
                    trackStarted = true;
                }
                writePoint(point);
            }
            if (trackStarted) {
                writeTrackPartEnd();
            }
            writeEnd();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            this.writer.close();
        }
    }

    protected abstract void writeStart() throws IOException;

    protected abstract void writeTrackPartStart(String trackPartName) throws IOException;

    protected abstract void writePoint(TrackPoint point) throws IOException;

    protected abstract void writeTrackPartEnd() throws IOException;

    protected abstract void writeEnd() throws IOException;
}
