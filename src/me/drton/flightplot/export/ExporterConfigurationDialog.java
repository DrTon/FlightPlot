package me.drton.flightplot.export;

import javax.swing.*;
import java.awt.event.*;

public class ExporterConfigurationDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JCheckBox splitTrackByFlightCheckBox;
    private JComboBox exportFormat;

    private boolean canceled;
    private ExporterConfiguration exporterConfiguration = new ExporterConfiguration();
    private ReaderConfiguration readerConfiguration = new ReaderConfiguration();

    private class FormatItem{
        String displayName;
        String name;

        FormatItem(String displayName, String name){
            this.displayName = displayName;
            this.name = name;
        }

        @Override
        public String toString() {
            return this.displayName;
        }
    }

    public ExporterConfigurationDialog() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        setTitle("Export settings");
        initFormatList();

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
    }

    private void initFormatList(){
        this.exportFormat.addItem(new FormatItem("", ""));
        for(ExportFormatFactory.ExportFormatType type : ExportFormatFactory.ExportFormatType.values()){
            this.exportFormat.addItem(new FormatItem(type.getExportFormat().getFormatName(), type.name()));
        }
    }

    public void display(){
        updateDialogFromConfiguration();
        pack();
        setVisible(true);
    }

    private void onOK() {
        this.canceled = false;
        if(isConfigValid()){
            updateConfigurationFromDialog();
            dispose();
        }
    }

    private boolean isConfigValid(){
        if(this.exportFormat.getSelectedIndex() <= 0){
            JOptionPane.showMessageDialog(this, "You must select a export format.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    private void updateConfigurationFromDialog() {
        this.exporterConfiguration.setSplitTracksByFlightMode(this.splitTrackByFlightCheckBox.isSelected());
        if(this.exportFormat.getSelectedIndex() > 0){
            FormatItem item = (FormatItem)this.exportFormat.getSelectedItem();
            this.exporterConfiguration.setExportFormatType(ExportFormatFactory.ExportFormatType.valueOf(item.name));
        }
    }

    private void updateDialogFromConfiguration(){
        this.splitTrackByFlightCheckBox.setSelected(this.exporterConfiguration.isSplitTracksByFlightMode());
        if(null != this.exporterConfiguration.getExportFormatType()){
            for(int index = 0; index < this.exportFormat.getItemCount(); index ++){
                FormatItem item = (FormatItem)this.exportFormat.getItemAt(index);
                if(this.exporterConfiguration.getExportFormatType().name().equals(item.name)){
                    this.exportFormat.setSelectedIndex(index);
                    break;
                }
            }
        }
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
}
