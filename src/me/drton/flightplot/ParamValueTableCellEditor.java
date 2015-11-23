package me.drton.flightplot;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.EventObject;

/**
* Created by ton on 13.03.15.
*/
class ParamValueTableCellEditor extends AbstractCellEditor implements TableCellEditor {
    private FlightPlot app;
    private TableCellEditor editor;

    public ParamValueTableCellEditor(FlightPlot app) {
        this.app = app;
    }

    public boolean isCellEditable(EventObject anEvent) {
        if (anEvent instanceof MouseEvent) {
            return ((MouseEvent)anEvent).getClickCount() >= 2;
        }
        return true;
    }

    @Override
    public Object getCellEditorValue() {
        return editor != null ? editor.getCellEditorValue() : null;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        app.setEditingProcessor();
        if (value instanceof Color) {
            editor = new ColorParamTableCellEditor(app.getColorSupplier());
            ((ColorParamTableCellEditor) editor).getComponent().addActionListener(new ActionDelegate());
        } else if (value instanceof String) {
            JTextField textField = new JTextField();
            textField.setFont(table.getFont());
            textField.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            editor = new DefaultCellEditor(textField);
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
