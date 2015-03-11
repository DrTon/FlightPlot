package me.drton.flightplot;

import me.drton.flightplot.processors.PlotProcessor;

import javax.swing.*;
import java.awt.event.*;

public class AddProcessorDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField titleField;
    private JComboBox processorTypeComboBox;
    private String[] processorsTypes;

    private ProcessorPreset origProcessorPreset = null;
    private Runnable callback;

    public AddProcessorDialog(String[] processorsTypes) {
        this.processorsTypes = processorsTypes;
        setContentPane(contentPane);
        setModal(true);
        setTitle("Add Processor");
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

    public String getProcessorTitle() {
        return titleField.getText();
    }

    public ProcessorPreset getOrigProcessorPreset() {
        return origProcessorPreset;
    }

    public String getProcessorType() {
        return (String) processorTypeComboBox.getSelectedItem();
    }

    public void display(Runnable callback, ProcessorPreset processorPreset) {
        this.callback = callback;
        if (processorPreset != null) {
            origProcessorPreset = processorPreset;
            titleField.setText(processorPreset.getTitle());
            processorTypeComboBox.setSelectedItem(processorPreset.getProcessorType());
        } else {
            origProcessorPreset = null;
            titleField.setText("");
        }
        titleField.requestFocus();
        this.setVisible(true);
    }

    private void onOK() {
        setVisible(false);
        callback.run();
    }

    private void onCancel() {
        setVisible(false);
    }

    private void createUIComponents() {
        processorTypeComboBox = new JComboBox(processorsTypes);
        processorTypeComboBox.setMaximumRowCount(20);
    }
}
