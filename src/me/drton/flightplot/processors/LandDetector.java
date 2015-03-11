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
    private double param_Filter_Time;
    private double param_Threshold_Alt2;
    private double param_Threshold_Thrust;
    private double timePrev;
    private double baro;
    private double thrust;
    private LowPassFilter baroLPF;
    private double landDetectedTime;
    private boolean landed;
    private boolean initialized;
    private double altAvg;
    private double altDisp;

    @Override
    public Map<String, Object> getDefaultParameters() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("Field Baro", "SENS.BaroAlt");
        params.put("Field Thrust", "ATTC.Thrust");
        params.put("Baro LPF", 20.0);
        params.put("Filter Time", 1.0);
        params.put("Threshold Alt", 0.3);
        params.put("Threshold Thrust", 0.25);
        return params;
    }

    @Override
    public void init() {
        timePrev = Double.NaN;
        landed = true;
        landDetectedTime = Double.NaN;
        initialized = false;
        altAvg = 0.0;
        altDisp = 0.0;
        baro = 0.0;
        baroLPF = new LowPassFilter();
        thrust = 0.0;
        param_Field_Baro = (String) parameters.get("Field Baro");
        param_Field_Thrust = (String) parameters.get("Field Thrust");
        param_Filter_Time = (Double) parameters.get("Filter Time");
        baroLPF.setF((Double) parameters.get("Baro LPF"));
        param_Threshold_Alt2 = (Double) parameters.get("Threshold Alt");
        param_Threshold_Alt2 = param_Threshold_Alt2 * param_Threshold_Alt2;
        param_Threshold_Thrust = (Double) parameters.get("Threshold Thrust");
        addSeries("Landed");
        addSeries("AltDisp");
    }

    @Override
    public void process(double time, Map<String, Object> update) {
        Number baroNum = (Number) update.get(param_Field_Baro);
        if (baroNum != null) {
            baro = baroLPF.getOutput(time, baroNum.doubleValue());
            if (!initialized) {
                initialized = true;
                altAvg = baro;
            }
        }
        Number thrustNum = (Number) update.get(param_Field_Thrust);
        if (thrustNum != null) {
            thrust = thrustNum.doubleValue();
        }
        if (initialized && !Double.isNaN(timePrev)) {
            double dt = time - timePrev;
            altAvg += (baro - altAvg) * dt / param_Filter_Time;
            altDisp = baro - altAvg;
            altDisp = altDisp * altDisp;
            if (landed) {
                if (altDisp > param_Threshold_Alt2 && thrust > param_Threshold_Thrust) {
                    landed = false;
                    landDetectedTime = Double.NaN;
                }
            } else {
                if (altDisp < param_Threshold_Alt2 && thrust < param_Threshold_Thrust) {
                    if (Double.isNaN(landDetectedTime)) {
                        landDetectedTime = time;    // land detected first time
                    } else {
                        if (time - landDetectedTime > param_Filter_Time) {
                            landed = true;
                            landDetectedTime = Double.NaN;
                        }
                    }
                } else {
                    landDetectedTime = Double.NaN;
                }
            }
        }
        addPoint(0, time, landed ? 1.0 : 0.0);
        addPoint(1, time, Math.sqrt(altDisp));
        timePrev = time;
    }
}
