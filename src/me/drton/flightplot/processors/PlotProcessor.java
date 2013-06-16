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
        parameters = params;
    }

    protected XYSeries createSeries() {
        return new XYSeries(getTitle(), false);
    }

    protected XYSeries createSeries(String label) {
        return new XYSeries(getTitle() + ":" + label, false);
    }

    public abstract void init();

    public abstract void process(double time, Map<String, Object> update);

    public abstract XYSeriesCollection getSeriesCollection();
}
