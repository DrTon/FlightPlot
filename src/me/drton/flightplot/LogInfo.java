package me.drton.flightplot;

import me.drton.jmavlib.log.LogReader;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
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
    private DateFormat dateFormat;

    public LogInfo() {
        mainFrame = new JFrame("Log Info");
        mainFrame.setContentPane(mainPanel);
        mainFrame.pack();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
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
            infoTableModel.addRow(new Object[]{"System", logReader.getSystemName()});
            infoTableModel.addRow(new Object[]{
                    "Length, s", String.format(Locale.ROOT, "%.3f", logReader.getSizeMicroseconds() * 1e-6)});
            String startTimeStr = "";
            if (logReader.getUTCTimeReferenceMicroseconds() > 0) {
                startTimeStr = dateFormat.format(
                        new Date((logReader.getStartMicroseconds() + logReader.getUTCTimeReferenceMicroseconds()) / 1000)) + " UTC";
            }
            infoTableModel.addRow(new Object[]{
                    "Start Time", startTimeStr});
            infoTableModel.addRow(new Object[]{"Updates count", logReader.getSizeUpdates()});
            infoTableModel.addRow(new Object[]{"Errors", logReader.getErrors().size()});
            Map<String, Object> ver = logReader.getVersion();
            infoTableModel.addRow(new Object[]{"Hardware Version", ver.get("HW")});
            infoTableModel.addRow(new Object[]{"Firmware Version", ver.get("FW")});
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
