package me.drton.flightplot.processors;

import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.util.Map;

/**
 * User: ton Date: 12.06.13 Time: 18:25
 */
public abstract class PlotProcessor {
    protected static String WHITESPACE_RE = "[ \t]+";

    private String title;
    protected Map<String, Object> parameters;

    protected PlotProcessor() {
        this.parameters = getDefaultParameters();
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

    public void setParameters(Map<String, Object> params) {
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            Object oldValue = parameters.get(key);
            Object newValue = params.get(key);
            if (oldValue != null) {
                parameters.put(key, castStringValue(oldValue, newValue));
            }
        }
    }

    private Object castStringValue(Object valueOld, Object valueNewObj) {
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
            valueNew = Boolean.parseBoolean(valueNewStr);
        }
        return valueNew;
    }

    protected XYSeries createSeries() {
        return new XYSeries(getTitle(), false);
    }

    protected XYSeries createSeries(String label) {
        return new XYSeries(title + ":" + label, false);
    }

    public abstract void init();

    public abstract void process(double time, Map<String, Object> update);

    public abstract XYSeriesCollection getSeriesCollection();

    public String getProcessorType() {
        return getClass().getSimpleName();
    }

    @Override
    public String toString() {
        return title + " [" + getProcessorType() + "]";
    }
}
