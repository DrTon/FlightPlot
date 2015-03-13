package me.drton.flightplot;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/**
* Created by ton on 13.03.15.
*/
class ParamValueTableCellRenderer extends JLabel implements TableCellRenderer {
    private DefaultTableCellRenderer defaultTableCellRenderer = new DefaultTableCellRenderer();

    public ParamValueTableCellRenderer() {
        setOpaque(true);
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected
            , boolean hasFocus, int row, int column) {
        if (value instanceof Color) {
            setBackground((Color) value);
        } else {
            return defaultTableCellRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
        return this;
    }
}
