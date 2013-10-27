package me.drton.flightplot;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.*;
import java.util.*;

public class FieldsListDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonAdd;
    private JTable fieldsTable;
    private JButton buttonClose;
    private DefaultTableModel fieldsTableModel;
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
        formatNames.put("c", "int16 * 1e-2");
        formatNames.put("C", "uint16 * 1e-2");
        formatNames.put("e", "int32 * 1e-2");
        formatNames.put("E", "uint32 * 1e-2");
        formatNames.put("n", "char[4]");
        formatNames.put("N", "char[16]");
        formatNames.put("Z", "char[64]");
        formatNames.put("M", "uint8 (mode)");
    }

    public FieldsListDialog(final Runnable callbackAdd) {
        setContentPane(contentPane);
        setModal(true);
        setTitle("Fields List");
        getRootPane().setDefaultButton(buttonAdd);
        buttonAdd.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                callbackAdd.run();
            }
        });
        buttonClose.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onClose();
            }
        });
        // call onClose() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onClose();
            }
        });
        // call onClose() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onClose();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void onClose() {
        setVisible(false);
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
