package me.drton.flightplot;

import me.drton.jmavlib.log.LogReader;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LogInfoDialogBackup extends JDialog {
    private JPanel contentPane;
    private JButton buttonClose;
    private JTable infoTable;
    private DefaultTableModel infoTableModel;

    public LogInfoDialogBackup() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonClose);
        buttonClose.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onClose();
            }
        });
    }

    public void updateInfo(LogReader logReader) {
        while (infoTableModel.getRowCount() > 0) {
            infoTableModel.removeRow(0);
        }
        String sizeSeconds;
        try {
            sizeSeconds = String.format("%.3f", logReader.getSizeMicroseconds() * 1e-6);
        } catch (Exception e) {
            sizeSeconds = "N/A";
        }
        String sizeUpdates;
        try {
            sizeUpdates = Long.toString(logReader.getSizeUpdates());
        } catch (Exception e) {
            sizeUpdates = "N/A";
        }
        infoTableModel.addRow(new Object[]{"Length, seconds", sizeSeconds});
        infoTableModel.addRow(new Object[]{"Updates count", sizeUpdates});
    }

    private void onClose() {
        setVisible(false);
    }

    private void createUIComponents() {
        // Info table
        infoTableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        infoTableModel.addColumn("Property");
        infoTableModel.addColumn("Value");
        infoTable = new JTable(infoTableModel);
    }
}
