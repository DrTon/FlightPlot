package me.drton.flightplot;

import me.drton.flightplot.processors.PlotProcessor;

import javax.swing.*;
import java.awt.event.*;

public class AddProcessorDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField titleField;
    private JList processorTypesList;
    private DefaultListModel processorTypesListModel;
    private String[] processorTypes;

    private ProcessorPreset origProcessorPreset = null;
    private Runnable callback;

    public AddProcessorDialog(String[] processorTypes) {
        this.processorTypes = processorTypes;
        //this.processorTypes = processorTypes;
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
        return (String) processorTypesList.getSelectedValue();
    }

    public void display(Runnable callback, ProcessorPreset processorPreset) {
        if (processorTypesListModel.size() == 0) {
            for (String processorType : processorTypes) {
                processorTypesListModel.addElement(processorType);
            }
            processorTypesList.setSelectedValue("Simple", true);
        }
        this.callback = callback;
        if (processorPreset != null) {
            origProcessorPreset = processorPreset;
            titleField.setText(processorPreset.getTitle());
            processorTypesList.setSelectedValue(processorPreset.getProcessorType(), true);
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
        processorTypesListModel = new DefaultListModel();
        processorTypesList = new JList(processorTypesListModel);
    }
}
