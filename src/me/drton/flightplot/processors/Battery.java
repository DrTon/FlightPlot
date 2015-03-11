package me.drton.flightplot.processors;

import me.drton.flightplot.processors.tools.LowPassFilter;

import java.util.HashMap;
import java.util.Map;

/**
 * User: ton Date: 10.11.13 Time: 16:50
 */
public class Battery extends PlotProcessor {
    private String param_Field_Voltage;
    private String param_Field_Current;
    private String param_Field_Discharged;
    private double param_Capacity;
    private double param_Resistance;
    private double param_N_Cells;
    private double param_V_Empty;
    private double param_V_Full;
    private boolean showV;
    private boolean showRemainingV;
    private boolean showRemainingC;
    private LowPassFilter lpf;

    @Override
    public Map<String, Object> getDefaultParameters() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("Field Voltage", "BATT.V");
        params.put("Field Current", "BATT.C");
        params.put("Field Discharged", "BATT.Discharged");
        params.put("Capacity", 2200.0);
        params.put("Resistance", 0.03);
        params.put("N Cells", 3);
        params.put("V Empty", 3.7);
        params.put("V Full", 4.0);
        params.put("LPF", 1.0);
        params.put("Show", "VC");
        return params;
    }

    @Override
    public void init() {
        param_Field_Voltage = (String) parameters.get("Field Voltage");
        param_Field_Current = (String) parameters.get("Field Current");
        param_Field_Discharged = (String) parameters.get("Field Discharged");
        param_Capacity = (Double) parameters.get("Capacity");
        param_Resistance = (Double) parameters.get("Resistance");
        param_N_Cells = (Integer) parameters.get("N Cells");
        param_V_Empty = (Double) parameters.get("V Empty");
        param_V_Full = (Double) parameters.get("V Full");
        lpf = new LowPassFilter();
        lpf.setF((Double) parameters.get("LPF"));
        String show = ((String) parameters.get("Show")).toUpperCase();
        showRemainingV = show.contains("V");
        showRemainingC = show.contains("C");
        addSeries("RemainingV");
        addSeries("RemainingC");
    }

    @Override
    public void process(double time, Map<String, Object> update) {
        Number voltageNum = (Number) update.get(param_Field_Voltage);
        Number currentNum = (Number) update.get(param_Field_Current);
        Number dischargedNum = (Number) update.get(param_Field_Discharged);
        if (voltageNum != null) {
            double v = voltageNum.doubleValue();
            double vFiltered = v;
            if (currentNum != null) {
                double current = currentNum.doubleValue();
                if (current > 0.0) {
                    // current < 0 means not available
                    vFiltered += current * param_Resistance;
                }
            }
            vFiltered = lpf.getOutput(time, vFiltered);
            double remainingV = Math.min(1.0,
                    Math.max(0.0, (vFiltered / param_N_Cells - param_V_Empty) / (param_V_Full - param_V_Empty)));
            if (showRemainingV)
                addPoint(0, time, remainingV * 100.0);
            if (dischargedNum != null) {
                double discharged = dischargedNum.doubleValue();
                if (discharged > 0.0) {
                    double remainingC = Math.min(1.0, Math.max(0.0, 1.0 - discharged / param_Capacity));
                    if (showRemainingC)
                        addPoint(1, time, remainingC * 100.0);
                }
            }
        }
    }
}
