package me.drton.flightplot.processors;

import me.drton.flightplot.Series;
import me.drton.flightplot.XYPoint;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: ton Date: 12.06.13 Time: 18:25
 */
public abstract class PlotProcessor {
    protected static final String WHITESPACE_RE = "[ \t]+";
    protected Map<String, Object> parameters;
    private double skipOut = 0.0;
    private List<Series> seriesList = new ArrayList<Series>();
    private List<XYPoint> lastPoints = new ArrayList<XYPoint>();

    protected PlotProcessor() {
        this.parameters = getDefaultParameters();
    }

    public abstract void init();

    public void setSkipOut(double skipOut) {
        this.skipOut = skipOut;
    }

    public abstract Map<String, Object> getDefaultParameters();

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parametersNew) {
        for (Map.Entry<String, Object> entry : parametersNew.entrySet()) {
            String key = entry.getKey();
            Object oldValue = parameters.get(key);
            Object newValue = parametersNew.get(key);
            if (oldValue != null) {
                parameters.put(key, newValue);
            }
        }
    }

    public Map<String, Color> getColors() {
        Map<String, Color> colors = new HashMap<String, Color>();
        for (Series series : seriesList) {
            colors.put(series.getTitle(), series.getColor());
        }
        return colors;
    }

    public void setColors(Map<String, Color> colors) {
        for (Map.Entry<String, Color> entry : colors.entrySet()) {
            for (Series series : seriesList) {
                if (series.getTitle().equals(entry.getKey())) {
                    series.setColor(entry.getValue());
                }
            }
        }
    }

    protected int addSeries() {
        int idx = seriesList.size();
        seriesList.add(new Series("", skipOut));
        lastPoints.add(null);
        return idx;
    }

    protected int addSeries(String label) {
        int idx = seriesList.size();
        seriesList.add(new Series(label, skipOut));
        lastPoints.add(null);
        return idx;
    }

    public List<Series> getSeriesList() {
        return seriesList;
    }

    protected void addPoint(int seriesIdx, double time, double value) {
        seriesList.get(seriesIdx).addPoint(time, value);
    }

    public abstract void process(double time, Map<String, Object> update);

    public String getProcessorType() {
        return getClass().getSimpleName();
    }
}
