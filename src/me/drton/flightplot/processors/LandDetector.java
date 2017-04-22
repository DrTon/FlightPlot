package me.drton.flightplot.processors;

import me.drton.flightplot.processors.tools.LowPassFilter;

import java.util.HashMap;
import java.util.Map;

/**
 * User: ton Date: 25.07.13 Time: 14:20
 */
public class LandDetector extends PlotProcessor {
    private String param_Field_Baro;
    private String param_Field_Thrust;
    private String[] param_Fields_Acc;
    private double param_Threshold_Alt;
    private double param_Threshold_Thrust;
    private double param_Threshold_Acc;
    private double param_Threshold_Time;
    private double timePrev;
    private double thrust;
    private double[] acc;
    private LowPassFilter baroFilt1;
    private LowPassFilter baroFilt2;
    private double baroFilt1Out = 0.0;
    private double baroFilt2Out = 0.0;
    private LowPassFilter[] accFilt1;
    private LowPassFilter[] accFilt2;
    private boolean landed;
    private boolean initialized;
    private double lastStill = 0.0;

    @Override
    public Map<String, Object> getDefaultParameters() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("Field Baro", "SENS.BaroAlt");
        params.put("Field Thrust", "ATTC.Thrust");
        params.put("Fields Acc", "IMU.AccX IMU.AccY IMU.AccZ");
        params.put("Baro Filt1", 10.0);
        params.put("Baro Filt2", 1.0);
        params.put("Acc Filter1", 10.0);
        params.put("Acc Filter2", 1.0);
        params.put("Threshold Alt", 0.3);
        params.put("Threshold Thrust", 0.25);
        params.put("Threshold Acc", 1.0);
        params.put("Threshold Time", 1.0);
        return params;
    }

    @Override
    public void init() {
        timePrev = Double.NaN;
        landed = true;
        initialized = false;
        lastStill = 0.0;
        baroFilt1 = new LowPassFilter();
        baroFilt2 = new LowPassFilter();
        thrust = 0.0;
        acc = new double[]{0.0, 0.0, -9.81};
        accFilt1 = new LowPassFilter[]{new LowPassFilter(), new LowPassFilter(), new LowPassFilter()};
        accFilt2 = new LowPassFilter[]{new LowPassFilter(), new LowPassFilter(), new LowPassFilter()};
        param_Field_Baro = (String) parameters.get("Field Baro");
        param_Field_Thrust = (String) parameters.get("Field Thrust");
        param_Fields_Acc = ((String) parameters.get("Fields Acc")).split(WHITESPACE_RE);
        baroFilt1.setF((Double) parameters.get("Baro Filt1"));
        baroFilt2.setF((Double) parameters.get("Baro Filt2"));
        for (int i = 0; i < 3; i++) {
            accFilt1[i].setF((Double) parameters.get("Acc Filter1"));
            accFilt2[i].setF((Double) parameters.get("Acc Filter2"));
        }
        param_Threshold_Alt = (Double) parameters.get("Threshold Alt");
        param_Threshold_Thrust = (Double) parameters.get("Threshold Thrust");
        param_Threshold_Acc = (Double) parameters.get("Threshold Acc");
        param_Threshold_Time = (Double) parameters.get("Threshold Time");
        addSeries("Landed");
        addSeries("AltDisp");
        addSeries("AccAvg");
        addSeries("AccDisp");
    }

    @Override
    public void process(double time, Map<String, Object> update) {
        Number baroNum = (Number) update.get(param_Field_Baro);
        if (baroNum != null) {
            initialized = true;
            baroFilt1Out = baroFilt1.getOutput(time, baroNum.doubleValue());
            baroFilt2Out = baroFilt2.getOutput(time, baroFilt1Out);
        }
        Number thrustNum = (Number) update.get(param_Field_Thrust);
        if (thrustNum != null) {
            thrust = thrustNum.doubleValue();
        }
        int accIdx = 0;
        for (String field : param_Fields_Acc) {
            Number accNum = (Number) update.get(field);
            if (accNum != null) {
                acc[accIdx] = accNum.doubleValue();
            }
            ++accIdx;
        }
        if (initialized && !Double.isNaN(timePrev)) {
            double altDiff = baroFilt1Out - baroFilt2Out;
            double altDisp = Math.abs(altDiff);
            addPoint(1, time, altDisp);

            double accAvgAbs = 0.0;
            double accDisp2 = 0.0;
            for (int i = 0; i < 3; ++i) {
                double filt1Out = accFilt1[i].getOutput(time, acc[i]);
                double filt2Out = accFilt2[i].getOutput(time, filt1Out);
                double accDiff = filt1Out - filt2Out;
                accAvgAbs += filt1Out * filt1Out;
                accDisp2 += accDiff * accDiff;
            }

            accAvgAbs = Math.abs(Math.sqrt(accAvgAbs) - 9.81);
            double accDisp = Math.sqrt(accDisp2);
            addPoint(2, time, accAvgAbs);
            addPoint(3, time, accDisp);

            if (altDisp < param_Threshold_Alt && thrust < param_Threshold_Thrust && accAvgAbs < param_Threshold_Acc && accDisp < param_Threshold_Acc) {
                if (lastStill == 0.0) {
                    lastStill = time;
                } else if (time > lastStill + param_Threshold_Time) {
                    landed = true;
                }
            } else {
                lastStill = 0.0;
                landed = false;
            }
        }
        addPoint(0, time, landed ? 1.0 : 0.0);
        timePrev = time;
    }
}
