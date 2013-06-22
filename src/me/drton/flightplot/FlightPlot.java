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
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * User: ton Date: 03.06.13 Time: 23:24
 */
public class FlightPlot {
    private JFrame mainFrame;
    private JLabel statusLabel;
    private JPanel mainPanel;
    private JTable parametersTable;
    private DefaultTableModel parametersTableModel;
    private ChartPanel chartPanel;
    private JList<PlotProcessor> processorsList;
    private DefaultListModel<PlotProcessor> processorsListModel;
    private JButton addProcessorButton;
    private JButton removeProcessorButton;
    private JButton openLogButton;
    private JButton fieldsListButton;
    private JComboBox presetComboBox;
    private JButton deletePresetButton;

    private static String appName = "FlightPlot";
    private final Preferences preferences;
    private String fileName = null;
    private LogReader logReader = null;
    private XYSeriesCollection dataset;
    private JFreeChart jFreeChart;
    private ProcessorsList processorsTypesList;
    private File lastLogDirectory = null;
    private AddProcessorDialog addProcessorDialog;
    private FieldsListDialog fieldsList;
    private Map<String, ProcessorPreset> presets = new HashMap<String, ProcessorPreset>();

    public static void main(String[] args)
            throws ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException,
                   IllegalAccessException {
        if (OSValidator.isMac()) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
        }
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new FlightPlot();
            }
        });
    }

    public FlightPlot() {
        preferences = Preferences.userRoot().node(appName);
        mainFrame = new JFrame(appName);
        mainFrame.setContentPane(mainPanel);
        mainFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        if (OSValidator.isMac()) {
            // Do it via separate class to avoid loading Mac classes
            new AppleQuitHandler(new Runnable() {
                @Override
                public void run() {
                    onQuit();
                }
            });
        } else {
            mainFrame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    onQuit();
                }
            });
        }
        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onQuit();
            }
        });
        mainFrame.pack();
        createMenuBar();
        java.util.List<String> processors = new ArrayList<String>(processorsTypesList.getProcessorsList());
        Collections.sort(processors);
        addProcessorDialog = new AddProcessorDialog(processors.toArray(new String[processors.size()]));
        addProcessorDialog.pack();
        fieldsList = new FieldsListDialog(new Runnable() {
            @Override
            public void run() {
                StringBuilder fieldsValue = new StringBuilder();
                for (String field : fieldsList.getSelectedFields()) {
                    if (fieldsValue.length() > 0)
                        fieldsValue.append(" ");
                    fieldsValue.append(field);
                }
                PlotProcessor processor = new Simple();
                processor.getParameters().put("Fields", fieldsValue.toString());
                processor.setTitle("New");
                processorsListModel.addElement(processor);
                processorsList.setSelectedValue(processor, true);
                processorsList.repaint();
                showAddProcessorDialog(true);
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
                fieldsList.setVisible(true);
            }
        });
        processorsList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                showPlotParameters();
            }
        });
        processorsList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.VK_ENTER)
                    showAddProcessorDialog(true);
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
        try {
            loadPreferences();
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
        mainFrame.setVisible(true);
        presetComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onPresetAction(e);
            }
        });
        updatePresetEdited(true);
        deletePresetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onDeletePreset();
            }
        });
    }

    private void onQuit() {
        try {
            savePreferences();
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    private void onPresetAction(ActionEvent e) {
        if ("comboBoxEdited".equals(e.getActionCommand())) {
            // Save preset
            String presetTitle = presetComboBox.getSelectedItem().toString();
            if (presetTitle.isEmpty())
                return;
            Preset preset = formatPreset();
            preset.setTitle(presetTitle);
            boolean addNew = true;
            for (int i = 0; i < presetComboBox.getItemCount(); i++) {
                if (presetTitle.equals(presetComboBox.getItemAt(i).toString())) {
                    // Update existing preset
                    addNew = false;
                    presetComboBox.removeItemAt(i);
                    presetComboBox.insertItemAt(preset, i);
                    presetComboBox.setSelectedIndex(i);
                    break;
                }
            }
            if (addNew) {
                // Add new preset
                presetComboBox.addItem(preset);
            }
            updatePresetEdited(false);
        } else if ("comboBoxChanged".equals(e.getActionCommand())) {
            // Load preset
            Object selection = presetComboBox.getSelectedItem();
            if ("".equals(selection)) {
                processorsListModel.clear();
            }
            if (selection instanceof Preset) {
                loadPreset((Preset) selection);
            }
            updatePresetEdited(false);
            processFile();
        }
    }

    private void onDeletePreset() {
        int i = presetComboBox.getSelectedIndex();
        if (i >= 0)
            presetComboBox.removeItemAt(i);
        presetComboBox.setSelectedIndex(0);
    }

    private void updatePresetEdited(boolean edited) {
        presetComboBox.getEditor().getEditorComponent().setForeground(edited ? Color.GRAY : Color.BLACK);
    }

    private void loadPreferences() throws BackingStoreException {
        loadWindowPreferences(mainFrame, preferences.node("MainWindow"), 800, 600);
        loadWindowPreferences(fieldsList, preferences.node("FieldsListDialog"), 300, 600);
        loadWindowPreferences(addProcessorDialog, preferences.node("AddProcessorDialog"), -1, -1);
        String logDirectoryStr = preferences.get("LogDirectory", null);
        if (logDirectoryStr != null)
            lastLogDirectory = new File(logDirectoryStr);
        Preferences presets = preferences.node("Presets");
        presetComboBox.addItem("");
        for (String p : presets.childrenNames()) {
            Preset preset = Preset.unpack(presets.node(p));
            if (preset != null) {
                presetComboBox.addItem(preset);
            }
        }
    }

    private void savePreferences() throws BackingStoreException {
        preferences.clear();
        saveWindowPreferences(mainFrame, preferences.node("MainWindow"));
        saveWindowPreferences(fieldsList, preferences.node("FieldsListDialog"));
        saveWindowPreferences(addProcessorDialog, preferences.node("AddProcessorDialog"));
        if (lastLogDirectory != null)
            preferences.put("LogDirectory", lastLogDirectory.getAbsolutePath());
        Preferences presetsPref = preferences.node("Presets");
        for (int i = 0; i < presetComboBox.getItemCount(); i++) {
            Object object = presetComboBox.getItemAt(i);
            if (object instanceof Preset) {
                Preset preset = (Preset) object;
                preset.pack(presetsPref);
            }
        }
    }

    private void loadPreset(Preset preset) {
        processorsListModel.clear();
        for (ProcessorPreset pp : preset.getProcessorPresets()) {
            try {
                PlotProcessor processor = processorsTypesList.getProcessorInstance(pp.getProcessorType());
                processor.setTitle(pp.getTitle());
                processor.setParameters(pp.getParameters());
                processorsListModel.addElement(processor);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private Preset formatPreset() {
        List<ProcessorPreset> processorPresets = new ArrayList<ProcessorPreset>();
        for (int i = 0; i < processorsListModel.size(); i++) {
            PlotProcessor processor = processorsListModel.elementAt(i);
            processorPresets.add(new ProcessorPreset(processor));
        }
        return new Preset("PresetLast", processorPresets);
    }

    private void loadWindowPreferences(Component window, Preferences windowPreferences, int defaultWidth,
                                       int defaultHeight) {
        if (defaultWidth > 0)
            window.setSize(windowPreferences.getInt("Width", defaultWidth),
                    windowPreferences.getInt("Height", defaultHeight));
        window.setLocation(windowPreferences.getInt("X", 0), windowPreferences.getInt("Y", 0));
    }

    private void saveWindowPreferences(Component window, Preferences windowPreferences) {
        Dimension size = window.getSize();
        windowPreferences.putInt("Width", size.width);
        windowPreferences.putInt("Height", size.height);
        Point location = window.getLocation();
        windowPreferences.putInt("X", location.x);
        windowPreferences.putInt("Y", location.y);
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
        processorsListModel = new DefaultListModel<PlotProcessor>();
        processorsList = new JList<PlotProcessor>(processorsListModel);
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
        mainFrame.setJMenuBar(menuBar);
    }

    public void setStatus(String status) {
        statusLabel.setText(status);
    }

    public void showOpenFileDialog() {
        JFileChooser fc = new JFileChooser();
        if (lastLogDirectory != null)
            fc.setCurrentDirectory(lastLogDirectory);
        int returnVal = fc.showDialog(mainFrame, "Open");
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            lastLogDirectory = fc.getCurrentDirectory();
            File file = fc.getSelectedFile();
            fileName = file.getPath();
            mainFrame.setTitle(appName + " - " + fileName);
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
            fieldsList.setFieldsList(logReader.getFields());
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
        PlotProcessor[] processors = new PlotProcessor[processorsListModel.size()];
        for (int i = 0; i < processorsListModel.size(); i++) {
            processors[i] = processorsListModel.get(i);
            processors[i].init();
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
            for (PlotProcessor processor : processors) {
                processor.process(t * 0.000001, data);
            }
        }
        for (PlotProcessor processor : processors) {
            for (XYSeries series : (List<XYSeries>) processor.getSeriesCollection().getSeries()) {
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
        PlotProcessor selectedProcessor = editMode ? processorsList.getSelectedValue() : null;
        addProcessorDialog.display(new Runnable() {
            @Override
            public void run() {
                onAddProcessorDialogOK();
            }
        }, selectedProcessor);
    }

    private void onAddProcessorDialogOK() {
        updatePresetEdited(true);
        PlotProcessor origProcessor = addProcessorDialog.getOrigProcessor();
        String title = addProcessorDialog.getTitle();
        String processorType = addProcessorDialog.getProcessorType();
        if (origProcessor != null) {
            // Edit processor
            PlotProcessor processor = origProcessor;
            if (!origProcessor.getProcessorType().equals(processorType)) {
                // Processor type changed, replace instance
                Map<String, Object> parameters = origProcessor.getParameters();
                try {
                    processor = processorsTypesList.getProcessorInstance(processorType);
                } catch (Exception e) {
                    setStatus("Error creating processor");
                    e.printStackTrace();
                    return;
                }
                processor.setParameters(parameters);
            }
            processor.setTitle(title);
            int idx = processorsListModel.indexOf(origProcessor);
            processorsListModel.set(idx, processor);
            processorsList.setSelectedValue(processor, true);
        } else {
            try {
                PlotProcessor processor = processorsTypesList.getProcessorInstance(processorType);
                processor.setTitle(title);
                processorsListModel.addElement(processor);
                processorsList.setSelectedValue(processor, true);
            } catch (Exception e) {
                setStatus("Error creating processor");
                e.printStackTrace();
            }
        }
        processFile();
    }

    private void removeSelectedProcessor() {
        PlotProcessor selectedProcessor = processorsList.getSelectedValue();
        if (selectedProcessor != null) {
            processorsListModel.removeElement(selectedProcessor);
            updatePresetEdited(true);
            processFile();
        }
    }

    private void showPlotParameters() {
        while (parametersTableModel.getRowCount() > 0) {
            parametersTableModel.removeRow(0);
        }
        PlotProcessor selectedProcessor = processorsList.getSelectedValue();
        if (selectedProcessor != null) {
            Map<String, Object> params = selectedProcessor.getParameters();
            List<String> keys = new ArrayList<String>(params.keySet());
            Collections.sort(keys);
            for (String key : keys) {
                String value = params.get(key).toString();
                parametersTableModel.addRow(new Object[]{key, value});
            }
        }
    }

    private void onParameterChanged() {
        PlotProcessor selectedProcessor = processorsList.getSelectedValue();
        if (selectedProcessor != null) {
            Map<String, Object> paramsUpdate = new HashMap<String, Object>();
            for (int i = 0; i < parametersTableModel.getRowCount(); i++) {
                String key = (String) parametersTableModel.getValueAt(i, 0);
                String valueNew = parametersTableModel.getValueAt(i, 1).toString();
                paramsUpdate.put(key, valueNew);
            }
            selectedProcessor.setParameters(paramsUpdate);
            updatePresetEdited(true);
        }
        processFile();
    }
}
