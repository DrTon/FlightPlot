package me.drton.flightplot;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.*;

/**
 * User: ton Date: 27.10.13 Time: 17:45
 */
public class LogInfo {
    private JFrame mainFrame;
    private JPanel mainPanel;
    private JTable infoTable;
    private DefaultTableModel infoTableModel;
    private JTable parametersTable;
    private DefaultTableModel parametersTableModel;

    public LogInfo() {
        mainFrame = new JFrame("Log Info");
        mainFrame.setContentPane(mainPanel);
        mainFrame.pack();
    }

    public JFrame getFrame() {
        return mainFrame;
    }

    public void setVisible(boolean visible) {
        mainFrame.setVisible(visible);
    }

    public void updateInfo(LogReader logReader) {
        while (infoTableModel.getRowCount() > 0) {
            infoTableModel.removeRow(0);
        }
        while (parametersTableModel.getRowCount() > 0) {
            parametersTableModel.removeRow(0);
        }
        if (logReader != null) {
            infoTableModel.addRow(new Object[]{"Format", logReader.getFormat()});
            infoTableModel.addRow(new Object[]{
                    "Length, s", String.format(Locale.ROOT, "%.3f", logReader.getSizeMicroseconds() * 1e-6)});
            infoTableModel.addRow(new Object[]{"Updates count", logReader.getSizeUpdates()});
            Map<String, Object> ver = logReader.getVersion();
            infoTableModel.addRow(new Object[]{"Hardware Version", ver.get("Arch")});
            infoTableModel.addRow(new Object[]{"Firmware Version", ver.get("FwGit")});
            Map<String, Object> parameters = logReader.getParameters();
            List<String> keys = new ArrayList<String>(parameters.keySet());
            Collections.sort(keys);
            for (String key : keys) {
                parametersTableModel.addRow(new Object[]{key, parameters.get(key).toString()});
            }
        }
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
        // Parameters table
        parametersTableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        parametersTableModel.addColumn("Parameter");
        parametersTableModel.addColumn("Value");
        parametersTable = new JTable(parametersTableModel);
    }
}
