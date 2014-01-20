package me.drton.flightplot.export;

import me.drton.flightplot.FormatErrorException;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Created by ada on 19.01.14.
 */
public class ExportRunner implements  Runnable {

    private FileNameExtensionFilter kmlExtensionFilter = new FileNameExtensionFilter("KML", ".kml");

    private TrackReader reader;

    private TrackExporter exporter;

    private File lastPresetDirectory;

    private String statusMessage;

    private Runnable finishedCallback;

    public ExportRunner(TrackReader reader, TrackExporter exporter){
        this.reader = reader;
        this.exporter = exporter;
    }

    @Override
    public void run() {
        if(reader.configureReader()
                && exporter.configureExporter()){
            File destination = getExportDestination();
            if(null != destination){
                try{
                    doExport(destination);
                    finish();
                    return;
                } catch (Exception e) {
                    this.statusMessage = "Error: " + e;
                    e.printStackTrace();
                }
            }
        }
        this.statusMessage = "Export canceled";
        finish();
    }

    private void finish(){
        if(null != this.finishedCallback){
            this.finishedCallback.run();
        }
    }

    private File getExportDestination(){
        JFileChooser fc = new JFileChooser();
        if (lastPresetDirectory != null)
            fc.setCurrentDirectory(lastPresetDirectory);
        fc.setFileFilter(kmlExtensionFilter);
        fc.setDialogTitle("Export Track");
        int returnVal = fc.showDialog(null, "Export");
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            lastPresetDirectory = fc.getCurrentDirectory();
            String exportFileName = fc.getSelectedFile().toString();
            String exportFileExtension = kmlExtensionFilter.getExtensions()[0];
            if (kmlExtensionFilter == fc.getFileFilter() && !exportFileName.toLowerCase().endsWith(exportFileExtension))
                exportFileName += exportFileExtension;
                File exportFile = new File(exportFileName);
                if (!exportFile.exists()) {
                    return exportFile;
                } else {
                    // TODO: ask for user approval to overwrite file
                    this.statusMessage = "File already exists, export aborted.";
                }
        }
        return null;
    }

    private void doExport(File exportFile) throws IOException, FormatErrorException {
        // get time of first point to use it as track title
        // TODO: < does this work if there was no GPS fix in the beginning?
        TrackPoint point = this.reader.readNextPoint();
        this.reader.reset();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String trackTitle = dateFormat.format(point.time) + " UTC";
        this.exporter.exportToFile(exportFile, trackTitle);
        this.statusMessage =
                String.format("Successfully exported track %s to %s", trackTitle, exportFile.getAbsoluteFile());
    }

    public File getLastPresetDirectory() {
        return lastPresetDirectory;
    }

    public void setLastPresetDirectory(File lastPresetDirectory) {
        this.lastPresetDirectory = lastPresetDirectory;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public Runnable getFinishedCallback() {
        return finishedCallback;
    }

    public void setFinishedCallback(Runnable finishedCallback) {
        this.finishedCallback = finishedCallback;
    }
}
