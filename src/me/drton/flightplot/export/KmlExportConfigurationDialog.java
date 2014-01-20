package me.drton.flightplot.export;

import javax.swing.*;
import java.awt.event.*;

public class KmlExportConfigurationDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JCheckBox splitTrackByFlightCheckBox;

    private boolean canceled;
    private KmlExportConfiguration configuration = new KmlExportConfiguration();

    public KmlExportConfigurationDialog() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        setTitle("KML export settings");

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

    private void onOK() {
        this.canceled = false;
        updateConfigurationFromDialog();
        dispose();
    }

    private void updateConfigurationFromDialog() {
        this.configuration.setSplitTracksByFlightMode(this.splitTrackByFlightCheckBox.isSelected());
    }

    private void onCancel() {
        this.canceled = true;
        dispose();
    }

    public KmlExportConfiguration getConfiguration(){
        return this.configuration;
    }

    public boolean isCanceled() {
        return canceled;
    }
}
