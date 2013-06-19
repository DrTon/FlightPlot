package me.drton.flightplot;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

/**
 * User: ton Date: 19.06.13 Time: 13:13
 */
public class FieldsList {
    private JFrame frame;
    private JButton closeButton;
    private JButton addButton;
    private DefaultTableModel fieldsTableModel;
    private JTable fieldsTable;
    private JPanel mainPanel;
    private static Map<String, String> formatNames = new HashMap<String, String>();

    static {
        formatNames.put("b", "int8");
        formatNames.put("B", "uint8");
        formatNames.put("L", "int32 * 1e-7 (lat/lon)");
        formatNames.put("i", "int32");
        formatNames.put("I", "uint32");
        formatNames.put("q", "int64");
        formatNames.put("Q", "uint64");
        formatNames.put("f", "float");
    }

    public FieldsList(final Runnable callbackAdd) {
        frame = new JFrame("FieldsList");
        frame.setContentPane(mainPanel);
        frame.pack();
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                callbackAdd.run();
            }
        });
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                frame.setVisible(false);
            }
        });
    }

    public void setFieldsList(Map<String, String> fields) {
        while (fieldsTableModel.getRowCount() > 0) {
            fieldsTableModel.removeRow(0);
        }
        List<String> fieldsList = new ArrayList<String>(fields.keySet());
        Collections.sort(fieldsList);
        for (String field : fieldsList) {
            String formatChar = fields.get(field);
            String formatName = formatNames.get(formatChar);
            if (formatName == null)
                formatName = formatChar;
            fieldsTableModel.addRow(new Object[]{field, formatName});
        }
    }

    public List<String> getSelectedFields() {
        List<String> selectedFields = new ArrayList<String>();
        for (int i : fieldsTable.getSelectedRows()) {
            selectedFields.add((String) fieldsTableModel.getValueAt(i, 0));
        }
        return selectedFields;
    }

    public void display() {
        frame.setVisible(true);
    }

    private void createUIComponents() {
        // Fields table
        fieldsTableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        fieldsTableModel.addColumn("Field");
        fieldsTableModel.addColumn("Type");
        fieldsTable = new JTable(fieldsTableModel);
    }
}
