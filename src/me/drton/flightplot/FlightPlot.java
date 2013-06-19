package me.drton.flightplot;

import me.drton.flightplot.processors.PlotProcessor;
import me.drton.flightplot.processors.ProcessorsList;
import me.drton.flightplot.processors.Simple;
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
import java.awt.event.*;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * User: ton Date: 03.06.13 Time: 23:24
 */
public class FlightPlot {
    private JFrame frame;
    private JLabel statusLabel;
    private JPanel mainPanel;
    private JTable parametersTable;
    private DefaultTableModel parametersTableModel;
    private ChartPanel chartPanel;
    private JList<String> processorsList;
    private DefaultListModel<String> processorsListModel;
    private JButton addProcessorButton;
    private JButton removeProcessorButton;
    private JButton openLogButton;
    private JButton fieldsListButton;

    private static String appName = "FlightPlot";
    private String fileName = null;
    private LogReader logReader = null;
    private XYSeriesCollection dataset;
    private JFreeChart jFreeChart;
    private ProcessorsList processorsTypesList;
    private Map<String, PlotProcessor> activeProcessors = new LinkedHashMap<String, PlotProcessor>();
    private File lastLogDirectory = null;
    private AddProcessorDialog addProcessorDialog;
    private FieldsList fieldsListFrame;

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
        frame.setContentPane(mainPanel);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        createMenuBar();
        java.util.List<String> processors = new ArrayList<String>(processorsTypesList.getProcessorsList());
        Collections.sort(processors);
        addProcessorDialog = new AddProcessorDialog(processors.toArray(new String[processors.size()]));
        addProcessorDialog.pack();
        fieldsListFrame = new FieldsList(new Runnable() {
            @Override
            public void run() {
                StringBuilder fieldsValue = new StringBuilder();
                for (String field : fieldsListFrame.getSelectedFields()) {
                    if (fieldsValue.length() > 0)
                        fieldsValue.append(" ");
                    fieldsValue.append(field);
                }
                PlotProcessor processor = new Simple();
                processor.getParameters().put("Fields", fieldsValue.toString());
                addProcessor(processor, fieldsValue.toString());
                processorsList.repaint();
                processFile();
            }
        });
        addProcessorButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showAddProcessorDialog(false);
            }
        });
        removeProcessorButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                removeSelectedProcessor();
            }
        });
        openLogButton.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                showOpenFileDialog();
            }
        });
        fieldsListButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fieldsListFrame.display();
            }
        });
        frame.setVisible(true);
    }

    private void createUIComponents() throws IllegalAccessException, InstantiationException {
        // Chart panel
        processorsTypesList = new ProcessorsList();
        dataset = new XYSeriesCollection();
        jFreeChart = ChartFactory.createXYLineChart("", "", "", null, PlotOrientation.VERTICAL, true, true, false);
        jFreeChart.getXYPlot().setDataset(dataset);
        XYPlot plot = jFreeChart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
        domainAxis.setAutoRangeIncludesZero(false);
        domainAxis.setLowerMargin(0.0);
        domainAxis.setUpperMargin(0.0);
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setAutoRangeIncludesZero(false);
        chartPanel = new ChartPanel(jFreeChart);
        chartPanel.setMouseWheelEnabled(true);
        chartPanel.setMouseZoomable(true, false);
        chartPanel.setPopupMenu(null);
        // Processors list
        processorsListModel = new DefaultListModel<String>();
        processorsList = new JList<String>(processorsListModel);
        // Parameters table
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
        parametersTable.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "startEditing");
        parametersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // Event listeners
        processorsList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                showPlotParameters();
            }
        });
        processorsList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() > 1) {
                    showAddProcessorDialog(true);
                }
            }
        });
        parametersTableModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (e.getType() == TableModelEvent.UPDATE)
                    onParameterChanged();
            }
        });
    }

    private void createMenuBar() {
        JMenu newMenu = new JMenu("File");
        JMenuItem fileOpenItem = new JMenuItem("Open Log...");
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
        if (lastLogDirectory != null)
            fc.setCurrentDirectory(lastLogDirectory);
        int returnVal = fc.showDialog(frame, "Open");
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            lastLogDirectory = fc.getCurrentDirectory();
            File file = fc.getSelectedFile();
            fileName = file.getPath();
            frame.setTitle(appName + " - " + fileName);
            if (logReader != null) {
                try {
                    logReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                logReader = null;
            }
            try {
                logReader = new PX4LogReader(fileName);
            } catch (Exception e) {
                logReader = null;
                setStatus("Error: " + e);
                e.printStackTrace();
            }
            fieldsListFrame.setFieldsList(logReader.getFields());
            setAutoRange(true, true);
            processFile();
        }
    }

    private void processFile() {
        if (logReader != null) {
            setStatus("Processing...");
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    try {
                        generateSeries();
                        setStatus(" ");
                    } catch (Exception e) {
                        setStatus("Error: " + e);
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private void generateSeries() throws IOException, FormatErrorException {
        dataset.removeAllSeries();
        for (PlotProcessor processor : activeProcessors.values()) {
            processor.init();
        }
        logReader.seek(0);
        Map<String, Object> data = new HashMap<String, Object>();
        while (true) {
            long t;
            data.clear();
            try {
                t = logReader.readUpdate(data);
            } catch (EOFException e) {
                break;
            } catch (Exception e) {
                setStatus("Error: " + e);
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
        chartPanel.repaint();
    }

    private void setAutoRange(boolean horizontal, boolean vertical) {
        if (horizontal)
            jFreeChart.getXYPlot().getDomainAxis().setAutoRange(true);
        if (vertical)
            jFreeChart.getXYPlot().getRangeAxis().setAutoRange(true);
    }

    private void showAddProcessorDialog(boolean editMode) {
        String title = null;
        String processorType = null;
        if (editMode) {
            String selectedProcessor = processorsList.getSelectedValue();
            if (selectedProcessor != null) {
                PlotProcessor processor = activeProcessors.get(selectedProcessor);
                title = processor.getTitle();
                processorType = processor.getProcessorName();
            }
        }
        addProcessorDialog.display(new Runnable() {
            @Override
            public void run() {
                onAddProcessorDialogOK();
            }
        }, title, processorType);
    }

    private void onAddProcessorDialogOK() {
        String oldTitle = addProcessorDialog.getOldTitle();
        String oldProcessorType = addProcessorDialog.getOldProcessorType();
        String title = addProcessorDialog.getTitle();
        String processorType = addProcessorDialog.getProcessorType();
        String fullTitle = PlotProcessor.getFullTitle(title, processorType);
        if (oldTitle != null) {
            // Edit processor
            String oldFullTitle = PlotProcessor.getFullTitle(oldTitle, oldProcessorType);
            PlotProcessor processor = activeProcessors.get(oldFullTitle);
            activeProcessors.remove(oldFullTitle);
            if (activeProcessors.containsKey(fullTitle)) {
                activeProcessors.put(oldFullTitle, processor);
                setStatus("Title already exists: " + fullTitle);
                return;
            }
            if (!oldProcessorType.equals(processorType)) {
                // Processor type changed, replace instance
                Map<String, Object> parameters = processor.getParameters();
                try {
                    processor = processorsTypesList.getProcessorInstance(processorType);
                } catch (Exception e) {
                    setStatus("Error creating processor");
                    e.printStackTrace();
                    return;
                }
                for (Map.Entry<String, Object> entry : processor.getParameters().entrySet()) {
                    String key = entry.getKey();
                    Object oldValue = parameters.get(key);
                    Object newValue = processor.getParameters().get(key);
                    if (oldValue != null && oldValue.getClass() == newValue.getClass()) {
                        processor.getParameters().put(key, oldValue);
                    }
                }
            }
            processor.setTitle(title);
            activeProcessors.put(fullTitle, processor);
            int idx = processorsListModel.indexOf(oldFullTitle);
            processorsListModel.remove(idx);
            processorsListModel.add(idx, fullTitle);
            processorsList.setSelectedValue(fullTitle, true);
        } else {
            try {
                PlotProcessor processor = processorsTypesList.getProcessorInstance(processorType);
                addProcessor(processor, title);
            } catch (Exception e) {
                setStatus("Error creating processor");
                e.printStackTrace();
            }
        }
        processorsList.repaint();
        processFile();
    }

    private void addProcessor(PlotProcessor processor, String title) {
        String fullTitle = PlotProcessor.getFullTitle(title, processor.getClass().getSimpleName());
        if (activeProcessors.containsKey(fullTitle)) {
            setStatus("Title already exists: " + fullTitle);
            return;
        }
        processor.setTitle(title);
        processorsListModel.addElement(fullTitle);
        activeProcessors.put(fullTitle, processor);
        processorsList.setSelectedValue(fullTitle, true);
    }

    private void removeProcessor(String fullTitle) {
        processorsListModel.removeElement(fullTitle);
        activeProcessors.remove(fullTitle);
    }

    private void removeSelectedProcessor() {
        String selectedProcessor = processorsList.getSelectedValue();
        if (selectedProcessor != null) {
            removeProcessor(selectedProcessor);
            processFile();
        }
    }

    private void showPlotParameters() {
        while (parametersTableModel.getRowCount() > 0) {
            parametersTableModel.removeRow(0);
        }
        String selectedProcessor = processorsList.getSelectedValue();
        if (selectedProcessor != null) {
            Map<String, Object> params = activeProcessors.get(selectedProcessor).getParameters();
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                parametersTableModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
            }
        }
    }

    private void onParameterChanged() {
        String selectedProcessor = processorsList.getSelectedValue();
        if (selectedProcessor != null) {
            Map<String, Object> params = activeProcessors.get(selectedProcessor).getParameters();
            for (int i = 0; i < parametersTableModel.getRowCount(); i++) {
                String key = (String) parametersTableModel.getValueAt(i, 0);
                Object valueOld = params.get(key);
                String valueNewStr = parametersTableModel.getValueAt(i, 1).toString();
                Object valueNew;
                try {
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
                } catch (Exception e) {
                    setStatus("Error: " + e);
                    e.printStackTrace();
                }
            }
        }
        processFile();
    }
}
