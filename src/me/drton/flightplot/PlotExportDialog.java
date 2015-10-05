package me.drton.flightplot;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.prefs.Preferences;

public class PlotExportDialog extends JDialog {
    private static final String DIALOG_SETTING = "PlotExportDialog";
    private static final String LAST_EXPORT_DIRECTORY_SETTING = "LastExportDirectory";

    private JPanel contentPane;
    private JButton buttonExport;
    private JButton buttonClose;
    private JTextField widthField;
    private JTextField heightField;
    private JComboBox formatComboBox;
    private JTextField scaleField;
    private FlightPlot app;
    private File lastExportDirectory;

    public PlotExportDialog(FlightPlot app) {
        this.app = app;
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonExport);

        buttonExport.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonClose.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onClose();
            }
        });

        // call onClose() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onClose();
            }
        });

        // call onClose() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onClose();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        pack();
    }

    private void onOK() {
        String format = ((String) formatComboBox.getSelectedItem()).toLowerCase();
        JFileChooser fc = new JFileChooser();
        if (lastExportDirectory != null) {
            fc.setCurrentDirectory(lastExportDirectory);
        }
        FileNameExtensionFilter extensionFilter = new FileNameExtensionFilter(format.toUpperCase() + " Image (*." + format + ")", format);
        fc.setFileFilter(extensionFilter);
        fc.setDialogTitle("Export Plot");
        int returnVal = fc.showDialog(null, "Export Plot");
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            lastExportDirectory = fc.getCurrentDirectory();
            String fileName = fc.getSelectedFile().toString();
            if (extensionFilter == fc.getFileFilter() && !fileName.toLowerCase().endsWith("." + format)) {
                fileName += "." + format;
            }
            try {
                int width = Integer.parseInt(widthField.getText());
                int height = Integer.parseInt(heightField.getText());

                BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2 = img.createGraphics();
                double scale = Double.parseDouble(scaleField.getText());
                AffineTransform st = AffineTransform.getScaleInstance(scale, scale);
                g2.transform(st);
                app.getChart().draw(g2, new Rectangle2D.Double(0.0D, 0.0D, width / scale, height / scale), null, null);
                g2.dispose();

                ImageWriter imgWriter = ImageIO.getImageWritersByFormatName(format).next();
                ImageWriteParam imgWriteParam = imgWriter.getDefaultWriteParam();
                if ("jpg".equals(format)) {
                    imgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    imgWriteParam.setCompressionQuality(1.0f);
                }
                ImageOutputStream outputStream = new FileImageOutputStream(new File(fileName));
                imgWriter.setOutput(outputStream);
                IIOImage outputImage = new IIOImage(img, null, null);
                imgWriter.write(null, outputImage, imgWriteParam);
                imgWriter.dispose();

                app.setStatus(String.format("Exported to \"%s\"", fileName));

            } catch (Exception e) {
                app.setStatus("Error: " + e);
                e.printStackTrace();
            }
        }
        dispose();
    }

    private void onClose() {
        dispose();
    }

    public void savePreferences(Preferences preferences) {
        PreferencesUtil.saveWindowPreferences(this, preferences.node(DIALOG_SETTING));
        if (lastExportDirectory != null) {
            preferences.put(LAST_EXPORT_DIRECTORY_SETTING, lastExportDirectory.getAbsolutePath());
        }
    }

    public void loadPreferences(Preferences preferences) {
        PreferencesUtil.loadWindowPreferences(this, preferences.node(DIALOG_SETTING), -1, -1);
        String lastExportDirectoryPath = preferences.get(LAST_EXPORT_DIRECTORY_SETTING, null);
        if (null != lastExportDirectoryPath) {
            lastExportDirectory = new File(lastExportDirectoryPath);
        }
    }
}
