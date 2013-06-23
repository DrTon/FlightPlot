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

    private PlotProcessor origProcessor = null;
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

    public PlotProcessor getOrigProcessor() {
        return origProcessor;
    }

    public String getProcessorType() {
        return (String) processorTypeComboBox.getSelectedItem();
    }

    public void display(Runnable callback, PlotProcessor processor) {
        this.callback = callback;
        if (processor != null) {
            origProcessor = processor;
            titleField.setText(processor.getTitle());
            processorTypeComboBox.setSelectedItem(processor.getProcessorType());
        } else {
            origProcessor = null;
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
    }
}
