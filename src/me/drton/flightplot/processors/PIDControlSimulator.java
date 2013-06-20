package me.drton.flightplot.processors;

import me.drton.flightplot.processors.tools.LowPassFilter;
import me.drton.flightplot.processors.tools.PID;
import org.jfree.data.xy.XYSeriesCollection;

import java.util.HashMap;
import java.util.Map;

/**
 * User: ton Date: 20.06.13 Time: 6:06
 */
public class PIDControlSimulator extends PlotProcessor {
    private double startTime;
    private double startSP;
    private double thrustK;
    private double attAccScale;

    private boolean started;
    private LowPassFilter propeller = new LowPassFilter();
    private PID pidPos = new PID();
    private PID pidRate = new PID();
    private double att;
    private double attRate;
    private double attSP;
    private double timePrev;
    private XYSeriesCollection seriesCollection;

    @Override
    public Map<String, Object> getDefaultParameters() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("Start Time", 100.0);
        params.put("Start SP", 1.0);
        params.put("Thrust T", 0.03);
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
        started = false;
        propeller.reset();
        att = 0.0;
        attRate = 0.0;
        attSP = 0.0;
        timePrev = Double.NaN;
        startTime = (Double) parameters.get("Start Time");
        startSP = (Double) parameters.get("Start SP");
        thrustK = (Double) parameters.get("Thrust K");
        attAccScale = (Double) parameters.get("Att Acc Scale");
        propeller.setT((Double) parameters.get("Thrust T"));
        pidPos.reset();
        pidRate.reset();
        pidPos.setK((Double) parameters.get("Ctrl P"), 0.0, (Double) parameters.get("Ctrl D"));
        pidRate.setK((Double) parameters.get("Ctrl Rate P"), 0.0, (Double) parameters.get("Ctrl Rate D"));
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
                    attSP = startSP;
                }
                double dt = time - timePrev;
                double force = propeller.getOutput(time, 0.0);
                double attAcc = force * thrustK;
                attRate += attAcc * dt;
                att += attRate * dt;
                double rateSP = pidPos.getOutput(attSP - att, -attRate, true, dt);
                double thrustControl = pidRate.getOutputDNoSP(rateSP, attRate, dt);
                propeller.setInput(thrustControl);
                seriesCollection.getSeries(0).add(time, att);
                seriesCollection.getSeries(1).add(time, attRate);
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
