package me.drton.flightplot.processors;

import me.drton.flightplot.processors.tools.DispersionTracker;

import java.util.HashMap;
import java.util.Map;

/**
 * User: ton Date: 24.06.13 Time: 22:46
 */
public class Dispersion extends PlotProcessor {
    private DispersionTracker[] dispersionTrackers;
    private String[] param_Fields;
    private double param_Scale;

    @Override
    public Map<String, Object> getDefaultParameters() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("Fields", "IMU.AccZ");
        params.put("LPF", 20.0);
        params.put("HPF", 1.0);
        params.put("Scale", 1.0);
        return params;
    }

    @Override
    public void init() {
        param_Fields = ((String) parameters.get("Fields")).split(WHITESPACE_RE);
        dispersionTrackers = new DispersionTracker[param_Fields.length];
        for (int i = 0; i < param_Fields.length; ++i) {
            DispersionTracker tracker = new DispersionTracker();
            tracker.setCutoffFreq((Double) parameters.get("HPF"), (Double) parameters.get("LPF"));
            dispersionTrackers[i] = tracker;
        }
        for (String field : param_Fields) {
            addSeries(field);
        }
        param_Scale = (Double) parameters.get("Scale");
    }

    @Override
    public void process(double time, Map<String, Object> update) {
        for (int i = 0; i < param_Fields.length; i++) {
            String field = param_Fields[i];
            Object v = update.get(field);
            if (v != null && v instanceof Number) {
                double in = ((Number) v).doubleValue();
                double out = dispersionTrackers[i].getOutput(time, in);
                addPoint(i, time, Math.sqrt(out) * param_Scale);
            }
        }
    }
}
