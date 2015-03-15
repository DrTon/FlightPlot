package me.drton.flightplot.processors;

import me.drton.flightplot.processors.tools.LowPassFilter;
import net.objecthunter.exp4j.ExpressionBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by markw on 1/22/15.
 */
public class Expression extends PlotProcessor {
    protected final Map<String, Object> data = new HashMap<String, Object>();
    protected LowPassFilter lowPassFilter;
    private net.objecthunter.exp4j.Expression expr;

    @Override
    public Map<String, Object> getDefaultParameters() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("Expression", "BATT.C * BATT.V");
        params.put("LPF", 0.0);
        return params;
    }

    @Override
    public void init() {
        String exprStr = (String) parameters.get("Expression");
        expr = null;
        ExpressionBuilder expBuilder = new ExpressionBuilder(exprStr);
        if (fieldsList != null) {
            expBuilder.variables(fieldsList.keySet());
            try {
                expr = expBuilder.build();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        lowPassFilter = new LowPassFilter();
        lowPassFilter.setF((Double) parameters.get("LPF"));
        addSeries();
    }

    @Override
    public void process(double time, Map<String, Object> update) {
        if (expr == null) {
            return;
        }
        for (Map.Entry<String, Object> entry : update.entrySet()) {
            Object val = entry.getValue();
            if (val != null && val instanceof Number) {
                expr.setVariable(entry.getKey(), ((Number) val).doubleValue());
            }
        }
        double res;
        try {
            res = expr.evaluate();
        } catch (Exception e) {
            return;
        }
        addPoint(0, time, lowPassFilter.getOutput(time, res));

    }
}
