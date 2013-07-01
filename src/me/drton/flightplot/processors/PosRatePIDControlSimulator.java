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
    private double thrustK;
    private double attAccScale;

    private boolean started;
    private DelayLine propellerDelay = new DelayLine();
    private LowPassFilter propeller = new LowPassFilter();
    private PID pidPos = new PID();
    private PID pidRate = new PID();
    private double pos;
    private double rate;
    private double posSP;
    private double timePrev;
    private XYSeriesCollection seriesCollection;

    @Override
    public Map<String, Object> getDefaultParameters() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("Start Time", 100.0);
        params.put("Start SP", 1.0);
        params.put("Thrust T", 0.03);
        params.put("Thrust Delay", 0.05);
        params.put("Thrust K", 1000.0);
        params.put("Ctrl P", 5.0);
        params.put("Ctrl D", 0.0);
        params.put("Ctrl Rate P", 0.2);
        params.put("Ctrl Rate D", 0.01);
        params.put("Att Acc Scale", 1.0);
        return params;
    }

    @Override
    public void init() {
        pos = 0.0;
        rate = 0.0;
        posSP = 0.0;
        timePrev = Double.NaN;
        started = false;
        startTime = (Double) parameters.get("Start Time");
        startSP = (Double) parameters.get("Start SP");
        thrustK = (Double) parameters.get("Thrust K");
        attAccScale = (Double) parameters.get("Att Acc Scale");
        propellerDelay.reset();
        propellerDelay.setDelay((Double) parameters.get("Thrust Delay"));
        propeller.reset();
        propeller.setT((Double) parameters.get("Thrust T"));
        pidPos.reset();
        pidPos.setK((Double) parameters.get("Ctrl P"), 0.0, (Double) parameters.get("Ctrl D"), 0.0);
        pidRate.reset();
        pidRate.setK((Double) parameters.get("Ctrl Rate P"), 0.0, (Double) parameters.get("Ctrl Rate D"), 0.0);
        seriesCollection = new XYSeriesCollection();
        seriesCollection.addSeries(createSeries("Pos"));
        seriesCollection.addSeries(createSeries("Rate"));
        seriesCollection.addSeries(createSeries("Acc"));
        seriesCollection.addSeries(createSeries("Ctrl"));
    }

    @Override
    public void process(double time, Map<String, Object> update) {
        if (update.containsKey("ATT.Roll")) {   // Act only on attitude updates
            if (!Double.isNaN(timePrev)) {
                if (!started && time > startTime) {
                    started = true;
                    posSP = startSP;
                }
                double dt = time - timePrev;
                double force = propellerDelay.getOutput(time, propeller.getOutput(time, 0.0));
                if (Double.isNaN(force))
                    force = 0.0;
                double attAcc = force * thrustK;
                rate += attAcc * dt;
                pos += rate * dt;
                double rateSP = pidPos.getOutput(posSP - pos, -rate, true, dt);
                double thrustControl = pidRate.getOutputDNoSP(rateSP, rate, dt);
                propeller.setInput(thrustControl);
                seriesCollection.getSeries(0).add(time, pos);
                seriesCollection.getSeries(1).add(time, rate);
                if (attAccScale != 0.0)
                    seriesCollection.getSeries(2).add(time, attAcc * attAccScale);
                seriesCollection.getSeries(3).add(time, thrustControl);
            }
            timePrev = time;
        }
    }

    @Override
    public XYSeriesCollection getSeriesCollection() {
        return seriesCollection;
    }
}
