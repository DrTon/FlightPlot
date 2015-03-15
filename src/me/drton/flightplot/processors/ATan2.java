package me.drton.flightplot.processors;

import java.util.HashMap;
import java.util.Map;

/**
 * User: ton Date: 16.06.13 Time: 19:55
 */
public class ATan2 extends PlotProcessor {
    protected String param_Field_X;
    protected String param_Field_Y;
    protected double param_Angle_Offset;

    @Override
    public Map<String, Object> getDefaultParameters() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("Field_X", "LPOS.VX");
        params.put("Field_Y", "LPOS.VY");
        params.put("Angle Offset", 0.0);
        return params;
    }

    @Override
    public void init() {
        param_Field_X = (String) parameters.get("Field_X");
        param_Field_Y = (String) parameters.get("Field_Y");
        param_Angle_Offset = (Double) parameters.get("Angle Offset");
        addSeries();
    }

    @Override
    public void process(double time, Map<String, Object> update) {
        Object x = update.get(param_Field_X);
        Object y = update.get(param_Field_Y);
        if (x != null && y != null && x instanceof Number && y instanceof Number) {
            double a = Math.atan2(((Number) y).doubleValue(), ((Number) x).doubleValue());
            a += param_Angle_Offset + Math.PI;
            int a_2pi = (int) Math.round(a / 2.0 / Math.PI - 0.5);
            a -= (a_2pi * 2.0 + 1.0) * Math.PI;
            addPoint(0, time, a);
        }
    }
}
