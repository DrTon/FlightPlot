package me.drton.flightplot.export;

import me.drton.flightplot.FormatErrorException;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Created by ada on 19.01.14.
 */
public class ExportRunner implements Runnable {

    private TrackReader reader;

    private TrackExporter exporter;

    private String statusMessage;

    private Runnable finishedCallback;

    private File destination;

    public ExportRunner(TrackReader reader, TrackExporter exporter, File destination) {
        this.reader = reader;
        this.exporter = exporter;
        this.destination = destination;
    }

    @Override
    public void run() {
        try {
            doExport(this.destination);
        } catch (Exception e) {
            this.statusMessage = "Error: " + e;
            e.printStackTrace();
        }
        finish();
    }

    private void finish() {
        if (null != this.finishedCallback) {
            this.finishedCallback.run();
        }
    }

    private void doExport(File exportFile) throws IOException, FormatErrorException {
        // get time of first point to use it as track title
        TrackPoint point = this.reader.readNextPoint();
        this.reader.reset();
        if (null != point) {
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String trackTitle = dateFormat.format(point.time) + " UTC";
            this.exporter.exportToFile(exportFile, trackTitle);
            this.statusMessage = String.format("Successfully exported track %s to %s", trackTitle,
                    exportFile.getAbsoluteFile());
        } else {
            this.statusMessage = String.format("Couldn't find any data to export");
        }
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setFinishedCallback(Runnable finishedCallback) {
        this.finishedCallback = finishedCallback;
    }
}
