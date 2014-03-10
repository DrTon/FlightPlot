package me.drton.flightplot.processors;

import java.util.Map;

/**
 * User: ton Date: 04.11.13 Time: 23:11
 */
public class Integral extends Simple {
    private double param_In_Offset;
    private double[] integrals;
    private double[] times;

    @Override
    public Map<String, Object> getDefaultParameters() {
        Map<String, Object> params = super.getDefaultParameters();
        params.put("In Offset", 0.0);
        return params;
    }

    @Override
    public void init() {
        super.init();
        param_In_Offset = (Double) parameters.get("In Offset");
        integrals = new double[param_Fields.length];
        times = new double[param_Fields.length];
        for (int i = 0; i < integrals.length; i++) {
            integrals[i] = 0.0;
            times[i] = Double.NaN;
        }
    }

    @Override
    protected double preProcessValue(int idx, double time, double in) {
        if (!Double.isNaN(times[idx])) {
            integrals[idx] += (in + param_In_Offset) * (time - times[idx]);
        }
        times[idx] = time;
        return integrals[idx];
    }
}
