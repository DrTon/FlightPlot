package me.drton.flightplot;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.table.DefaultTableModel;

public class FieldsListDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonAdd;
    private JTable fieldsTable;
    private JButton buttonClose;
    private DefaultTableModel fieldsTableModel;

    public FieldsListDialog(final Runnable callbackAdd) {
        setContentPane(contentPane);
        setModal(false);
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

        fieldsTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                if (me.getClickCount() == 2) {
                    callbackAdd.run();
                }
            }
        });
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
            fieldsTableModel.addRow(new Object[]{field, fields.get(field)});
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
