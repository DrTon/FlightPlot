package me.drton.flightplot.processors;

import java.util.HashMap;
import java.util.Map;

/**
 * User: ton Date: 24.06.13 Time: 22:46
 */
public class Derivative extends PlotProcessor {
    protected String param_Field;
    protected double param_Scale;
    private double valuePrev;
    private double timePrev;

    @Override
    public Map<String, Object> getDefaultParameters() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("Field", "LPOS.Z");
        params.put("Scale", 1.0);
        return params;
    }

    @Override
    public void init() {
        super.init();
        valuePrev = Double.NaN;
        timePrev = Double.NaN;
        param_Field = (String) parameters.get("Field");
        param_Scale = (Double) parameters.get("Scale");
        addSeries();
    }

    @Override
    public void process(double time, Map<String, Object> update) {
        Object v = update.get(param_Field);
        if (v != null && v instanceof Number) {
            double value = ((Number) v).doubleValue();
            if (!Double.isNaN(timePrev)) {
                double dt = time - timePrev;
                if (dt > 0.0) {
                    addPoint(0, time, (value - valuePrev) / dt * param_Scale);
                }
            }
            timePrev = time;
            valuePrev = value;
        }
    }
}
