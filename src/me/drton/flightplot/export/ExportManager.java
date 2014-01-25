package me.drton.flightplot.export;

import me.drton.flightplot.FormatErrorException;
import me.drton.flightplot.LogReader;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.IOException;

/**
 * Created by ada on 25.01.14.
 */
public class ExportManager {

    private FileNameExtensionFilter kmlExtensionFilter = new FileNameExtensionFilter("KML", ".kml");

    private File lastPresetDirectory;

    private ExporterConfigurationDialog dialog;

    private ExportRunner runner;

    public ExportManager(){
        this.dialog = new ExporterConfigurationDialog();
        dialog.pack();
    }

    public boolean export(LogReader logReader, Runnable finishedCallback) throws IOException, FormatErrorException {
        if(showConfigurationDialog()){
            File destination = getExportDestination();
            if(null != destination){
                TrackReader trackReader = TrackReaderFactory.getTrackReader(logReader);
                KmlTrackExporter exporter = new KmlTrackExporter(trackReader);
                trackReader.setConfiguration(this.dialog.getReaderConfiguration());
                exporter.setConfiguration(this.dialog.getExporterConfiguration());
                this.runner = new ExportRunner(trackReader, exporter, destination);
                this.runner.setFinishedCallback(finishedCallback);
                new Thread(this.runner).start();
                return true;
            }
        }
        return false;
    }

    private boolean showConfigurationDialog(){
        dialog.setVisible(true);
        if(dialog.isCanceled()){
            return false;
        }
        else {
            // TODO: get config values
            return true;
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
                int result = JOptionPane.showConfirmDialog(null
                        , "Do you want to overwrite the existing file?"
                        + "\n" + exportFile.getAbsoluteFile()
                        , "File already exists", JOptionPane.YES_NO_OPTION);
                if(JOptionPane.YES_OPTION == result){
                    return exportFile;
                }
            }
        }
        return null;
    }

    public String getLastStatusMessage(){
        if(null != this.runner){
            return this.runner.getStatusMessage();
        }
        return "";
    }

    public File getLastPresetDirectory() {
        return lastPresetDirectory;
    }

    public void setLastPresetDirectory(File lastPresetDirectory) {
        this.lastPresetDirectory = lastPresetDirectory;
    }

}
