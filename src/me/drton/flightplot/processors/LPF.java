package me.drton.flightplot.processors;

import me.drton.flightplot.processors.tools.LowPassFilter;

import java.util.Map;

/**
 * User: ton Date: 16.06.13 Time: 12:18
 */
public class LPF extends Simple {
    private double param_F;
    private LowPassFilter[] filters;

    @Override
    public Map<String, Object> getDefaultParameters() {
        Map<String, Object> params = super.getDefaultParameters();
        params.put("Fields", "SENS.BaroAlt");
        params.put("F", 10.0);
        return params;
    }

    @Override
    public void init() {
        super.init();
        param_F = (Double) parameters.get("F");
        filters = new LowPassFilter[param_Fields.length];
        for (int i = 0; i < filters.length; i++) {
            LowPassFilter filter = new LowPassFilter();
            filter.setF(param_F);
            filters[i] = filter;
        }
    }

    @Override
    public void process(double time, Map<String, Object> update) {
        for (int i = 0; i < param_Fields.length; i++) {
            String field = param_Fields[i];
            Object v = update.get(field);
            if (v != null && v instanceof Number) {
                double filtered = filters[i].getOutput(time, ((Number) v).doubleValue());
                seriesCollection.getSeries(i).add(time, filtered * param_Scale + param_Offset);
            }
        }
    }
}
