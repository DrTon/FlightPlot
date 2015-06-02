package me.drton.flightplot.export;

import me.drton.flightplot.PreferencesUtil;
import me.drton.jmavlib.log.FormatErrorException;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.IOException;
import java.util.prefs.Preferences;

/**
 * Created by ada on 25.01.14.
 */
public class ExportManager {

    private static final String DIALOG_SETTING = "ExportDialog";
    private static final String EXPORTER_CONFIGURATION_SETTING = "ExporterConfiguration";
    private static final String READER_CONFIGURATION_SETTING = "ReaderConfiguration";
    private static final String LAST_EXPORT_DIRECTORY_SETTING = "LastExportDirectory";

    private File lastExportDirectory;
    private ExportConfigurationDialog dialog;
    private ExportRunner runner;
    private PreferencesUtil preferencesUtil;

    public ExportManager() {
        this.preferencesUtil = new PreferencesUtil();
        this.dialog = new ExportConfigurationDialog();
    }

    public boolean export(ExportData exportData, Runnable finishedCallback)
            throws IOException, FormatErrorException, ConfigurationException {
        if (showConfigurationDialog(exportData)) {
            ExportFormat exportFormat = this.dialog.getExporterConfiguration().getExportFormat();
            File destination = getExportDestination(exportFormat);

            if (null != destination) {
                TrackReader trackReader = TrackReaderFactory.getTrackReader(exportData.getLogReader());
                TrackExporter exporter = exportFormat.getTrackExporter(trackReader);
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

    private boolean showConfigurationDialog(ExportData exportData) {
        this.dialog.display(exportData);
        return !this.dialog.isCanceled();
    }

    private File getExportDestination(ExportFormat exportFormat) {
        JFileChooser fc = new JFileChooser();
        if (lastExportDirectory != null) {
            fc.setCurrentDirectory(lastExportDirectory);
        }
        FileNameExtensionFilter extensionFilter = new FileNameExtensionFilter(exportFormat.getFileExtensionName(),
                exportFormat.getFileExtension());
        fc.setFileFilter(extensionFilter);
        fc.setDialogTitle("Export Track");
        int returnVal = fc.showDialog(null, "Export");
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            lastExportDirectory = fc.getCurrentDirectory();
            String exportFileName = fc.getSelectedFile().toString();
            String exportFileExtension = extensionFilter.getExtensions()[0];
            if (extensionFilter == fc.getFileFilter() && !exportFileName.toLowerCase().endsWith(exportFileExtension)) {
                exportFileName +=  ( "." + exportFileExtension);
            }
            File exportFile = new File(exportFileName);
            if (!exportFile.exists()) {
                return exportFile;
            } else {
                int result = JOptionPane.showConfirmDialog(null,
                        "Do you want to overwrite the existing file?" + "\n" + exportFile.getAbsoluteFile(),
                        "File already exists", JOptionPane.YES_NO_OPTION);
                if (JOptionPane.YES_OPTION == result) {
                    return exportFile;
                }
            }
        }
        return null;
    }

    public String getLastStatusMessage() {
        if (null != this.runner) {
            return this.runner.getStatusMessage();
        }
        return "";
    }

    public void savePreferences(Preferences preferences) {
        this.preferencesUtil.saveWindowPreferences(this.dialog, preferences.node(DIALOG_SETTING));
        this.dialog.getExporterConfiguration().saveConfiguration(preferences.node(EXPORTER_CONFIGURATION_SETTING));
        this.dialog.getReaderConfiguration().saveConfiguration(preferences.node(READER_CONFIGURATION_SETTING));
        if (this.lastExportDirectory != null) {
            preferences.put(LAST_EXPORT_DIRECTORY_SETTING, this.lastExportDirectory.getAbsolutePath());
        }
    }

    public void loadPreferences(Preferences preferences) {
        this.preferencesUtil.loadWindowPreferences(this.dialog, preferences.node(DIALOG_SETTING), -1, -1);
        this.dialog.getExporterConfiguration().loadConfiguration(preferences.node(EXPORTER_CONFIGURATION_SETTING));
        this.dialog.getReaderConfiguration().loadConfiguration(preferences.node(READER_CONFIGURATION_SETTING));
        String lastExportDirectoryPath = preferences.get(LAST_EXPORT_DIRECTORY_SETTING, null);
        if (null != lastExportDirectoryPath) {
            this.lastExportDirectory = new File(lastExportDirectoryPath);
        }
    }
}
