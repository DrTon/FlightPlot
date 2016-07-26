package me.drton.flightplot.processors;

import me.drton.flightplot.processors.tools.LowPassFilter;
import me.drton.flightplot.processors.tools.PID;
import me.drton.jmavlib.conversion.RotationConversion;
import me.drton.jmavlib.processing.Batterworth2pLPF;
import me.drton.jmavlib.processing.DelayLine;
import me.drton.jmavlib.processing.Filter;

import java.util.HashMap;
import java.util.Map;

/**
 * User: ton Date: 20.06.13 Time: 6:06
 */
public class PosRatePIDControlSimulator extends PlotProcessor {
    private double timeStep;
    private double thrustK;
    private double accScale;
    private double drag;
    private DelayLine<Double> delayLine = new DelayLine<Double>();
    private Filter rateLPF = new Batterworth2pLPF();
    private LowPassFilter controlLPF = new LowPassFilter();
    private PID pidPos = new PID();
    private PID pidRate = new PID();
    private double pos;
    private double rate;
    private double posSP;
    private boolean useRateSP;
    private double rateSP;
    private double timePrev;
    private String spField;
    private String rateSpField;

    @Override
    public Map<String, Object> getDefaultParameters() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("Time Step", 0.004);
        params.put("Thrust T", 0.03);
        params.put("Thrust Delay", 0.05);
        params.put("Thrust K", 200.0);
        params.put("Ctrl P", 5.0);
        params.put("Ctrl D", 0.0);
        params.put("Ctrl Limit", 0.0);
        params.put("Ctrl Rate P", 0.1);
        params.put("Ctrl Rate I", 0.0);
        params.put("Ctrl Rate D", 0.0);
        params.put("Ctrl Rate D SP", false);
        params.put("Ctrl Rate Limit", 0.0);
        params.put("Acc Scale", 1.0);
        params.put("Drag", 0.0);
        params.put("Use Rate SP", false);
        params.put("Rate LPF", 0.0);
        params.put("Field SP", "");
        params.put("Field Rate SP", "");
        return params;
    }

    @Override
    public void init() {
        pos = 0.0;
        rate = 0.0;
        posSP = 0.0;
        rateSP = 0.0;
        timePrev = -1.0;
        timeStep = (Double) parameters.get("Time Step");
        thrustK = (Double) parameters.get("Thrust K");
        accScale = (Double) parameters.get("Acc Scale");
        drag = (Double) parameters.get("Drag");
        useRateSP = (Boolean) parameters.get("Use Rate SP");
        delayLine.reset();
        delayLine.setDelay((Double) parameters.get("Thrust Delay"));
        controlLPF.reset();
        controlLPF.setT((Double) parameters.get("Thrust T"));
        pidPos.reset();
        pidPos.setK((Double) parameters.get("Ctrl P"), 0.0, (Double) parameters.get("Ctrl D"),
                (Double) parameters.get("Ctrl Limit"), PID.MODE.DERIVATIVE_SET);
        pidRate.reset();
        PID.MODE pidRateMode = (Boolean) parameters.get(
                "Ctrl Rate D SP") ? PID.MODE.DERIVATIVE_CALC : PID.MODE.DERIVATIVE_CALC_NO_SP;
        pidRate.setK((Double) parameters.get("Ctrl Rate P"), (Double) parameters.get("Ctrl Rate I"),
                (Double) parameters.get("Ctrl Rate D"), (Double) parameters.get("Ctrl Rate Limit"), pidRateMode);
        rateLPF.setCutoffFreqFactor(((Double) parameters.get("Rate LPF")) * timeStep);
        spField = (String) parameters.get("Field SP");
        rateSpField = (String) parameters.get("Field Rate SP");
        addSeries("Rate");
        addSeries("Ctrl");
        addSeries("Acc");
        if (useRateSP) {
            addSeries("Rate SP");
        } else {
            addSeries("Pos SP");
            addSeries("Pos");
        }
    }

    @Override
    public void process(double time, Map<String, Object> update) {
        if (timePrev < 0.0) {
            timePrev = time;
            return;
        }
        while (time > timePrev + timeStep) {
            timePrev += timeStep;
            updateSimulation(timePrev, timeStep);
        }
        updateSP(update);
    }

    private void updateSP(Map<String, Object> update) {
        if (useRateSP) {
            Number v = (Number) update.get(rateSpField);
            if (v != null) {
                rateSP = v.doubleValue();
            }
        } else {
            String[] p = spField.split(" ");
            if (p.length > 1) {
                int axis = "RPY".indexOf(p[0]);
                if (axis >= 0 && axis < 3) {
                    double[] q = new double[4];
                    for (int i = 0; i < 4; i++) {
                        Number v = (Number) update.get(p[i + 1]);
                        if (v == null) {
                            return;
                        }
                        q[i] = v.doubleValue();
                    }
                    double[] euler = RotationConversion.eulerAnglesByQuaternion(q);
                    posSP = euler[axis];
                }
            } else {
                Number v = (Number) update.get(spField);
                if (v != null) {
                    posSP = v.doubleValue();
                }
            }
        }
    }

    private void updateSimulation(double time, double dt) {
        Double force = delayLine.getOutput(time, controlLPF.getOutput(time, 0.0));
        if (force == null) {
            force = 0.0;
        }
        double acc = force * thrustK - drag * Math.abs(rate) * rate;
        rate += acc * dt;
        pos += rate * dt;
        double rateFiltered = rateLPF.apply(rate);
        if (!useRateSP) {
            rateSP = pidPos.getOutput(posSP - pos, - rateFiltered, dt);
        }
        double control = pidRate.getOutput(rateSP, rateFiltered, 0.0, dt, 1.0);
        controlLPF.setInput(control);

        addPoint(0, time, rate);
        addPoint(1, time, control);
        if (accScale != 0.0) {
            addPoint(2, time, acc * accScale);
        }
        if (useRateSP) {
            addPoint(3, time, rateSP);
        } else {
            addPoint(3, time, posSP);
            addPoint(4, time, pos);
        }
    }
}
