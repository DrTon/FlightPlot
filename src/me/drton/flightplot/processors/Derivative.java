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
        for (int i = 0; i < param_Fields.length; i++) {
            valuesPrev[i] = Double.NaN;
            timesPrev[i] = Double.NaN;
        }
    }

    @Override
    protected double postProcessValue(int idx, double time, double in) {
        double out = Double.NaN;
        if (!Double.isNaN(timesPrev[idx])) {
            double dt = time - timesPrev[idx];
            if (dt > 1.0e-5) {
                out = (in - valuesPrev[idx]) / dt;
            }
        }
        valuesPrev[idx] = in;
        timesPrev[idx] = time;
        return out;
    }
}
