package me.drton.flightplot;

import me.drton.flightplot.processors.PlotProcessor;
import me.drton.flightplot.processors.ProcessorsList;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * User: ton Date: 03.06.13 Time: 23:24
 */
public class FlightPlot {
    private JFrame frame;
    private JLabel statusLabel;
    private JPanel mainPanel;
    private JTable parametersTable;
    private DefaultTableModel parametersTableModel;
    private JPanel chartPanel;
    private JList<String> plotsList;
    private DefaultListModel<String> plotsListModel;
    private JButton addPlotButton;

    private static String appName = "FlightPlot";
    private String fileName = null;
    private LogReader logReader = null;
    private XYSeriesCollection dataset;
    private JFreeChart jFreeChart;
    private ProcessorsList processorsList;
    private Map<String, PlotProcessor> activeProcessors = new HashMap<String, PlotProcessor>();

    public static void main(String[] args)
            throws ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException,
                   IllegalAccessException {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new FlightPlot();
            }
        });
    }

    public FlightPlot() {
        frame = new JFrame(appName);
        frame.setContentPane(FlightPlot.this.mainPanel);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        createMenuBar();
        frame.setVisible(true);
        addPlotButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showAddPlotDialog();
            }
        });
    }

    private void createUIComponents() throws IllegalAccessException, InstantiationException {
        // Chart panel
        processorsList = new ProcessorsList();
        dataset = new XYSeriesCollection();
        jFreeChart = ChartFactory.createXYLineChart("", "", "", null, PlotOrientation.VERTICAL, true, true, false);
        jFreeChart.getXYPlot().setDataset(dataset);
        XYPlot plot = jFreeChart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        NumberAxis numberAxis = (NumberAxis) plot.getRangeAxis();
        numberAxis.setAutoRangeIncludesZero(false);
        chartPanel = new ChartPanel(jFreeChart);
        plotsListModel = new DefaultListModel<String>();
        plotsList = new JList<String>(plotsListModel);
        parametersTableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 1;
            }
        };
        parametersTableModel.addColumn("Parameter");
        parametersTableModel.addColumn("Value");
        parametersTable = new JTable(parametersTableModel) {
            // Workaround for bug to avoid starting edit on Fn or Cmd keys
            @Override
            protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
                boolean retValue = false;
                if (e.getKeyCode() != KeyEvent.VK_META || e.getKeyCode() != KeyEvent.VK_CONTROL ||
                        e.getKeyCode() != KeyEvent.VK_ALT) {
                    if (e.isControlDown() || e.isMetaDown() || e.isAltDown() || e.getKeyChar() == 0xFFFF) {
                        InputMap map = this.getInputMap(condition);
                        ActionMap am = getActionMap();
                        if (map != null && am != null && isEnabled()) {
                            Object binding = map.get(ks);
                            Action action = (binding == null) ? null : am.get(binding);
                            if (action != null) {
                                SwingUtilities.notifyAction(action, ks, e, this, e.getModifiers());
                                retValue = false;
                            } else {
                                try {
                                    JComponent ancestor = (JComponent) SwingUtilities.getAncestorOfClass(
                                            Class.forName("javax.swing.JComponent"), this);
                                    ancestor.dispatchEvent(e);
                                } catch (ClassNotFoundException err) {
                                    err.printStackTrace();
                                }
                            }
                        } else {
                            retValue = super.processKeyBinding(ks, e, condition, pressed);
                        }
                    } else {
                        retValue = super.processKeyBinding(ks, e, condition, pressed);
                    }
                }
                return retValue;
            }
        };
        plotsList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                showPlotParameters();
            }
        });
        parametersTableModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (e.getType() == TableModelEvent.UPDATE)
                    onParameterChanged();
            }
        });
        parametersTable.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "startEditing");
    }

    private void createMenuBar() {
        JMenu newMenu = new JMenu("File");
        JMenuItem fileOpenItem = new JMenuItem("Open...");
        fileOpenItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showOpenFileDialog();
            }
        });
        newMenu.add(fileOpenItem);
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(newMenu);
        frame.setJMenuBar(menuBar);
    }

    public void setStatus(String status) {
        statusLabel.setText(status);
    }

    public void showOpenFileDialog() {
        JFileChooser fc = new JFileChooser();
        int returnVal = fc.showDialog(frame, "Open");
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            fileName = file.getPath();
            frame.setTitle(appName + " - " + fileName);
            setAutoRange(true, true);
            processFile();
        }
    }

    private void processFile() {
        if (fileName != null) {
            setStatus("Processing...");
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    try {
                        logReader = new PX4LogReader(fileName);
                        generateSeries();
                        setStatus(" ");
                    } catch (IOException e) {
                        setStatus("Error: " + e);
                    }
                }
            });
        }
    }

    private void generateSeries() throws IOException {
        dataset.removeAllSeries();
        for (PlotProcessor processor : activeProcessors.values()) {
            processor.init();
        }
        Map<String, Object> data = new HashMap<String, Object>();
        while (true) {
            long t;
            try {
                t = logReader.readUpdate(data);
            } catch (EOFException e) {
                break;
            }
            for (PlotProcessor processor : activeProcessors.values()) {
                processor.process(t * 0.000001, data);
            }
        }
        for (PlotProcessor processor : activeProcessors.values()) {
            for (XYSeries series : (java.util.List<XYSeries>) processor.getSeriesCollection().getSeries()) {
                dataset.addSeries(series);
            }
        }
    }

    private void setAutoRange(boolean horizontal, boolean vertical) {
        if (horizontal)
            jFreeChart.getXYPlot().getDomainAxis().setAutoRange(true);
        if (vertical)
            jFreeChart.getXYPlot().getRangeAxis().setAutoRange(true);
    }

    private void showAddPlotDialog() {
        java.util.List<String> processors = new ArrayList<String>(processorsList.getProcessorsList());
        Collections.sort(processors);
        AddPlotDialog dialog = new AddPlotDialog(processors.toArray(new String[processors.size()]), this);
        dialog.pack();
        dialog.setVisible(true);
    }

    public void addPlot(String title, String processorType) {
        try {
            PlotProcessor processor = processorsList.getProcessorInstance(processorType);
            processor.setTitle(title);
            plotsListModel.addElement(title);
            activeProcessors.put(title, processor);
            plotsList.setSelectedValue(title, true);
            plotsList.repaint();
        } catch (Exception e) {
            setStatus("Error creating processor");
            e.printStackTrace();
        }
        processFile();
    }

    private void showPlotParameters() {
        while (parametersTableModel.getRowCount() > 0) {
            parametersTableModel.removeRow(0);
        }
        String selectedProcessor = plotsList.getSelectedValue();
        if (selectedProcessor != null) {
            Map<String, Object> params = activeProcessors.get(selectedProcessor).getParameters();
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                parametersTableModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
            }
        }
    }

    private void onParameterChanged() {
        String selectedProcessor = plotsList.getSelectedValue();
        if (selectedProcessor != null) {
            Map<String, Object> params = activeProcessors.get(selectedProcessor).getParameters();
            for (int i = 0; i < parametersTableModel.getRowCount(); i++) {
                String key = (String) parametersTableModel.getValueAt(i, 0);
                Object valueOld = params.get(key);
                String valueNewStr = parametersTableModel.getValueAt(i, 1).toString();
                Object valueNew;
                if (valueOld instanceof String) {
                    valueNew = valueNewStr;
                } else if (valueOld instanceof Double) {
                    valueNew = Double.parseDouble(valueNewStr);
                } else if (valueOld instanceof Float) {
                    valueNew = Double.parseDouble(valueNewStr);
                } else if (valueOld instanceof Integer) {
                    valueNew = Double.parseDouble(valueNewStr);
                } else if (valueOld instanceof Long) {
                    valueNew = Double.parseDouble(valueNewStr);
                } else if (valueOld instanceof Boolean) {
                    valueNew = Double.parseDouble(valueNewStr);
                } else {
                    throw new RuntimeException(
                            String.format("Unsupported parameter type for %s: %s", key, valueOld.getClass()));
                }
                params.put(key, valueNew);
            }
        }
        processFile();
    }
}
