package me.drton.flightplot.export;

import me.drton.flightplot.PreferencesUtil;
import me.drton.jmavlib.log.FormatErrorException;
import me.drton.jmavlib.log.LogReader;
import org.jfree.data.Range;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.prefs.Preferences;

public class TrackExportDialog extends JDialog {
    private static final String DIALOG_SETTING = "TrackExportDialog";
    private static final String EXPORTER_CONFIGURATION_SETTING = "ExporterConfiguration";
    private static final String READER_CONFIGURATION_SETTING = "ReaderConfiguration";
    private static final String LAST_EXPORT_DIRECTORY_SETTING = "LastExportDirectory";

    private JPanel contentPane;
    private JButton buttonExport;
    private JCheckBox splitTrackByFlightCheckBox;
    private JComboBox exportFormatComboBox;
    private JSlider samplesPerSecond;
    private JLabel samplesPerSecondValue;
    private JLabel logEndTimeValue;
    private JTextField timeEndField;
    private JTextField timeStartField;
    private JLabel timeStartLabel;
    private JLabel timeEndLabel;
    private JTextField altOffsetField;
    private JLabel statusLabel;
    private JButton buttonClose;
    private JCheckBox exportDataInChartCheckBox;
    private File lastExportDirectory;
    private Map<String, TrackExporter> exporters;

    private TrackExporterConfiguration exporterConfiguration = new TrackExporterConfiguration();
    private TrackReaderConfiguration readerConfiguration = new TrackReaderConfiguration();
    private LogReader logReader;
    private Range chartRange;

    private class ExportFormatItem {
        String description;
        String formatName;

        ExportFormatItem(String description, String formatName) {
            this.description = description;
            this.formatName = formatName;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    public TrackExportDialog(Map<String, TrackExporter> exporters) {
        this.exporters = exporters;
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonExport);
        setTitle("Export settings");
        initExportersList(exporters);
        initSampleSlider();
        buttonExport.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                export();
            }
        });
        buttonClose.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onClose();
            }
        });
        // call onClose() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onClose();
            }
        });
        // call onClose() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onClose();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        samplesPerSecond.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent changeEvent) {
                updateForSamplesPerSecond();
            }
        });
        exportDataInChartCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                validateTimeRange(null);
            }
        });
        logEndTimeValue.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                if (!exportDataInChartCheckBox.isSelected()) {
                    timeEndField.setText(stringFromMicroseconds(logReader.getSizeMicroseconds()));
                }
            }
        });

        DocumentListener timeChangedListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                validateTimeRange(null);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                validateTimeRange(null);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                validateTimeRange(null);
            }
        };
        timeStartField.getDocument().addDocumentListener(timeChangedListener);
        timeEndField.getDocument().addDocumentListener(timeChangedListener);
        pack();
    }

    private void initExportersList(Map<String, TrackExporter> exporters) {
        for (TrackExporter exporter : exporters.values()) {
            exportFormatComboBox.addItem(new ExportFormatItem(exporter.getDescription(), exporter.getName()));
        }
    }

    private void initSampleSlider() {
        Dictionary<Integer, JLabel> labels = new Hashtable<Integer, JLabel>();
        labels.put(1, new JLabel("0.1"));
        labels.put(10, new JLabel("1"));
        labels.put(19, new JLabel("10"));
        samplesPerSecond.setLabelTable(labels);
    }

    private String stringFromMicroseconds(long us) {
        return String.format(Locale.ROOT, "%.3f", us / 1000000.0);
    }

    private long getTimeInterval() {
        int value = samplesPerSecond.getValue();
        if (value <= 10) {
            return 10000000 / value;
        } else if (value == 20) {
            return 0;
        } else {
            return 1000000 / (value - 9);
        }
    }

    private void setTimeInterval(long interval) {
        if (interval == 0) {
            samplesPerSecond.setValue(20);
        } else if (interval <= 1000000) {
            samplesPerSecond.setValue((int) (1000000 / interval + 9));
        } else {
            samplesPerSecond.setValue((int) (10000000 / interval));
        }
        updateForSamplesPerSecond();
    }

    private void updateForSamplesPerSecond() {
        if (getTimeInterval() == 0) {
            samplesPerSecondValue.setText("max");
        } else {
            samplesPerSecondValue.setText(String.format(Locale.ROOT, "%.1f", 1000000.0 / getTimeInterval()));
        }
    }

    private String formatTime(long time) {
        long s = time / 1000000;
        long ms = (time / 1000) % 1000;
        return String.format(Locale.ROOT, "%02d:%02d:%02d.%03d",
                (int) (s / 3600), s / 60 % 60, s % 60, ms);
    }

    public void display(LogReader logReader, Range chartRange) {
        if (logReader == null) {
            throw new RuntimeException("Log not opened");
        }
        this.logReader = logReader;
        this.chartRange = chartRange;
        readerConfiguration.setTimeStart(logReader.getStartMicroseconds());
        readerConfiguration.setTimeEnd(logReader.getStartMicroseconds() + logReader.getSizeMicroseconds());
        updateDialogFromConfiguration();
        setVisible(true);
    }

    private double getLogSizeInSeconds() {
        return logReader.getSizeMicroseconds() / 1000000.0;
    }

    private File getDestinationFile(String extension, String description) {
        JFileChooser fc = new JFileChooser();
        if (lastExportDirectory != null) {
            fc.setCurrentDirectory(lastExportDirectory);
        }
        FileNameExtensionFilter extensionFilter = new FileNameExtensionFilter(description, extension);
        fc.setFileFilter(extensionFilter);
        fc.setDialogTitle("Export Track");
        int returnVal = fc.showDialog(null, "Export");
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            lastExportDirectory = fc.getCurrentDirectory();
            String exportFileName = fc.getSelectedFile().toString();
            String exportFileExtension = extensionFilter.getExtensions()[0];
            if (extensionFilter == fc.getFileFilter() && !exportFileName.toLowerCase().endsWith(exportFileExtension)) {
                exportFileName += ("." + exportFileExtension);
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

    private void export() {
        updateConfiguration();
        final TrackExporter exporter = exporters.get(exporterConfiguration.getExportFormat());
        if (exporter != null) {
            final File file = getDestinationFile(exporter.getFileExtension(), exporter.getDescription());
            if (null != file) {
                setStatus("Exporting...", false);
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            TrackReader trackReader = TrackReaderFactory.getTrackReader(logReader, readerConfiguration);
                            String trackTitle = "Track";
                            exporter.export(trackReader, exporterConfiguration, file, trackTitle);
                            setStatus(String.format("Exported to \"%s\"", file), false);
                        } catch (Exception e) {
                            setStatus(String.format("Export failed: %s", e.getMessage()), true);
                            e.printStackTrace();
                        }
                    }
                });
            }
        }
    }

    private void setStatus(String status, boolean error) {
        statusLabel.setText(status);
        if (error) {
            statusLabel.setForeground(Color.RED);
        } else {
            statusLabel.setForeground(Color.BLACK);
        }
    }

    private Long parseExportTime(JTextField field, JLabel label) {
        try {
            long time = (long) (Double.parseDouble(field.getText()) * 1000000);
            label.setText(formatTime(time));
            return time;
        } catch (NumberFormatException e) {
            label.setText("-");
            return null;
        }
    }

    private boolean validateTimeRange(TrackReaderConfiguration configuration) {
        String errorMsg = null;
        if (exportDataInChartCheckBox.isSelected()) {
            timeStartField.setEnabled(false);
            timeEndField.setEnabled(false);
            timeStartLabel.setText(formatTime((long) (chartRange.getLowerBound() * 1000000) - logReader.getStartMicroseconds()));
            timeEndLabel.setText(formatTime((long) (chartRange.getUpperBound() * 1000000) - logReader.getStartMicroseconds()));
        } else {
            timeStartField.setEnabled(true);
            timeEndField.setEnabled(true);
            Long timeStart;
            Long timeEnd;
            timeStart = parseExportTime(timeStartField, timeStartLabel);
            timeEnd = parseExportTime(timeEndField, timeEndLabel);
            if (timeStart == null || timeEnd == null) {
                errorMsg = "Invalid export time format";
            } else {
                if (timeStart < 0 || timeEnd <= timeStart) {
                    errorMsg = "Invalid export time range";
                } else if (configuration != null) {
                    configuration.setTimeStart(timeStart + logReader.getStartMicroseconds());
                    configuration.setTimeEnd(timeEnd + logReader.getStartMicroseconds());
                }
            }
        }
        if (errorMsg != null) {
            buttonExport.setEnabled(false);
            setStatus(errorMsg, true);
            return false;
        } else {
            buttonExport.setEnabled(true);
            setStatus("Ready to export", false);
            return true;
        }
    }

    private boolean updateConfiguration() {
        String errorMsg = null;
        exporterConfiguration.setSplitTracksByFlightMode(splitTrackByFlightCheckBox.isSelected());
        ExportFormatItem item = (ExportFormatItem) exportFormatComboBox.getSelectedItem();
        exporterConfiguration.setExportFormat(item.formatName);

        readerConfiguration.setTimeInterval(getTimeInterval());
        if (exportDataInChartCheckBox.isSelected()) {
            readerConfiguration.setTimeStart((long) (chartRange.getLowerBound() * 1000000));
            readerConfiguration.setTimeEnd((long) (chartRange.getUpperBound() * 1000000));
        } else {
            if (!validateTimeRange(readerConfiguration)) {
                return false;
            }
        }
        double altOffset;
        try {
            altOffset = Double.parseDouble(altOffsetField.getText());
            readerConfiguration.setAltitudeOffset(altOffset);
        } catch (NumberFormatException e) {
            errorMsg = "Invalid altitude offset format";
        }
        if (errorMsg != null) {
            buttonExport.setEnabled(false);
            setStatus(errorMsg, true);
            return false;
        } else {
            buttonExport.setEnabled(true);
            setStatus("Ready to export", false);
            return true;
        }
    }

    private void updateDialogFromConfiguration() {
        splitTrackByFlightCheckBox.setSelected(exporterConfiguration.isSplitTracksByFlightMode());
        if (exporterConfiguration.getExportFormat() != null) {
            for (int index = 0; index < exportFormatComboBox.getItemCount(); index++) {
                ExportFormatItem item = (ExportFormatItem) exportFormatComboBox.getItemAt(index);
                if (exporterConfiguration.getExportFormat().equals(item.formatName)) {
                    exportFormatComboBox.setSelectedIndex(index);
                    break;
                }
            }
        }
        timeStartField.setText(stringFromMicroseconds(readerConfiguration.getTimeStart() - logReader.getStartMicroseconds()));
        timeEndField.setText(stringFromMicroseconds(readerConfiguration.getTimeEnd() - logReader.getStartMicroseconds()));
        altOffsetField.setText(String.valueOf(readerConfiguration.getAltitudeOffset()));

        setTimeInterval(readerConfiguration.getTimeInterval());
        logEndTimeValue.setText(String.format(" (log end: %s)", stringFromMicroseconds(logReader.getSizeMicroseconds())));
        validateTimeRange(null);
    }

    private void onClose() {
        dispose();
    }

    public void savePreferences(Preferences preferences) {
        PreferencesUtil.saveWindowPreferences(this, preferences.node(DIALOG_SETTING));
        exporterConfiguration.saveConfiguration(preferences.node(EXPORTER_CONFIGURATION_SETTING));
        readerConfiguration.saveConfiguration(preferences.node(READER_CONFIGURATION_SETTING));
        if (lastExportDirectory != null) {
            preferences.put(LAST_EXPORT_DIRECTORY_SETTING, lastExportDirectory.getAbsolutePath());
        }
    }

    public void loadPreferences(Preferences preferences) {
        PreferencesUtil.loadWindowPreferences(this, preferences.node(DIALOG_SETTING), -1, -1);
        exporterConfiguration.loadConfiguration(preferences.node(EXPORTER_CONFIGURATION_SETTING));
        readerConfiguration.loadConfiguration(preferences.node(READER_CONFIGURATION_SETTING));
        String lastExportDirectoryPath = preferences.get(LAST_EXPORT_DIRECTORY_SETTING, null);
        if (null != lastExportDirectoryPath) {
            lastExportDirectory = new File(lastExportDirectoryPath);
        }
    }
}
