package me.drton.flightplot;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;

/**
* Created by ton on 13.03.15.
*/
class ColorParamTableCellEditor extends AbstractCellEditor implements TableCellEditor {
    private ColorSupplier colorSupplier;
    private Color color;
    private JComboBox select;

    public ColorParamTableCellEditor(ColorSupplier colorSupplier) {
        this.colorSupplier = colorSupplier;
        select = new JComboBox();
        select.setRenderer(new ColorCellRenderer());
        for (Paint paint : colorSupplier.getPaintSequence()) {
            select.addItem(paint);
        }
    }

    public JComboBox getComponent() {
        return select;
    }

    @Override
    public Object getCellEditorValue() {
        return colorSupplier.getPaint(select.getSelectedIndex());
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        color = (Color) value;
        select.setSelectedItem(color);
        return select;
    }

    private class ColorCellRenderer extends JLabel implements ListCellRenderer {
        boolean setBg = false;

        public ColorCellRenderer() {
            setOpaque(true);
            setPreferredSize(new Dimension(0, 15));
        }

        @Override
        public void setBackground(Color bg) {
            if (!setBg) {
                return;
            }
            super.setBackground(bg);
        }

        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            setBg = true;
            setText("");
            setBackground((Color) value);
            setBorder(BorderFactory.createEmptyBorder());
            setBg = false;

            if (isSelected) {
                setBorder(BorderFactory.createLineBorder(Color.white, 2));
            }

            return this;
        }
    }
}
