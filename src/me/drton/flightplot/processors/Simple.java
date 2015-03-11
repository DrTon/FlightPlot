package me.drton.flightplot.processors;

import me.drton.flightplot.processors.tools.LowPassFilter;

import java.util.HashMap;
import java.util.Map;

/**
 * User: ton Date: 15.06.13 Time: 12:04
 */
public class Simple extends PlotProcessor {
    protected String[] param_Fields;
    protected double param_Scale;
    protected double param_Offset;
    protected double param_Delay;
    protected LowPassFilter[] lowPassFilters;

    @Override
    public Map<String, Object> getDefaultParameters() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("Fields", "ATT.Pitch ATT.Roll");
        params.put("Delay", 0.0);
        params.put("LPF", 0.0);
        params.put("Scale", 1.0);
        params.put("Offset", 0.0);
        return params;
    }

    @Override
    public void init() {
        param_Fields = ((String) parameters.get("Fields")).split(WHITESPACE_RE);
        param_Scale = (Double) parameters.get("Scale");
        param_Offset = (Double) parameters.get("Offset");
        param_Delay = (Double) parameters.get("Delay");
        lowPassFilters = new LowPassFilter[param_Fields.length];
        for (int i = 0; i < param_Fields.length; i++) {
            LowPassFilter lowPassFilter = new LowPassFilter();
            lowPassFilter.setF((Double) parameters.get("LPF"));
            lowPassFilters[i] = lowPassFilter;
        }
        for (String field : param_Fields) {
            addSeries(field);
        }
    }

    protected double preProcessValue(int idx, double time, double in) {
        return in;
    }

    protected double postProcessValue(int idx, double time, double in) {
        return in;
    }

    @Override
    public void process(double time, Map<String, Object> update) {
        for (int i = 0; i < param_Fields.length; i++) {
            String field = param_Fields[i];
            Object v = update.get(field);
            if (v != null && v instanceof Number) {
                double out = preProcessValue(i, time, ((Number) v).doubleValue());
                if (Double.isNaN(out)) {
                    addPoint(i, time, Double.NaN);
                } else {
                    out = lowPassFilters[i].getOutput(time, out);
                    out = postProcessValue(i, time, out);
                    addPoint(i, time + param_Delay, out * param_Scale + param_Offset);
                }
            }
        }
    }
}
