package me.drton.flightplot.processors;

import me.drton.flightplot.processors.tools.DispersionTracker;
import me.drton.flightplot.processors.tools.LowPassFilter;

import java.util.HashMap;
import java.util.Map;

/**
 * User: ton Date: 25.07.13 Time: 14:20
 */
public class LandDetector extends PlotProcessor {
    private String param_Field_Baro;
    private String param_Field_Thrust;
    private String[] param_Fields_Gyro;
    private double param_Thrust;
    private double param_Baro_Disp_Max;
    private double param_Gyro_Disp_Max;
    private DispersionTracker dispersionTrackerBaro;
    private DispersionTracker[] dispersionTrackerGyro;
    private double thrust;
    private double dispBaro;
    private double[] dispGyro;

    @Override
    public Map<String, Object> getDefaultParameters() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("Field Baro", "SENS.BaroAlt");
        params.put("Field Thrust", "ATTC.Thrust");
        params.put("Fields Gyro", "IMU.GyrX IMU.GyrY IMU.GyrZ");
        params.put("Baro Disp", 0.2);
        params.put("Baro LPF", 10.0);
        params.put("Baro HPF", 0.5);
        params.put("Gyro Disp", 0.02);
        params.put("Gyro LPF", 10.0);
        params.put("Gyro HPF", 1.0);
        params.put("Thrust", 0.2);
        return params;
    }

    @Override
    public void init() {
        thrust = 0;
        dispBaro = 0;
        dispGyro = new double[]{0, 0, 0};
        param_Field_Baro = (String) parameters.get("Field Baro");
        param_Field_Thrust = (String) parameters.get("Field Thrust");
        param_Fields_Gyro = ((String) parameters.get("Fields Gyro")).split(WHITESPACE_RE);
        param_Thrust = (Double) parameters.get("Thrust");
        param_Baro_Disp_Max = (Double) parameters.get("Baro Disp");
        param_Baro_Disp_Max *= param_Baro_Disp_Max;
        param_Gyro_Disp_Max = (Double) parameters.get("Gyro Disp");
        param_Gyro_Disp_Max *= param_Gyro_Disp_Max;
        dispersionTrackerBaro = new DispersionTracker();
        dispersionTrackerBaro.setCutoffFreq((Double) parameters.get("Baro HPF"), (Double) parameters.get("Baro LPF"));
        dispersionTrackerGyro = new DispersionTracker[3];
        for (int i = 0; i < 3; ++i) {
            DispersionTracker tracker = new DispersionTracker();
            tracker.setCutoffFreq((Double) parameters.get("Gyro HPF"), (Double) parameters.get("Gyro LPF"));
            dispersionTrackerGyro[i] = tracker;
        }
        addSeries("DispBaro");
        addSeries("DispGyro");
        addSeries("Thrust");
        addSeries("Landed");
    }

    @Override
    public void process(double time, Map<String, Object> update) {
        Number baroNum = (Number) update.get(param_Field_Baro);
        if (baroNum != null) {
            dispBaro = dispersionTrackerBaro.getOutput(time, baroNum.doubleValue());
            addPoint(0, time, Math.sqrt(dispBaro));
        }

        for (int i = 0; i < 3; ++i) {
            Number accNum = (Number) update.get(param_Fields_Gyro[i]);
            if (accNum != null) {
                dispGyro[i] = dispersionTrackerGyro[i].getOutput(time, accNum.doubleValue());
            }
        }
        double dispGyroSum = dispGyro[0] + dispGyro[1] + dispGyro[2];
        addPoint(1, time, Math.sqrt(dispGyroSum));

        Number thrustNum = (Number) update.get(param_Field_Thrust);
        if (thrustNum != null) {
            thrust = thrustNum.doubleValue();
            addPoint(2, time, thrust);
        }

        boolean landed = thrust < param_Thrust && dispBaro < param_Baro_Disp_Max && dispGyroSum < param_Gyro_Disp_Max;
        addPoint(3, time, landed ? 1 : 0);
    }
}
