package me.drton.flightplot.processors;

import org.jfree.data.xy.XYSeriesCollection;

import java.util.HashMap;
import java.util.Map;

/**
 * User: ton Date: 15.06.13 Time: 12:04
 */
public class Simple extends PlotProcessor {
    private String[] param_Fields;
    private double param_Scale;
    private XYSeriesCollection seriesCollection;

    @Override
    public Map<String, Object> getDefaultParameters() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("Fields", "ATT.Pitch ATT.Roll");
        params.put("Scale", 1.0);
        return params;
    }

    @Override
    public void init() {
        param_Fields = ((String) parameters.get("Fields")).split(" +");
        param_Scale = (Double) parameters.get("Scale");
        seriesCollection = new XYSeriesCollection();
        for (String field : param_Fields) {
            seriesCollection.addSeries(createSeries(field));
        }
    }

    @Override
    public void process(double time, Map<String, Object> update) {
        for (int i = 0; i < param_Fields.length; i++) {
            String field = param_Fields[i];
            Object v = update.get(field);
            if (v != null && v instanceof Number) {
                seriesCollection.getSeries(i).add(time, ((Number) v).doubleValue() * param_Scale);
            }
        }
    }

    @Override
    public XYSeriesCollection getSeriesCollection() {
        return seriesCollection;
    }
}
