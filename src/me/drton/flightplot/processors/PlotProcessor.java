package me.drton.flightplot.processors;

import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * User: ton Date: 12.06.13 Time: 18:25
 */
public abstract class PlotProcessor {
    protected static final String WHITESPACE_RE = "[ \t]+";
    private double skipOut = 0.0;
    private XYSeriesCollection seriesCollection;
    private List<Double> lastUpdates;
    private List<Double> lastValues;

    private String title;
    protected Map<String, Object> parameters;

    protected PlotProcessor() {
        this.parameters = getDefaultParameters();
    }

    public void init() {
        seriesCollection = new XYSeriesCollection();
        lastUpdates = new ArrayList<Double>();
        lastValues = new ArrayList<Double>();
    }

    public void setSkipOut(double skipOut) {
        this.skipOut = skipOut;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public abstract Map<String, Object> getDefaultParameters();

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameter(String key, Object value) {
        Object oldValue = parameters.get(key);
        if (oldValue != null) {
            parameters.put(key, castValue(oldValue, value));
        }
    }

    public void setParameters(Map<String, Object> params) {
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            Object oldValue = parameters.get(key);
            Object newValue = params.get(key);
            if (oldValue != null) {
                parameters.put(key, castValue(oldValue, newValue));
            }
        }
    }

    private static Object castValue(Object valueOld, Object valueNewObj) {
        String valueNewStr = valueNewObj.toString();
        Object valueNew = null;
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

    protected int addSeries() {
        int idx = seriesCollection.getSeriesCount();
        seriesCollection.addSeries(new XYSeries(getTitle(), false));
        lastUpdates.add(null);
        lastValues.add(null);
        return idx;
    }

    protected int addSeries(String label) {
        int idx = seriesCollection.getSeriesCount();
        seriesCollection.addSeries(new XYSeries(title + ":" + label, false));
        lastUpdates.add(null);
        lastValues.add(null);
        return idx;
    }

    protected void addPoint(int seriesIdx, double time, double value) {
        Double lastUpdate = lastUpdates.get(seriesIdx);
        if (lastUpdate != null && time - lastUpdate < skipOut) {
            lastValues.set(seriesIdx, value);
            return;
        }
        Double lastValue = lastValues.get(seriesIdx);
        if (lastValue != null && lastUpdate != null && time - lastUpdate > skipOut * 2) {
            seriesCollection.getSeries(seriesIdx).add(lastUpdate, lastValue);
        }
        lastValues.set(seriesIdx, null);
        lastUpdates.set(seriesIdx, time);
        seriesCollection.getSeries(seriesIdx).add(time, value);
    }

    public abstract void process(double time, Map<String, Object> update);

    public XYSeriesCollection getSeriesCollection() {
        return seriesCollection;
    }

    public String getProcessorType() {
        return getClass().getSimpleName();
    }

    @Override
    public String toString() {
        return title + " [" + getProcessorType() + "]";
    }
}
