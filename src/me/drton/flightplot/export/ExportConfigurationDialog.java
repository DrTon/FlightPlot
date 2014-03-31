package me.drton.flightplot.export;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.*;
import java.util.Dictionary;
import java.util.Hashtable;

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

    private boolean canceled;
    private ExporterConfiguration exporterConfiguration = new ExporterConfiguration();
    private ReaderConfiguration readerConfiguration = new ReaderConfiguration();
    private ExportData exportData;

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
                if (!ExportConfigurationDialog.this.exportDataInRange.isSelected()) {
                    ExportConfigurationDialog.this.exportTimeTo.setText(String.valueOf(
                            ExportConfigurationDialog.this.exportData.getLogReader().getSizeMicroseconds()));
                }
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

    private void updateForExportDataInRange() {
        if (exportDataInRange.isSelected()) {
            this.exportTimeFrom.setText(
                    String.valueOf(
                            Math.max(this.exportData.getChartRangeFrom()
                                    - this.exportData.getLogReader().getStartMicroseconds()
                                    , 0)
                    )
            );
            this.exportTimeTo.setText(
                    String.valueOf(
                            Math.min(this.exportData.getChartRangeTo()
                                    - this.exportData.getLogReader().getStartMicroseconds()
                                    , this.exportData.getLogReader().getSizeMicroseconds())
                    )
            );
            this.exportTimeFrom.setEnabled(false);
            this.exportTimeTo.setEnabled(false);
        } else {
            this.exportTimeFrom.setEnabled(true);
            this.exportTimeTo.setEnabled(true);
        }
    }

    private void updateForSamplesPerSecond() {
        if (getSamplesPerSecond() == Double.MAX_VALUE) {
            this.samplesPerSecondValue.setText("max");
        } else {
            this.samplesPerSecondValue.setText(
                    String.format("%.1f", getSamplesPerSecond()));
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
            long from = Long.parseLong(this.exportTimeFrom.getText());
            long to = Long.parseLong(this.exportTimeTo.getText());
            if (from < 0 || to <= from || to > this.exportData.getLogReader().getSizeMicroseconds()) {
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
        FormatItem item = (FormatItem) this.exportFormat.getSelectedItem();
        this.exporterConfiguration.setExportFormatType(ExportFormatFactory.ExportFormatType.valueOf(item.name));

        this.readerConfiguration.setSamplesPerSecond(getSamplesPerSecond());
        this.readerConfiguration.setTimeTo(getNumberFromTextField(this.exportTimeTo));
        this.readerConfiguration.setTimeFrom(getNumberFromTextField(this.exportTimeFrom));
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
        this.exportTimeFrom.setText(String.valueOf(0));
        this.exportTimeTo.setText(String.valueOf(this.exportData.getLogReader().getSizeMicroseconds()));
        this.maxTimeValue.setText(String.format(" (max: %d)", this.exportData.getLogReader().getSizeMicroseconds()));
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
