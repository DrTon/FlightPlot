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
    protected Map<String, String> fieldsList = new HashMap<String, String>();
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

    public void setFieldsList(Map<String, String> fieldsList) {
        this.fieldsList = fieldsList;
    }

    private static Object castValue(Object valueOld, Object valueNewObj) {
        String valueNewStr = valueNewObj.toString();
        Object valueNew = valueNewObj;
        if (valueOld instanceof String) {
            valueNew = valueNewStr;
        } else if (valueOld instanceof Double) {
            valueNew = Double.parseDouble(valueNewStr);
        } else if (valueOld instanceof Float) {
            valueNew = Float.parseFloat(valueNewStr);
        } else if (valueOld instanceof Integer) {
            valueNew = Integer.parseInt(valueNewStr);
        } else if (valueOld instanceof Long) {
            valueNew = Long.parseLong(valueNewStr);
        } else if (valueOld instanceof Boolean) {
            char firstChar = valueNewStr.toLowerCase().charAt(0);
            if (firstChar == 'f' || firstChar == 'n' || "0".equals(valueNewStr))
                valueNew = false;
            else
                valueNew = true;
        }
        return valueNew;
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
                parameters.put(key, castValue(oldValue, newValue));
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
