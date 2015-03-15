package me.drton.flightplot;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
* Created by ton on 13.03.15.
*/
class ParamValueTableCellEditor extends AbstractCellEditor implements TableCellEditor {
    private ColorSupplier colorSupplier;
    private TableCellEditor editor;

    public ParamValueTableCellEditor(ColorSupplier colorSupplier) {
        this.colorSupplier = colorSupplier;
    }

    @Override
    public Object getCellEditorValue() {
        return editor != null ? editor.getCellEditorValue() : null;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        if (value instanceof Color) {
            editor = new ColorParamTableCellEditor(colorSupplier);
            ((ColorParamTableCellEditor) editor).getComponent().addActionListener(new ActionDelegate());
        } else if (value instanceof String) {
            editor = new DefaultCellEditor(new JTextField());
            ((JTextField) ((DefaultCellEditor) editor).getComponent()).addActionListener(new ActionDelegate());
        }

        return editor.getTableCellEditorComponent(table, value, isSelected, row, column);
    }

    private class ActionDelegate implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            ParamValueTableCellEditor.this.stopCellEditing();
        }
    }
}
