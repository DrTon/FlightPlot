package me.drton.flightplot;

import me.drton.flightplot.processors.PlotProcessor;
import me.drton.flightplot.processors.ProcessorsList;
import me.drton.flightplot.processors.Simple;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.event.ChartChangeEvent;
import org.jfree.chart.event.ChartChangeEventType;
import org.jfree.chart.event.ChartChangeListener;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.Range;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.Charset;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private JList processorsList;
    private DefaultListModel processorsListModel;
    private TableModelListener parameterChangedListener;
    private JButton addProcessorButton;
    private JButton removeProcessorButton;
    private JButton openLogButton;
    private JButton fieldsListButton;
    private JComboBox presetComboBox;
    private JButton deletePresetButton;
    private JButton logInfoButton;

    private static String appName = "FlightPlot";
    private static String version = "0.2.1";
    private static String appNameAndVersion = appName + " v." + version;
    private final Preferences preferences;
    private String fileName = null;
    private LogReader logReader = null;
    private XYSeriesCollection dataset;
    private JFreeChart jFreeChart;
    private ProcessorsList processorsTypesList;
    private File lastLogDirectory = null;
    private File lastPresetDirectory = null;
    private AddProcessorDialog addProcessorDialog;
    private FieldsListDialog fieldsListDialog;
    private LogInfo logInfo;
    private FileNameExtensionFilter logExtensionFilter = new FileNameExtensionFilter("PX4 Logs (*.bin)", "bin");
    private FileNameExtensionFilter presetExtensionFilter = new FileNameExtensionFilter("FlightPlot Presets (*.fplot)",
            "fplot");
    private AtomicBoolean invokeProcessFile = new AtomicBoolean(false);

    private static final NumberFormat doubleNumberFormat = NumberFormat.getInstance(Locale.ROOT);

    static {
        doubleNumberFormat.setGroupingUsed(false);
        doubleNumberFormat.setMinimumFractionDigits(1);
        doubleNumberFormat.setMaximumFractionDigits(10);
    }

    public static void main(String[] args)
            throws ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException,
                   IllegalAccessException {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (OSValidator.isMac()) {
                    System.setProperty("apple.laf.useScreenMenuBar", "true");
                }
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
                new FlightPlot();
            }
        });
    }

    public FlightPlot() {
        preferences = Preferences.userRoot().node(appName);
        mainFrame = new JFrame(appNameAndVersion);
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
        fieldsListDialog = new FieldsListDialog(new Runnable() {
            @Override
            public void run() {
                StringBuilder fieldsValue = new StringBuilder();
                for (String field : fieldsListDialog.getSelectedFields()) {
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
        logInfo = new LogInfo();
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
                showOpenLogDialog();
            }
        });
        fieldsListButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fieldsListDialog.setVisible(true);
            }
        });
        logInfoButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logInfo.setVisible(true);
            }
        });
        processorsList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                showProcessorParameters();
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
        parameterChangedListener = new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (e.getType() == TableModelEvent.UPDATE) {
                    int row = e.getFirstRow();
                    onParameterChanged(row);
                }
            }
        };
        parametersTableModel.addTableModelListener(parameterChangedListener);
        try {
            loadPreferences();
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
        presetComboBox.setMaximumRowCount(20);
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
        mainFrame.setVisible(true);
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
            Preset preset = formatPreset(presetTitle);
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
        loadWindowPreferences(fieldsListDialog, preferences.node("FieldsListDialog"), 300, 600);
        loadWindowPreferences(addProcessorDialog, preferences.node("AddProcessorDialog"), -1, -1);
        loadWindowPreferences(logInfo.getFrame(), preferences.node("LogInfoFrame"), 600, 600);
        String logDirectoryStr = preferences.get("LogDirectory", null);
        if (logDirectoryStr != null)
            lastLogDirectory = new File(logDirectoryStr);
        String presetDirectoryStr = preferences.get("PresetDirectory", null);
        if (presetDirectoryStr != null)
            lastPresetDirectory = new File(presetDirectoryStr);
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
        for (String child : preferences.childrenNames()) {
            preferences.node(child).removeNode();
        }
        saveWindowPreferences(mainFrame, preferences.node("MainWindow"));
        saveWindowPreferences(fieldsListDialog, preferences.node("FieldsListDialog"));
        saveWindowPreferences(addProcessorDialog, preferences.node("AddProcessorDialog"));
        saveWindowPreferences(logInfo.getFrame(), preferences.node("LogInfoFrame"));
        if (lastLogDirectory != null)
            preferences.put("LogDirectory", lastLogDirectory.getAbsolutePath());
        if (lastPresetDirectory != null)
            preferences.put("PresetDirectory", lastPresetDirectory.getAbsolutePath());
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
                if (processor != null) {
                    processor.setTitle(pp.getTitle());
                    processor.setParameters(pp.getParameters());
                    processorsListModel.addElement(processor);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private Preset formatPreset(String title) {
        List<ProcessorPreset> processorPresets = new ArrayList<ProcessorPreset>();
        for (int i = 0; i < processorsListModel.size(); i++) {
            PlotProcessor processor = (PlotProcessor) processorsListModel.elementAt(i);
            processorPresets.add(new ProcessorPreset(processor));
        }
        return new Preset(title, processorPresets);
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
        jFreeChart.addChangeListener(new ChartChangeListener() {
            @Override
            public void chartChanged(ChartChangeEvent chartChangeEvent) {
                if (chartChangeEvent.getType() == ChartChangeEventType.GENERAL) {
                    processFile();
                }
            }
        });
        // Processors list
        processorsListModel = new DefaultListModel();
        processorsList = new JList(processorsListModel);
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
        JMenuItem importPresetItem = new JMenuItem("Import Preset...");
        JMenuItem exportPresetItem = new JMenuItem("Export Preset...");
        fileOpenItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showOpenLogDialog();
            }
        });
        importPresetItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showImportPresetDialog();
            }
        });
        exportPresetItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showExportPresetDialog();
            }
        });
        newMenu.add(fileOpenItem);
        newMenu.add(importPresetItem);
        newMenu.add(exportPresetItem);
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(newMenu);
        mainFrame.setJMenuBar(menuBar);
    }

    public void setStatus(String status) {
        statusLabel.setText(status);
    }

    public void showOpenLogDialog() {
        JFileChooser fc = new JFileChooser();
        if (lastLogDirectory != null)
            fc.setCurrentDirectory(lastLogDirectory);
        fc.setFileFilter(logExtensionFilter);
        fc.setDialogTitle("Open Log");
        int returnVal = fc.showDialog(mainFrame, "Open");
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            lastLogDirectory = fc.getCurrentDirectory();
            File file = fc.getSelectedFile();
            fileName = file.getPath();
            mainFrame.setTitle(appNameAndVersion + " - " + fileName);
            if (logReader != null) {
                try {
                    logReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                logReader = null;
            }
            long logStart = 0;
            long logSize = 1000000;
            try {
                logReader = new PX4LogReader(fileName);
                logInfo.updateInfo(logReader);
                logStart = logReader.getStartMicroseconds();
                logSize = logReader.getSizeMicroseconds();
            } catch (Exception e) {
                logReader = null;
                setStatus("Error: " + e);
                e.printStackTrace();
            }
            fieldsListDialog.setFieldsList(logReader.getFields());
            Range timeRange = new Range(logStart / 1000000.0, (logStart + logSize) / 1000000.0);
            jFreeChart.getXYPlot().getDomainAxis().setDefaultAutoRange(timeRange);
            jFreeChart.getXYPlot().getDomainAxis().setAutoRange(true);
            jFreeChart.getXYPlot().getRangeAxis().setAutoRange(true);
            processFile();
        }
    }

    public void showImportPresetDialog() {
        JFileChooser fc = new JFileChooser();
        if (lastPresetDirectory != null)
            fc.setCurrentDirectory(lastPresetDirectory);
        fc.setFileFilter(presetExtensionFilter);
        fc.setDialogTitle("Import Preset");
        int returnVal = fc.showDialog(mainFrame, "Import");
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            lastPresetDirectory = fc.getCurrentDirectory();
            File file = fc.getSelectedFile();
            try {
                byte[] b = new byte[(int) file.length()];
                FileInputStream fileInputStream = new FileInputStream(file);
                int n = 0;
                while (n < b.length) {
                    int r = fileInputStream.read(b, n, b.length - n);
                    if (r <= 0)
                        throw new Exception("Read error");
                    n += r;
                }
                Preset preset = Preset.unpackJSONObject(new JSONObject(new String(b, Charset.forName("utf8"))));
                loadPreset(preset);
                processFile();
            } catch (Exception e) {
                setStatus("Error: " + e);
                e.printStackTrace();
            }
        }
    }

    public void showExportPresetDialog() {
        JFileChooser fc = new JFileChooser();
        if (lastPresetDirectory != null)
            fc.setCurrentDirectory(lastPresetDirectory);
        fc.setFileFilter(presetExtensionFilter);
        fc.setDialogTitle("Export Preset");
        int returnVal = fc.showDialog(mainFrame, "Export");
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            lastPresetDirectory = fc.getCurrentDirectory();
            String fileName = fc.getSelectedFile().toString();
            if (presetExtensionFilter == fc.getFileFilter() && !fileName.toLowerCase().endsWith(".fplot"))
                fileName += ".fplot";
            try {
                Preset preset = formatPreset(presetComboBox.getSelectedItem().toString());
                FileWriter fileWriter = new FileWriter(new File(fileName));
                fileWriter.write(preset.packJSONObject().toString(1));
                fileWriter.close();
            } catch (Exception e) {
                setStatus("Error: " + e);
                e.printStackTrace();
            }
        }
    }

    private void processFile() {
        if (logReader != null) {
            if (invokeProcessFile.compareAndSet(false, true)) {
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
                        invokeProcessFile.lazySet(false);
                    }
                });
            }
        }
    }

    private void generateSeries() throws IOException, FormatErrorException {
        dataset.removeAllSeries();
        PlotProcessor[] processors = new PlotProcessor[processorsListModel.size()];
        Range timeAxisRange = jFreeChart.getXYPlot().getDomainAxis().getRange();
        // Process some extra data in hidden areas
        long timeStart = (long) ((timeAxisRange.getLowerBound() - timeAxisRange.getLength()) * 1000000);
        long timeStop = (long) ((timeAxisRange.getUpperBound() + timeAxisRange.getLength()) * 1000000);
        timeStart = Math.max(logReader.getStartMicroseconds(), timeStart);
        timeStop = Math.min(logReader.getStartMicroseconds() + logReader.getSizeMicroseconds(), timeStop);
        int displayPixels = 2000;
        double skip = timeAxisRange.getLength() / displayPixels;
        if (processors.length > 0) {
            for (int i = 0; i < processorsListModel.size(); i++) {
                processors[i] = (PlotProcessor) processorsListModel.get(i);
                processors[i].init();
                processors[i].setSkipOut(skip);
            }
            logReader.seek(timeStart);
            Map<String, Object> data = new HashMap<String, Object>();
            while (true) {
                long t;
                data.clear();
                try {
                    t = logReader.readUpdate(data);
                } catch (EOFException e) {
                    break;
                }
                if (t > timeStop)
                    break;
                for (PlotProcessor processor : processors) {
                    processor.process(t * 0.000001, data);
                }
            }
            for (PlotProcessor processor : processors) {
                for (XYSeries series : (List<XYSeries>) processor.getSeriesCollection().getSeries()) {
                    dataset.addSeries(series);
                }
            }
        }
        chartPanel.repaint();
    }

    private void showAddProcessorDialog(boolean editMode) {
        PlotProcessor selectedProcessor = editMode ? (PlotProcessor) processorsList.getSelectedValue() : null;
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
        String title = addProcessorDialog.getProcessorTitle();
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
            showProcessorParameters();
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
        PlotProcessor selectedProcessor = (PlotProcessor) processorsList.getSelectedValue();
        if (selectedProcessor != null) {
            processorsListModel.removeElement(selectedProcessor);
            updatePresetEdited(true);
            processFile();
        }
    }

    private static String formatParameterValue(Object value) {
        String valueStr;
        if (value instanceof Double)
            valueStr = doubleNumberFormat.format(value);
        else
            valueStr = value.toString();
        return valueStr;
    }

    private void showProcessorParameters() {
        while (parametersTableModel.getRowCount() > 0) {
            parametersTableModel.removeRow(0);
        }
        PlotProcessor selectedProcessor = (PlotProcessor) processorsList.getSelectedValue();
        if (selectedProcessor != null) {
            Map<String, Object> params = selectedProcessor.getParameters();
            List<String> keys = new ArrayList<String>(params.keySet());
            Collections.sort(keys);
            for (String key : keys) {
                parametersTableModel.addRow(new Object[]{key, formatParameterValue(params.get(key))});
            }
        }
    }

    private void onParameterChanged(int row) {
        PlotProcessor selectedProcessor = (PlotProcessor) processorsList.getSelectedValue();
        if (selectedProcessor != null) {
            String key = parametersTableModel.getValueAt(row, 0).toString();
            Object value = parametersTableModel.getValueAt(row, 1);
            try {
                selectedProcessor.setParameter(key, value);
                updatePresetEdited(true);
            } catch (Exception e) {
                setStatus("Error: " + e);
            }
            parametersTableModel.removeTableModelListener(parameterChangedListener);
            parametersTableModel.setValueAt(formatParameterValue(selectedProcessor.getParameters().get(key)), row, 1);
            parametersTableModel.addTableModelListener(parameterChangedListener);
            processFile();
        }
    }
}
