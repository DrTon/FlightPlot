package me.drton.flightplot.processors;

import me.drton.flightplot.processors.tools.RotationConversion;
import org.ejml.simple.SimpleMatrix;

import java.util.HashMap;
import java.util.Map;

/**
 * User: ton Date: 24.06.13 Time: 13:10
 */
public class AltitudeEstimator extends PlotProcessor {
    private String param_Field_Baro;
    private String[] param_Fields_Acc;
    private String[] param_Fields_Att;
    private String param_Field_Sonar;
    private double param_Weight_Acc;
    private double param_Weight_Baro;
    private double param_Weight_Sonar;
    private double param_Weight_Acc_Bias;
    private double param_Baro_Offset;
    private double baroOffset;
    private double timePrev;
    private double[] x;   // Pos, Vel, Acc
    private double corrBaro;
    private double corrAcc;
    private double corrSonar;
    private SimpleMatrix acc = new SimpleMatrix(3, 1);
    private SimpleMatrix rot;
    private double sonarPrev;
    private double sonarTime;
    private boolean sonarValid;
    private double[] accBias = new double[]{0.0, 0.0, 0.0};
    private static final double G = 9.81;

    @Override
    public Map<String, Object> getDefaultParameters() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("Field Baro", "SENS.BaroAlt");
        params.put("Fields Acc", "IMU.AccX IMU.AccY IMU.AccZ");
        params.put("Fields Att", "ATT.Roll ATT.Pitch");
        params.put("Field Sonar", "FLOW.Dist");
        params.put("Weight Baro", 1.0);
        params.put("Weight Acc", 20.0);
        params.put("Weight Sonar", 3.0);
        params.put("Weight Acc Bias", 0.05);
        params.put("Baro Offset", 0.0);
        return params;
    }

    @Override
    public void init() {
        super.init();
        timePrev = Double.NaN;
        x = new double[]{0.0, 0.0, 0.0};
        accBias = new double[]{0.0, 0.0, 0.0};
        corrBaro = 0.0;
        corrAcc = 0.0;
        corrSonar = 0.0;
        sonarPrev = 0.0;
        sonarTime = 0.0;
        sonarValid = false;
        param_Field_Baro = (String) parameters.get("Field Baro");
        param_Fields_Acc = ((String) parameters.get("Fields Acc")).split(WHITESPACE_RE);
        param_Fields_Att = ((String) parameters.get("Fields Att")).split(WHITESPACE_RE);
        param_Field_Sonar = (String) parameters.get("Field Sonar");
        param_Weight_Baro = (Double) parameters.get("Weight Baro");
        param_Weight_Acc = (Double) parameters.get("Weight Acc");
        param_Weight_Sonar = (Double) parameters.get("Weight Sonar");
        param_Weight_Acc_Bias = (Double) parameters.get("Weight Acc Bias");
        param_Baro_Offset = (Double) parameters.get("Baro Offset");
        baroOffset = -param_Baro_Offset;
        addSeries("Alt");
        addSeries("AltV");
        addSeries("AccBiasZ");
        addSeries("BaroOffset");
    }

    @Override
    public void process(double time, Map<String, Object> update) {
        boolean act = false;
        Number baroNum = (Number) update.get(param_Field_Baro);
        if (baroNum != null) {
            double baro = baroNum.doubleValue();
            corrBaro = baro - x[0];
            act = true;
        }
        Number accX = (Number) update.get(param_Fields_Acc[0]);
        Number accY = (Number) update.get(param_Fields_Acc[1]);
        Number accZ = (Number) update.get(param_Fields_Acc[2]);
        if (accX != null && accY != null && accZ != null) {
            acc.set(0, 0, accX.doubleValue() - accBias[0]);
            acc.set(1, 0, accY.doubleValue() - accBias[1]);
            acc.set(2, 0, accZ.doubleValue() - accBias[2]);
            act = true;
        }
        Number roll = (Number) update.get(param_Fields_Att[0]);
        Number pitch = (Number) update.get(param_Fields_Att[1]);
        if (roll != null && pitch != null) {
            rot = RotationConversion.rotationMatrixByEulerAngles(roll.doubleValue(), pitch.doubleValue(), 0.0);
            act = true;
        }
        Number sonarNum = (Number) update.get(param_Field_Sonar);
        if (sonarNum != null) {
            double sonar = sonarNum.doubleValue();
            if (sonar > 0.31 && sonar < 4.0 && (sonar != sonarPrev || time - sonarTime < 0.15)) {
                if (sonar != sonarPrev) {
                    sonarTime = time;
                    sonarPrev = sonar;
                    corrSonar = sonar - x[0];
                }
                sonarValid = true;
                act = true;
            } else {
                corrSonar = 0.0;
                sonarValid = false;
            }
        }
        if (act) {
            SimpleMatrix accNED = rot.mult(acc);
            if (!Double.isNaN(timePrev)) {
                double dt = time - timePrev;
                corrAcc = -accNED.get(2) - G - x[2];
                // Baro offset correction
                if (sonarValid) {
                    baroOffset += (corrBaro - baroOffset) * param_Weight_Sonar * dt;
                }
                // Accelerometer bias correction
                double accBiasCorr = (corrBaro - baroOffset) * param_Weight_Baro * param_Weight_Baro;
                if (sonarValid) {
                    accBiasCorr += corrSonar * param_Weight_Sonar * param_Weight_Sonar;
                }
                SimpleMatrix accBiasCorrV = new SimpleMatrix(3, 1);
                accBiasCorrV.set(0, 0.0);
                accBiasCorrV.set(1, 0.0);
                accBiasCorrV.set(2, accBiasCorr);
                SimpleMatrix b = rot.transpose().mult(accBiasCorrV).scale(param_Weight_Acc_Bias * dt);
                accBias[0] += b.get(0);
                accBias[1] += b.get(1);
                accBias[2] += b.get(2);
                // Inertial filter prediction
                predict(dt);
                // Inertial filter correction
                if (sonarValid) {
                    correct(dt, 0, corrSonar, param_Weight_Sonar);
                }
                correct(dt, 0, corrBaro - baroOffset, param_Weight_Baro);
                correct(dt, 2, corrAcc, param_Weight_Acc);
                addPoint(0, time, x[0]);
                addPoint(1, time, x[1]);
                addPoint(2, time, accBias[2]);
                addPoint(3, time, baroOffset + param_Baro_Offset);
            }
            timePrev = time;
        }
    }

    private void predict(double dt) {
        x[0] += x[1] * dt + x[2] * dt * dt / 2.0;
        x[1] += x[2] * dt;
    }

    private void correct(double dt, int i, double e, double w) {
        double ewdt = w * e * dt;
        x[i] += ewdt;
        if (i == 0) {
            x[1] += w * ewdt;
            x[2] += w * w * ewdt / 3.0;
        } else if (i == 1) {
            x[2] += w * ewdt;
        }
    }
}
