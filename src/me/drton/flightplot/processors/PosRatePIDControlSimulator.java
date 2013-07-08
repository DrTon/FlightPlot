package me.drton.flightplot.processors;

import me.drton.flightplot.processors.tools.DelayLine;
import me.drton.flightplot.processors.tools.LowPassFilter;
import me.drton.flightplot.processors.tools.PID;
import org.jfree.data.xy.XYSeriesCollection;

import java.util.HashMap;
import java.util.Map;

/**
 * User: ton Date: 20.06.13 Time: 6:06
 */
public class PosRatePIDControlSimulator extends PlotProcessor {
    private double startTime;
    private double startSP;
    private double startSPRate;
    private double thrustK;
    private double accScale;
    private double drag;
    private DelayLine delayLine = new DelayLine();
    private LowPassFilter lpf = new LowPassFilter();
    private PID pidPos = new PID();
    private PID pidRate = new PID();
    private double pos;
    private double rate;
    private double posSP;
    private boolean useRateSP;
    private double timePrev;
    private XYSeriesCollection seriesCollection;

    @Override
    public Map<String, Object> getDefaultParameters() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("Start Time", 100.0);
        params.put("Start SP", 1.0);
        params.put("Start SP Rate", 1.0);
        params.put("Thrust T", 0.03);
        params.put("Thrust Delay", 0.05);
        params.put("Thrust K", 1000.0);
        params.put("Ctrl P", 5.0);
        params.put("Ctrl D", 0.0);
        params.put("Ctrl Limit", 0.0);
        params.put("Ctrl Rate P", 0.2);
        params.put("Ctrl Rate I", 0.0);
        params.put("Ctrl Rate D", 0.01);
        params.put("Ctrl Rate D SP", true);
        params.put("Ctrl Rate Limit", 0.0);
        params.put("Att Acc Scale", 1.0);
        params.put("Drag", 0.0);
        params.put("Use Rate SP", false);
        return params;
    }

    @Override
    public void init() {
        pos = 0.0;
        rate = 0.0;
        posSP = 0.0;
        timePrev = Double.NaN;
        startTime = (Double) parameters.get("Start Time");
        startSP = (Double) parameters.get("Start SP");
        startSPRate = (Double) parameters.get("Start SP Rate");
        thrustK = (Double) parameters.get("Thrust K");
        accScale = (Double) parameters.get("Att Acc Scale");
        drag = (Double) parameters.get("Drag");
        useRateSP = (Boolean) parameters.get("Use Rate SP");
        delayLine.reset();
        delayLine.setDelay((Double) parameters.get("Thrust Delay"));
        lpf.reset();
        lpf.setT((Double) parameters.get("Thrust T"));
        pidPos.reset();
        pidPos.setK((Double) parameters.get("Ctrl P"), 0.0, (Double) parameters.get("Ctrl D"),
                (Double) parameters.get("Ctrl Limit"), PID.MODE.DERIVATIVE_SET);
        pidRate.reset();
        PID.MODE pidRateMode = (Boolean) parameters.get(
                "Ctrl Rate D SP") ? PID.MODE.DERIVATIVE_CALC : PID.MODE.DERIVATIVE_CALC_NO_SP;
        pidRate.setK((Double) parameters.get("Ctrl Rate P"), (Double) parameters.get("Ctrl Rate I"), (Double) parameters.get("Ctrl Rate D"),
                (Double) parameters.get("Ctrl Rate Limit"), pidRateMode);
        seriesCollection = new XYSeriesCollection();
        seriesCollection.addSeries(createSeries("Pos"));
        seriesCollection.addSeries(createSeries("Rate"));
        seriesCollection.addSeries(createSeries("Acc"));
        seriesCollection.addSeries(createSeries("Ctrl"));
        seriesCollection.addSeries(createSeries("Pos SP"));
    }

    @Override
    public void process(double time, Map<String, Object> update) {
        if (update.containsKey("ATT.Roll")) {   // Act only on attitude updates
            if (!Double.isNaN(timePrev)) {
                double dt = time - timePrev;
                double force = delayLine.getOutput(time, lpf.getOutput(time, 0.0));
                if (Double.isNaN(force))
                    force = 0.0;
                double acc = force * thrustK - drag * rate;
                rate += acc * dt;
                pos += rate * dt;
                double posSPRate = 0.0;
                if (time > startTime && posSP < startSP) {
                    posSPRate = startSPRate;
                    posSP += startSPRate * dt;
                }
                double rateSP;
                if (useRateSP) {
                    rateSP = posSP;
                } else {
                    rateSP = pidPos.getOutput(posSP - pos, posSPRate - rate, dt);
                }
                double control = pidRate.getOutput(rateSP, rate, 0.0, dt, 1.0);
                lpf.setInput(control);
                seriesCollection.getSeries(0).add(time, pos);
                seriesCollection.getSeries(1).add(time, rate);
                if (accScale != 0.0)
                    seriesCollection.getSeries(2).add(time, acc * accScale);
                seriesCollection.getSeries(3).add(time, control);
                seriesCollection.getSeries(4).add(time, posSP);
            }
            timePrev = time;
        }
    }

    @Override
    public XYSeriesCollection getSeriesCollection() {
        return seriesCollection;
    }
}
