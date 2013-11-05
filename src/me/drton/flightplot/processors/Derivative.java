package me.drton.flightplot.processors;

import java.util.Map;

/**
 * User: ton Date: 24.06.13 Time: 22:46
 */
public class Derivative extends Simple {
    private double[] valuesPrev;
    private double[] timesPrev;

    @Override
    public Map<String, Object> getDefaultParameters() {
        Map<String, Object> params = super.getDefaultParameters();
        params.put("Fields", "LPOS.VX LPOS.VY");
        return params;
    }

    @Override
    public void init() {
        super.init();
        valuesPrev = new double[param_Fields.length];
        timesPrev = new double[param_Fields.length];
    }

    @Override
    protected double postProcessValue(int idx, double time, double in) {
        double out;
        if (!Double.isNaN(timesPrev[idx])) {
            out = (in - valuesPrev[idx]) / (time - timesPrev[idx]);
        } else {
            out = Double.NaN;
        }
        valuesPrev[idx] = in;
        timesPrev[idx] = time;
        return out;
    }
}
