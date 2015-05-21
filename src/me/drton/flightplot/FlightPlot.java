package me.drton.flightplot;

import me.drton.flightplot.export.ExportData;
import me.drton.flightplot.export.ExportManager;
import me.drton.flightplot.processors.PlotProcessor;
import me.drton.flightplot.processors.ProcessorsList;
import me.drton.flightplot.processors.Simple;
import me.drton.jmavlib.log.FormatErrorException;
import me.drton.jmavlib.log.LogReader;
import me.drton.jmavlib.log.PX4LogReader;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
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
    private static final int TIME_MODE_LOG_START = 0;
    private static final int TIME_MODE_BOOT = 1;
    private static final int TIME_MODE_GPS = 2;
    private static final NumberFormat doubleNumberFormat = NumberFormat.getInstance(Locale.ROOT);

    static {
        doubleNumberFormat.setGroupingUsed(false);
        doubleNumberFormat.setMinimumFractionDigits(1);
        doubleNumberFormat.setMaximumFractionDigits(10);
    }

    private static String appName = "FlightPlot";
    private static String version = "0.2.14";
    private static String appNameAndVersion = appName + " v." + version;
    private static String colorParamPrefix = "Color ";
    private final Preferences preferences;
    private JFrame mainFrame;
    private JLabel statusLabel;
    private JPanel mainPanel;
    private JTable parametersTable;
    private DefaultTableModel parametersTableModel;
    private ChartPanel chartPanel;
    private JTable processorsList;
    private DefaultTableModel processorsListModel;
    private TableModelListener parameterChangedListener;
    private JButton addProcessorButton;
    private JButton removeProcessorButton;
    private JButton openLogButton;
    private JButton fieldsListButton;
    private JComboBox presetComboBox;
    private JButton deletePresetButton;
    private JButton logInfoButton;
    private JRadioButtonMenuItem[] timeModeItems;
    private LogReader logReader = null;
    private XYSeriesCollection dataset;
    private JFreeChart jFreeChart;
    private ColorSupplier colorSupplier;
    private ProcessorsList processorsTypesList;
    private File lastLogDirectory = null;
    private File lastPresetDirectory = null;
    private AddProcessorDialog addProcessorDialog;
    private FieldsListDialog fieldsListDialog;
    private LogInfo logInfo;
    private FileNameExtensionFilter logExtensionFilter = new FileNameExtensionFilter("PX4/APM Logs (*.bin, *.px4log)", "bin", "px4log");
    private FileNameExtensionFilter presetExtensionFilter = new FileNameExtensionFilter("FlightPlot Presets (*.fplot)",
            "fplot");
    private AtomicBoolean invokeProcessFile = new AtomicBoolean(false);
    private ExportManager exportManager = new ExportManager();
    private PreferencesUtil preferencesUtil = new PreferencesUtil();
    private NumberAxis domainAxisSeconds;
    private DateAxis domainAxisDate;
    private int timeMode = 0;
    private List<Map<String, Integer>> seriesIndex = new ArrayList<Map<String, Integer>>();
    private ProcessorPreset editingProcessor = null;
    private List<ProcessorPreset> activeProcessors = new ArrayList<ProcessorPreset>();

    public FlightPlot() {
        preferences = Preferences.userRoot().node(appName);
        mainFrame = new JFrame(appNameAndVersion);
        mainFrame.setContentPane(mainPanel);
        mainFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

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
                    if (fieldsValue.length() > 0) {
                        fieldsValue.append(" ");
                    }
                    fieldsValue.append(field);
                }
                PlotProcessor processor = new Simple();
                processor.setParameters(Collections.<String, Object>singletonMap("Fields", fieldsValue.toString()));
                ProcessorPreset pp = new ProcessorPreset("New", processor.getProcessorType(),
                        processor.getParameters(), Collections.<String, Color>emptyMap());
                updatePresetParameters(pp, null);
                int i = processorsListModel.getRowCount();
                processorsListModel.addRow(new Object[]{true, pp});
                processorsList.getSelectionModel().setSelectionInterval(i, i);
                processorsList.repaint();
                updateUsedColors();
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
            @Override
            public void actionPerformed(ActionEvent e) {
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
        processorsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        processorsList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                showProcessorParameters();
            }
        });
        processorsList.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "Enter");
        processorsList.getActionMap().put("Enter", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                showAddProcessorDialog(true);
            }
        });
        processorsList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JTable target = (JTable) e.getSource();
                if (e.getClickCount() > 1 && target.getSelectedColumn() == 1) {
                    showAddProcessorDialog(true);
                }
            }
        });
        processorsListModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (e.getType() == TableModelEvent.UPDATE) {
                    if (e.getColumn() == 0) {
                        processFile();
                    }
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

    private static Object formatParameterValue(Object value) {
        Object returnValue;
        if (value instanceof Double) {
            returnValue = doubleNumberFormat.format(value);
        } else if (value instanceof Color) {
            returnValue = value;
        } else {
            returnValue = value.toString();
        }
        return returnValue;
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
            if (presetTitle.isEmpty()) {
                return;
            }
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
                processorsListModel.setRowCount(0);
                updateUsedColors();
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
        if (i >= 0) {
            presetComboBox.removeItemAt(i);
        }
        presetComboBox.setSelectedIndex(0);
    }

    private void updatePresetEdited(boolean edited) {
        presetComboBox.getEditor().getEditorComponent().setForeground(edited ? Color.GRAY : Color.BLACK);
    }

    private void loadPreferences() throws BackingStoreException {
        preferencesUtil.loadWindowPreferences(mainFrame, preferences.node("MainWindow"), 800, 600);
        preferencesUtil.loadWindowPreferences(fieldsListDialog, preferences.node("FieldsListDialog"), 300, 600);
        preferencesUtil.loadWindowPreferences(addProcessorDialog, preferences.node("AddProcessorDialog"), -1, -1);
        preferencesUtil.loadWindowPreferences(logInfo.getFrame(), preferences.node("LogInfoFrame"), 600, 600);
        String logDirectoryStr = preferences.get("LogDirectory", null);
        if (logDirectoryStr != null) {
            lastLogDirectory = new File(logDirectoryStr);
        }
        String presetDirectoryStr = preferences.get("PresetDirectory", null);
        if (presetDirectoryStr != null) {
            lastPresetDirectory = new File(presetDirectoryStr);
        }
        Preferences presets = preferences.node("Presets");
        presetComboBox.addItem("");
        for (String p : presets.keys()) {
            try {
                Preset preset = Preset.unpackJSONObject(new JSONObject(presets.get(p, "{}")));
                if (preset != null) {
                    presetComboBox.addItem(preset);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        timeMode = Integer.parseInt(preferences.get("TimeMode", "0"));
        timeModeItems[timeMode].setSelected(true);
        this.exportManager.loadPreferences(preferences);
    }

    private void savePreferences() throws BackingStoreException {
        preferences.clear();
        for (String child : preferences.childrenNames()) {
            preferences.node(child).removeNode();
        }
        preferencesUtil.saveWindowPreferences(mainFrame, preferences.node("MainWindow"));
        preferencesUtil.saveWindowPreferences(fieldsListDialog, preferences.node("FieldsListDialog"));
        preferencesUtil.saveWindowPreferences(addProcessorDialog, preferences.node("AddProcessorDialog"));
        preferencesUtil.saveWindowPreferences(logInfo.getFrame(), preferences.node("LogInfoFrame"));
        if (lastLogDirectory != null) {
            preferences.put("LogDirectory", lastLogDirectory.getAbsolutePath());
        }
        if (lastPresetDirectory != null) {
            preferences.put("PresetDirectory", lastPresetDirectory.getAbsolutePath());
        }
        Preferences presetsPref = preferences.node("Presets");
        for (int i = 0; i < presetComboBox.getItemCount(); i++) {
            Object object = presetComboBox.getItemAt(i);
            if (object instanceof Preset) {
                Preset preset = (Preset) object;
                try {
                    presetsPref.put(preset.getTitle(), preset.packJSONObject().toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        preferences.put("TimeMode", Integer.toString(timeMode));
        this.exportManager.savePreferences(preferences);
        preferences.sync();
    }

    private void loadPreset(Preset preset) {
        processorsListModel.setRowCount(0);
        for (ProcessorPreset pp : preset.getProcessorPresets()) {
            updatePresetParameters(pp, null);
            processorsListModel.addRow(new Object[]{true, pp});
        }
        updateUsedColors();
    }

    private Preset formatPreset(String title) {
        List<ProcessorPreset> processorPresets = new ArrayList<ProcessorPreset>();
        for (int i = 0; i < processorsListModel.getRowCount(); i++) {
            processorPresets.add((ProcessorPreset) processorsListModel.getValueAt(i, 1));
        }
        return new Preset(title, processorPresets);
    }

    private void createUIComponents() throws IllegalAccessException, InstantiationException {
        // Chart panel
        processorsTypesList = new ProcessorsList();
        dataset = new XYSeriesCollection();
        colorSupplier = new ColorSupplier();
        jFreeChart = ChartFactory.createXYLineChart("", "", "", null, PlotOrientation.VERTICAL, true, true, false);
        jFreeChart.getXYPlot().setDataset(dataset);

        // Set plot colors
        XYPlot plot = jFreeChart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        // Domain (X) axis - seconds
        domainAxisSeconds = new NumberAxis("T") {
            // Use default auto range to adjust range
            protected void autoAdjustRange() {
                setRange(getDefaultAutoRange());
            }
        };
        //domainAxisSeconds.setAutoRangeIncludesZero(false);
        domainAxisSeconds.setLowerMargin(0.0);
        domainAxisSeconds.setUpperMargin(0.0);

        // Domain (X) axis - date
        domainAxisDate = new DateAxis("T") {
            // Use default auto range to adjust range
            protected void autoAdjustRange() {
                setRange(getDefaultAutoRange());
            }
        };
        domainAxisDate.setTimeZone(TimeZone.getTimeZone("GMT"));
        domainAxisDate.setLowerMargin(0.0);
        domainAxisDate.setUpperMargin(0.0);

        // Use seconds by default
        plot.setDomainAxis(domainAxisSeconds);

        // Range (Y) axis
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
        processorsListModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 0;
            }

            @Override
            public Class<?> getColumnClass(int col) {
                return col == 0 ? Boolean.class : String.class;
            }
        };
        processorsListModel.addColumn("");
        processorsListModel.addColumn("Processor");
        processorsList = new JTable(processorsListModel);
        processorsList.getColumnModel().getColumn(0).setMinWidth(20);
        processorsList.getColumnModel().getColumn(0).setMaxWidth(20);
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
        parametersTable.getColumnModel().getColumn(1).setCellEditor(new ParamValueTableCellEditor(this));
        parametersTable.getColumnModel().getColumn(1).setCellRenderer(new ParamValueTableCellRenderer());
    }

    private void createMenuBar() {
        // File menu
        JMenu fileMenu = new JMenu("File");

        JMenuItem fileOpenItem = new JMenuItem("Open Log...");
        fileOpenItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showOpenLogDialog();
            }
        });
        fileMenu.add(fileOpenItem);

        JMenuItem importPresetItem = new JMenuItem("Import Preset...");
        importPresetItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showImportPresetDialog();
            }
        });
        fileMenu.add(importPresetItem);

        JMenuItem exportPresetItem = new JMenuItem("Export Preset...");
        exportPresetItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showExportPresetDialog();
            }
        });
        fileMenu.add(exportPresetItem);

        JMenuItem exportTrackItem = new JMenuItem("Export Track...");
        exportTrackItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exportTrack();
            }
        });
        fileMenu.add(exportTrackItem);

        if (!OSValidator.isMac()) {
            fileMenu.add(new JPopupMenu.Separator());
            JMenuItem exitItem = new JMenuItem("Exit");
            exitItem.setAccelerator(KeyStroke.getKeyStroke('Q', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            exitItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    onQuit();
                }
            });
            fileMenu.add(exitItem);
        }

        // View menu
        JMenu viewMenu = new JMenu("View");
        timeModeItems = new JRadioButtonMenuItem[3];
        timeModeItems[TIME_MODE_LOG_START] = new JRadioButtonMenuItem("Log Start Time");
        timeModeItems[TIME_MODE_BOOT] = new JRadioButtonMenuItem("Boot Time");
        timeModeItems[TIME_MODE_GPS] = new JRadioButtonMenuItem("GPS Time");
        ButtonGroup timeModeGroup = new ButtonGroup();
        for (JRadioButtonMenuItem item : timeModeItems) {
            timeModeGroup.add(item);
            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    onTimeModeChanged();
                    processFile();
                }
            });
            viewMenu.add(item);
        }

        // Menu bar
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(fileMenu);
        menuBar.add(viewMenu);
        mainFrame.setJMenuBar(menuBar);
    }

    private void onTimeModeChanged() {
        int timeModeOld = timeMode;
        for (int i = 0; i < timeModeItems.length; i++) {
            if (timeModeItems[i].isSelected()) {
                timeMode = i;
                break;
            }
        }

        long timeOffset = 0;
        long logStart = 0;
        long logSize = 1000000;
        Range rangeOld = new Range(0.0, 1.0);

        if (logReader != null) {
            timeOffset = getTimeOffset(timeMode);
            logStart = logReader.getStartMicroseconds() + timeOffset;
            logSize = logReader.getSizeMicroseconds();
            rangeOld = getLogRange(timeModeOld);
        }

        ValueAxis domainAxis = selectDomainAxis(timeMode);
        // Set axis type according to selected time mode
        jFreeChart.getXYPlot().setDomainAxis(domainAxis);

        if (domainAxis == domainAxisDate) {
            // DateAxis uses ms instead of seconds
            domainAxis.setRange(rangeOld.getLowerBound() * 1e3 + timeOffset * 1e-3,
                    rangeOld.getUpperBound() * 1e3 + timeOffset * 1e-3);
            domainAxis.setDefaultAutoRange(new Range(logStart * 1e-3, (logStart + logSize) * 1e-3));
        } else {
            domainAxis.setRange(rangeOld.getLowerBound() + timeOffset * 1e-6,
                    rangeOld.getUpperBound() + timeOffset * 1e-6);
            domainAxis.setDefaultAutoRange(new Range(logStart * 1e-6, (logStart + logSize) * 1e-6));
        }
    }

    private Range getLogRange(int tm) {
        Range range = selectDomainAxis(tm).getRange();
        if (tm == TIME_MODE_GPS) {
            long timeOffset = getTimeOffset(tm);
            return new Range((range.getLowerBound() * 1e3 - timeOffset) * 1e-6,
                    (range.getUpperBound() * 1e3 - timeOffset) * 1e-6);
        } else {
            long timeOffset = getTimeOffset(tm);
            return new Range(range.getLowerBound() - timeOffset * 1e-6, range.getUpperBound() - timeOffset * 1e-6);
        }
    }

    public void setStatus(String status) {
        statusLabel.setText(status);
    }

    public void showOpenLogDialog() {
        JFileChooser fc = new JFileChooser();
        if (lastLogDirectory != null) {
            fc.setCurrentDirectory(lastLogDirectory);
        }
        fc.setFileFilter(logExtensionFilter);
        fc.setDialogTitle("Open Log");
        int returnVal = fc.showDialog(mainFrame, "Open");
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            lastLogDirectory = fc.getCurrentDirectory();
            File file = fc.getSelectedFile();
            String logFileName = file.getPath();
            mainFrame.setTitle(appNameAndVersion + " - " + logFileName);
            if (logReader != null) {
                try {
                    logReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                logReader = null;
            }
            try {
                logReader = new PX4LogReader(logFileName);
                logInfo.updateInfo(logReader);
            } catch (Exception e) {
                logReader = null;
                setStatus("Error: " + e);
                e.printStackTrace();
            }
            fieldsListDialog.setFieldsList(logReader.getFields());
            onTimeModeChanged();
            jFreeChart.getXYPlot().getDomainAxis().setAutoRange(true);
            jFreeChart.getXYPlot().getRangeAxis().setAutoRange(true);
            processFile();
        }
    }

    public void showImportPresetDialog() {
        JFileChooser fc = new JFileChooser();
        if (lastPresetDirectory != null) {
            fc.setCurrentDirectory(lastPresetDirectory);
        }
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
                    if (r <= 0) {
                        throw new Exception("Read error");
                    }
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
        if (lastPresetDirectory != null) {
            fc.setCurrentDirectory(lastPresetDirectory);
        }
        fc.setFileFilter(presetExtensionFilter);
        fc.setDialogTitle("Export Preset");
        int returnVal = fc.showDialog(mainFrame, "Export");
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            lastPresetDirectory = fc.getCurrentDirectory();
            String fileName = fc.getSelectedFile().toString();
            if (presetExtensionFilter == fc.getFileFilter() && !fileName.toLowerCase().endsWith(".fplot")) {
                fileName += ".fplot";
            }
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

    public void exportTrack() {
        if (null == this.logReader) {
            JOptionPane.showMessageDialog(mainFrame, "Log file must be opened first.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            ExportData data = new ExportData();
            Range timeAxisRange = jFreeChart.getXYPlot().getDomainAxis().getRange();
            data.setChartRangeFrom((long) (timeAxisRange.getLowerBound() * 1000000));
            data.setChartRangeTo((long) (timeAxisRange.getUpperBound() * 1000000));
            data.setLogReader(this.logReader);

            boolean exportStarted = this.exportManager.export(data, new Runnable() {
                @Override
                public void run() {
                    showExportTrackStatusMessage(FlightPlot.this.exportManager.getLastStatusMessage());
                }
            });
            if (exportStarted) {
                showExportTrackStatusMessage("Exporting...");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showExportTrackStatusMessage("Track could not be exported.");
        }
    }

    private void showExportTrackStatusMessage(String message) {
        setStatus(String.format("Track export: %s", message));
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

    private long getTimeOffset(int tm) {
        // Set time offset according t selected time mode
        long timeOffset = 0;
        if (tm == TIME_MODE_GPS) {
            // GPS time
            timeOffset = logReader.getUTCTimeReferenceMicroseconds();
            if (timeOffset < 0) {
                timeOffset = 0;
            }
        } else if (tm == TIME_MODE_LOG_START) {
            // Log start time
            timeOffset = -logReader.getStartMicroseconds();
        }
        return timeOffset;
    }

    private ValueAxis selectDomainAxis(int tm) {
        if (tm == TIME_MODE_GPS) {
            return domainAxisDate;
        } else {
            return domainAxisSeconds;
        }
    }

    private void generateSeries() throws IOException, FormatErrorException {
        activeProcessors.clear();
        for (int row = 0; row < processorsListModel.getRowCount(); row++) {
            if ((Boolean) processorsListModel.getValueAt(row, 0)) {
                activeProcessors.add((ProcessorPreset) processorsListModel.getValueAt(row, 1));
            }
        }

        dataset.removeAllSeries();
        seriesIndex.clear();
        PlotProcessor[] processors = new PlotProcessor[activeProcessors.size()];

        // Update time offset according to selected time mode
        long timeOffset = getTimeOffset(timeMode);

        // Displayed log range in seconds of native log time
        Range range = getLogRange(timeMode);

        // Process some extra data in hidden areas
        long timeStart = (long) ((range.getLowerBound() - range.getLength()) * 1e6);
        long timeStop = (long) ((range.getUpperBound() + range.getLength()) * 1e6);
        timeStart = Math.max(logReader.getStartMicroseconds(), timeStart);
        timeStop = Math.min(logReader.getStartMicroseconds() + logReader.getSizeMicroseconds(), timeStop);

        double timeScale = (selectDomainAxis(timeMode) == domainAxisDate) ? 1000.0 : 1.0;

        int displayPixels = 2000;
        double skip = range.getLength() / displayPixels;
        if (processors.length > 0) {
            for (int i = 0; i < activeProcessors.size(); i++) {
                ProcessorPreset pp = activeProcessors.get(i);
                PlotProcessor processor;
                try {
                    processor = processorsTypesList.getProcessorInstance(pp, skip, logReader.getFields());
                    processor.setFieldsList(logReader.getFields());
                    processors[i] = processor;
                } catch (Exception e) {
                    e.printStackTrace();
                }
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
                if (t > timeStop) {
                    break;
                }
                for (PlotProcessor processor : processors) {
                    processor.process((t + timeOffset) * 1e-6, data);
                }
            }
            for (int i = 0; i < activeProcessors.size(); i++) {
                PlotProcessor processor = processors[i];
                String processorTitle = activeProcessors.get(i).getTitle();
                Map<String, Integer> processorSeriesIndex = new HashMap<String, Integer>();
                seriesIndex.add(processorSeriesIndex);
                for (Series series : processor.getSeriesList()) {
                    processorSeriesIndex.put(series.getTitle(), dataset.getSeriesCount());
                    XYSeries jseries = new XYSeries(series.getFullTitle(processorTitle), false);
                    for (XYPoint point : series) {
                        jseries.add(point.x * timeScale, point.y);
                    }
                    dataset.addSeries(jseries);
                }
            }
            setChartColors();
        }
        chartPanel.repaint();
    }

    private void setChartColors() {
        if (dataset.getSeriesCount() > 0) {
            for (int i = 0; i < activeProcessors.size(); i++) {
                for (Map.Entry<String, Integer> entry : seriesIndex.get(i).entrySet()) {
                    ProcessorPreset processorPreset = activeProcessors.get(i);
                    jFreeChart.getXYPlot().getRendererForDataset(dataset).setSeriesPaint(entry.getValue(), processorPreset.getColors().get(entry.getKey()));
                }
            }
        }
    }

    private void showAddProcessorDialog(boolean editMode) {
        ProcessorPreset selectedProcessor = editMode ? getSelectedProcessor() : null;
        addProcessorDialog.display(new Runnable() {
            @Override
            public void run() {
                onAddProcessorDialogOK();
            }
        }, selectedProcessor);
    }

    private void onAddProcessorDialogOK() {
        updatePresetEdited(true);
        ProcessorPreset processorPreset = addProcessorDialog.getOrigProcessorPreset();
        String title = addProcessorDialog.getProcessorTitle();
        String processorType = addProcessorDialog.getProcessorType();
        if (processorPreset != null) {
            // Edit processor
            ProcessorPreset processorPresetNew = processorPreset;
            if (!processorPreset.getProcessorType().equals(processorType)) {
                // Processor type changed
                Map<String, Object> parameters = processorPreset.getParameters();
                processorPresetNew = new ProcessorPreset(title, processorType, new HashMap<String, Object>(), Collections.<String, Color>emptyMap());
                updatePresetParameters(processorPresetNew, parameters);
                for (int row = 0; row < processorsListModel.getRowCount(); row++) {
                    if (processorsListModel.getValueAt(row, 1) == processorPreset) {
                        processorsListModel.setValueAt(processorPresetNew, row, 1);
                        processorsList.setRowSelectionInterval(row, row);
                        break;
                    }
                }
                showProcessorParameters();
            } else {
                // Only change title
                processorPresetNew.setTitle(title);
            }
        } else {
            processorPreset = new ProcessorPreset(title, processorType, Collections.<String, Object>emptyMap(), Collections.<String, Color>emptyMap());
            updatePresetParameters(processorPreset, null);
            int i = processorsListModel.getRowCount();
            processorsListModel.addRow(new Object[]{true, processorPreset});
            processorsList.setRowSelectionInterval(i, i);
        }
        updateUsedColors();
        processFile();
    }

    private void updatePresetParameters(ProcessorPreset processorPreset, Map<String, Object> parametersUpdate) {
        if (parametersUpdate != null) {
            // Update parameters of preset
            processorPreset.getParameters().putAll(parametersUpdate);
        }
        // Construct and initialize processor to cleanup parameters list and get list of series
        PlotProcessor p;
        try {
            p = processorsTypesList.getProcessorInstance(processorPreset, 0.0, null);
        } catch (Exception e) {
            setStatus("Error in processor \"" + processorPreset + "\"");
            e.printStackTrace();
            return;
        }
        processorPreset.setParameters(p.getParameters());
        Map<String, Color> colorsNew = new HashMap<String, Color>();
        for (Series series : p.getSeriesList()) {
            Color color = processorPreset.getColors().get(series.getTitle());
            if (color == null) {
                color = colorSupplier.getNextColor(series.getTitle());
            }
            colorsNew.put(series.getTitle(), color);
        }
        processorPreset.setColors(colorsNew);
    }

    private void removeSelectedProcessor() {
        ProcessorPreset selectedProcessor = getSelectedProcessor();
        if (selectedProcessor != null) {
            int row = processorsList.getSelectedRow();
            processorsListModel.removeRow(row);
            updatePresetEdited(true);
            updateUsedColors();
            processFile();
        }
    }

    private void updateUsedColors() {
        colorSupplier.resetColorsUsed();
        for (int i = 0; i < processorsListModel.getRowCount(); i++) {
            ProcessorPreset pp = (ProcessorPreset) processorsListModel.getValueAt(i, 1);
            for (Color color : pp.getColors().values()) {
                colorSupplier.markColorUsed(color);
            }
        }
    }

    private ProcessorPreset getSelectedProcessor() {
        int row = processorsList.getSelectedRow();
        return row < 0 ? null : (ProcessorPreset) processorsListModel.getValueAt(row, 1);
    }

    private void showProcessorParameters() {
        while (parametersTableModel.getRowCount() > 0) {
            parametersTableModel.removeRow(0);
        }
        ProcessorPreset selectedProcessor = getSelectedProcessor();
        if (selectedProcessor != null) {
            // Parameters
            Map<String, Object> params = selectedProcessor.getParameters();
            List<String> param_keys = new ArrayList<String>(params.keySet());
            Collections.sort(param_keys);
            for (String key : param_keys) {
                parametersTableModel.addRow(new Object[]{key, formatParameterValue(params.get(key))});
            }
            // Colors
            Map<String, Color> colors = selectedProcessor.getColors();
            List<String> color_keys = new ArrayList<String>(colors.keySet());
            Collections.sort(color_keys);
            for (String key : color_keys) {
                parametersTableModel.addRow(new Object[]{colorParamPrefix + key, colors.get(key)});
            }
        }
    }

    private void onParameterChanged(int row) {
        if (editingProcessor != null) {
            String key = parametersTableModel.getValueAt(row, 0).toString();
            Object value = parametersTableModel.getValueAt(row, 1);
            if (value instanceof Color) {
                editingProcessor.getColors().put(key.substring(colorParamPrefix.length(), key.length()), (Color) value);
                setChartColors();
            } else {
                try {
                    updatePresetParameters(editingProcessor, Collections.<String, Object>singletonMap(key, value.toString()));
                    updatePresetEdited(true);
                } catch (Exception e) {
                    e.printStackTrace();
                    setStatus("Error: " + e);
                }
                parametersTableModel.removeTableModelListener(parameterChangedListener);
                showProcessorParameters(); // refresh all parameters because changing one param might influence others (e.g. color)
                parametersTableModel.addTableModelListener(parameterChangedListener);
                parametersTable.addRowSelectionInterval(row, row);
                processFile();
            }
        }
    }

    ColorSupplier getColorSupplier() {
        return colorSupplier;
    }

    void setEditingProcessor() {
        editingProcessor = getSelectedProcessor();
    }
}
