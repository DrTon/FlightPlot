package me.drton.flightplot.export;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.*;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Locale;

public class ExportConfigurationDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JCheckBox splitTrackByFlightCheckBox;
    private JComboBox exportFormat;
    private JSlider samplesPerSecond;
    private JLabel samplesPerSecondValue;
    private JLabel maxTimeValue;
    private JTextField exportTimeTo;
    private JTextField exportTimeFrom;
    private JCheckBox exportDataInRange;
    private JLabel exportTimeFromLabel;
    private JLabel exportTimeToLabel;
    private JTextField altOffsetTextField;
    private JLabel altOffsetLabel;

    private String exportTimeToHold = null;
    private String exportTimeFromHold = null;

    private boolean canceled;
    private ExporterConfiguration exporterConfiguration = new ExporterConfiguration();
    private ReaderConfiguration readerConfiguration = new ReaderConfiguration();
    private ExportData exportData;

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }

    private class FormatItem {
        String displayName;
        String name;

        FormatItem(String displayName, String name) {
            this.displayName = displayName;
            this.name = name;
        }

        @Override
        public String toString() {
            return this.displayName;
        }
    }

    public ExportConfigurationDialog() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        setTitle("Export settings");
        initGuiElements();
        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });
        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });
        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });
        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        samplesPerSecond.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent changeEvent) {
                updateForSamplesPerSecond();
            }
        });
        exportDataInRange.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                updateForExportDataInRange();
            }
        });
        maxTimeValue.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                setMaxExportTimeTo();
            }
        });
        exportTimeFrom.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent keyEvent) {
                updateExportTimeFrom();
            }

            @Override
            public void keyPressed(KeyEvent keyEvent) {
                updateExportTimeFrom();
            }

            @Override
            public void keyReleased(KeyEvent keyEvent) {
                updateExportTimeFrom();
            }
        });
        exportTimeTo.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent keyEvent) {
                updateExportTimeTo();
            }

            @Override
            public void keyPressed(KeyEvent keyEvent) {
                updateExportTimeTo();
            }

            @Override
            public void keyReleased(KeyEvent keyEvent) {
                updateExportTimeTo();
            }
        });
    }

    private void initGuiElements() {
        initFormatList();
        initSampleSlider();
    }

    private void initFormatList() {
        for (ExportFormatFactory.ExportFormatType type : ExportFormatFactory.ExportFormatType.values()) {
            this.exportFormat.addItem(new FormatItem(type.getExportFormat().getFormatName(), type.name()));
        }
    }

    private void initSampleSlider() {
        Dictionary<Integer, JLabel> labels = new Hashtable<Integer, JLabel>();
        labels.put(1, new JLabel("0.1"));
        labels.put(10, new JLabel("1"));
        labels.put(19, new JLabel("10"));
        this.samplesPerSecond.setLabelTable(labels);
    }

    private double getSamplesPerSecond() {
        int value = this.samplesPerSecond.getValue();
        if (value <= 10) {
            return (double) value / 10;
        } else if (value == 20) {
            return Double.MAX_VALUE;
        } else {
            return value - 9;
        }
    }

    private void setSamplesPerSecond(double value) {
        if (Double.MAX_VALUE == value) {
            this.samplesPerSecond.setValue(20);
        } else if (value <= 1) {
            this.samplesPerSecond.setValue((int) (value * 10));
        } else {
            this.samplesPerSecond.setValue((int) value + 9);
        }
        updateForSamplesPerSecond();
    }

    private void updateForSamplesPerSecond() {
        if (getSamplesPerSecond() == Double.MAX_VALUE) {
            this.samplesPerSecondValue.setText("max");
        } else {
            this.samplesPerSecondValue.setText(
                    String.format(Locale.ROOT, "%.1f", getSamplesPerSecond()));
        }
    }

    private void updateForExportDataInRange() {
        if (exportDataInRange.isSelected()) {
            this.exportTimeFrom.setText(
                    String.valueOf(this.exportData.getChartRangeFromInSeconds())
            );
            this.exportTimeTo.setText(
                    String.valueOf(this.exportData.getChartRangeToInSeconds())
            );
            this.exportTimeFrom.setEnabled(false);
            this.exportTimeTo.setEnabled(false);
        } else {
            if (null == this.exportTimeFromHold) {
                this.exportTimeFrom.setText(String.valueOf(0));
            } else {
                this.exportTimeFrom.setText(this.exportTimeFromHold);
            }
            if (null == exportTimeToHold) {
                this.exportTimeTo.setText(String.valueOf(this.exportData.getLogSizeInSeconds()));
            } else {
                this.exportTimeTo.setText(this.exportTimeToHold);
            }
            this.exportTimeFrom.setEnabled(true);
            this.exportTimeTo.setEnabled(true);
        }
        updateExportTimeFromLabel();
        updateExportTimeToLabel();
    }

    private void setMaxExportTimeTo() {
        if (!this.exportDataInRange.isSelected()) {
            this.exportTimeTo.setText(String.valueOf(this.exportData.getLogSizeInSeconds()));
            this.exportTimeToHold = String.valueOf(this.exportData.getLogSizeInSeconds());
        }
    }

    private void updateExportTimeTo() {
        this.exportTimeToHold = this.exportTimeTo.getText();
        updateExportTimeToLabel();
    }

    private void updateExportTimeToLabel() {
        try {
            this.exportTimeToLabel.setText(String.format(Locale.ROOT, "%d:%02d:%02d"
                    , (int) (getNumberFromTextField(this.exportTimeTo) / 3600)
                    , getNumberFromTextField(this.exportTimeTo) / 60 % 60
                    , getNumberFromTextField(this.exportTimeTo) % 60
            ));
        } catch (NumberFormatException nfe) {
            this.exportTimeToLabel.setText("-");
        }
    }

    private void updateExportTimeFrom() {
        this.exportTimeFromHold = this.exportTimeFrom.getText();
        updateExportTimeFromLabel();
    }

    private void updateExportTimeFromLabel() {
        try {
            this.exportTimeFromLabel.setText(String.format(Locale.ROOT, "%d:%02d:%02d"
                    , (int) (getNumberFromTextField(this.exportTimeFrom) / 3600)
                    , getNumberFromTextField(this.exportTimeFrom) / 60 % 60
                    , getNumberFromTextField(this.exportTimeFrom) % 60
            ));
        } catch (NumberFormatException nfe) {
            this.exportTimeFromLabel.setText("-");
        }
    }

    public void display(ExportData exportData) {
        this.exportData = exportData;
        updateDialogFromConfiguration();
        pack();
        setVisible(true);
    }

    private void onOK() {
        this.canceled = false;
        if (isConfigValid()) {
            updateConfigurationFromDialog();
            dispose();
        }
    }

    private boolean isConfigValid() {
        try {
            long from = getNumberFromTextField(this.exportTimeFrom);
            long to = getNumberFromTextField(this.exportTimeTo);
            if (from < 0 || to <= from || to > this.exportData.getLogSizeInSeconds()) {
                JOptionPane.showMessageDialog(this, "Export range FROM must be greater or equal to 0, TO must be greater " +
                        "than FROM and smaller than MAX.", "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Export range FROM and TO must be valid numbers."
                    , "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
    }

    private void updateConfigurationFromDialog() {
        this.exporterConfiguration.setSplitTracksByFlightMode(this.splitTrackByFlightCheckBox.isSelected());
        this.exporterConfiguration.setAltitudeOffset(Float.valueOf(this.altOffsetTextField.getText()));
        FormatItem item = (FormatItem) this.exportFormat.getSelectedItem();
        this.exporterConfiguration.setExportFormatType(ExportFormatFactory.ExportFormatType.valueOf(item.name));

        this.readerConfiguration.setSamplesPerSecond(getSamplesPerSecond());
        this.readerConfiguration.setTimeToInSeconds(getNumberFromTextField(this.exportTimeTo));
        this.readerConfiguration.setTimeFromInSeconds(getNumberFromTextField(this.exportTimeFrom));
        this.readerConfiguration.setExportChartRangeOnly(this.exportDataInRange.isSelected());
    }

    private long getNumberFromTextField(final JTextField field) {
        return Long.parseLong(field.getText());
    }

    private void updateDialogFromConfiguration() {
        this.splitTrackByFlightCheckBox.setSelected(this.exporterConfiguration.isSplitTracksByFlightMode());
        if (null != this.exporterConfiguration.getExportFormatType()) {
            for (int index = 0; index < this.exportFormat.getItemCount(); index++) {
                FormatItem item = (FormatItem) this.exportFormat.getItemAt(index);
                if (this.exporterConfiguration.getExportFormatType().name().equals(item.name)) {
                    this.exportFormat.setSelectedIndex(index);
                    break;
                }
            }
        }
        setSamplesPerSecond(this.readerConfiguration.getSamplesPerSecond());
        this.maxTimeValue.setText(String.format(Locale.ROOT, " (max: %d)", this.exportData.getLogSizeInSeconds()));
        this.exportDataInRange.setSelected(this.readerConfiguration.isExportChartRangeOnly());
        updateForExportDataInRange();
    }

    private void onCancel() {
        this.canceled = true;
        dispose();
    }

    public ReaderConfiguration getReaderConfiguration() {
        return readerConfiguration;
    }

    public ExporterConfiguration getExporterConfiguration() {
        return exporterConfiguration;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void setExportData(ExportData exportData) {
        this.exportData = exportData;
    }
}
