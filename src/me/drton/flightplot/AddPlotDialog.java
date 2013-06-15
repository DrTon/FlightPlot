package me.drton.flightplot;

import javax.swing.*;
import java.awt.event.*;

public class AddPlotDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField titleField;
    private JComboBox typeComboBox;
    private String[] processorsTypes;

    private FlightPlot app;

    public AddPlotDialog(String[] processorsTypes, FlightPlot app) {
        this.app = app;
        this.processorsTypes = processorsTypes;
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
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
        app.addPlot(titleField.getText(), typeComboBox.getSelectedItem().toString());
        dispose();
    }

    private void onCancel() {
        dispose();
    }

    private void createUIComponents() {
        typeComboBox = new JComboBox(processorsTypes);
    }
}
