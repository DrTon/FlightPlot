package me.drton.flightplot.processors;

import me.drton.flightplot.processors.tools.LowPassFilter;
import me.drton.flightplot.processors.tools.PID;

import java.util.HashMap;
import java.util.Map;

/**
 * User: ton Date: 20.06.13 Time: 6:06
 */
public class PosPIDControlSimulator extends PlotProcessor {
    private double startTime;
    private double startSP;
    private double startSPRate;
    private double thrustK;
    private double accScale;
    private double drag;
    private double awuRate;

    private LowPassFilter propeller = new LowPassFilter();
    private PID pidPos = new PID();
    private double pos;
    private double rate;
    private double posSP;
    private double timePrev;

    @Override
    public Map<String, Object> getDefaultParameters() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("Start Time", 100.0);
        params.put("Start SP", 1.0);
        params.put("Start SP Rate", 1.0);
        params.put("Thrust T", 0.03);
        params.put("Thrust K", 40.0);
        params.put("Ctrl P", 0.1);
        params.put("Ctrl I", 0.05);
        params.put("Ctrl D", 0.1);
        params.put("Ctrl Limit", 0.2);
        params.put("AWU Rate", 1.0);
        params.put("Acc Scale", 1.0);
        params.put("Drag", 0.0);
        return params;
    }

    @Override
    public void init() {
        propeller.reset();
        pos = 0.0;
        rate = 0.0;
        posSP = 0.0;
        timePrev = Double.NaN;
        startTime = (Double) parameters.get("Start Time");
        startSP = (Double) parameters.get("Start SP");
        startSPRate = (Double) parameters.get("Start SP Rate");
        thrustK = (Double) parameters.get("Thrust K");
        accScale = (Double) parameters.get("Acc Scale");
        drag = (Double) parameters.get("Drag");
        awuRate = (Double) parameters.get("AWU Rate");
        propeller.setT((Double) parameters.get("Thrust T"));
        pidPos.reset();
        pidPos.setK((Double) parameters.get("Ctrl P"), (Double) parameters.get("Ctrl I"),
                (Double) parameters.get("Ctrl D"), (Double) parameters.get("Ctrl Limit"), PID.MODE.DERIVATIVE_CALC);
        addSeries("Pos");
        addSeries("Rate");
        addSeries("Acc");
        addSeries("Ctrl");
    }

    @Override
    public void process(double time, Map<String, Object> update) {
        if (update.containsKey("ATT.Roll")) {   // Act only on attitude updates
            if (!Double.isNaN(timePrev)) {
                double dt = time - timePrev;
                double spRate = 0.0;
                if (time > startTime && posSP < startSP) {
                    spRate = startSPRate;
                    posSP += startSPRate * dt;
                }
                double force = propeller.getOutput(time, 0.0);
                double acc = force * thrustK - drag * rate;
                rate += acc * dt;
                pos += rate * dt;
                double awuW =
                        awuRate == 0.0 ? 1.0 : Math.exp(-(spRate * spRate + rate * rate) / 2.0 / awuRate / awuRate);
                double thrustControl = pidPos.getOutput(posSP, pos, spRate - rate, dt, awuW);
                propeller.setInput(thrustControl);
                addPoint(0, time, pos);
                addPoint(1, time, rate);
                if (accScale != 0.0)
                    addPoint(2, time, acc * accScale);
                addPoint(3, time, pidPos.getIntegral() * 10);
            }
            timePrev = time;
        }
    }
}
