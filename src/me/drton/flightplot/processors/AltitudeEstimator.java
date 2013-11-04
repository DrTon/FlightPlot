package me.drton.flightplot.processors;

import me.drton.flightplot.processors.tools.RotationConversion;
import org.ejml.simple.SimpleMatrix;

import java.util.HashMap;
import java.util.Map;

/**
 * User: ton Date: 24.06.13 Time: 13:10
 */
public class AltitudeEstimator extends PlotProcessor {
    private String[] param_Fields_GPS;
    private String param_Field_Baro;
    private String[] param_Fields_Acc;
    private String[] param_Fields_Att;
    private double param_Weight_GPS_Pos;
    private double param_Weight_GPS_Vel;
    private double param_Weight_Baro;
    private double param_Weight_Acc;
    private double param_Weight_Acc_Bias;
    private double param_Offset;
    private double baroOffset;
    private double timePrev;
    private double[] x;   // Pos, Vel, Acc
    private boolean gpsValid;
    private double altGPS;
    private double altBaro;
    private double corrGPSPos;
    private double corrGPSVel;
    private double corrBaro;
    private double corrAcc;
    private double wGPS;
    private SimpleMatrix acc = new SimpleMatrix(3, 1);
    private SimpleMatrix rot;
    private double[] accBias = new double[]{0.0, 0.0, 0.0};
    private boolean inited = false;
    private static final double G = 9.81;

    @Override
    public Map<String, Object> getDefaultParameters() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("Fields GPS", "GPS.Alt GPS.VelD GPS.EPV");
        params.put("Field Baro", "SENS.BaroAlt");
        params.put("Fields Acc", "IMU.AccX IMU.AccY IMU.AccZ");
        params.put("Fields Att", "ATT.Roll ATT.Pitch");
        params.put("Weight GPS Pos", 0.5);
        params.put("Weight GPS Vel", 1.0);
        params.put("Weight Baro", 1.0);
        params.put("Weight Acc", 20.0);
        params.put("Weight Acc Bias", 0.05);
        params.put("Offset", 0.0);
        return params;
    }

    @Override
    public void init() {
        super.init();
        timePrev = Double.NaN;
        x = new double[]{0.0, 0.0, 0.0};
        accBias = new double[]{0.0, 0.0, 0.0};
        gpsValid = false;
        altGPS = Double.NaN;
        altBaro = Double.NaN;
        baroOffset = 0.0;
        corrGPSPos = 0.0;
        corrBaro = 0.0;
        corrAcc = 0.0;
        wGPS = 0.0;
        inited = false;
        param_Fields_GPS = ((String) parameters.get("Fields GPS")).split(WHITESPACE_RE);
        param_Field_Baro = (String) parameters.get("Field Baro");
        param_Fields_Acc = ((String) parameters.get("Fields Acc")).split(WHITESPACE_RE);
        param_Fields_Att = ((String) parameters.get("Fields Att")).split(WHITESPACE_RE);
        param_Weight_GPS_Pos = (Double) parameters.get("Weight GPS Pos");
        param_Weight_GPS_Vel = (Double) parameters.get("Weight GPS Vel");
        param_Weight_Baro = (Double) parameters.get("Weight Baro");
        param_Weight_Acc = (Double) parameters.get("Weight Acc");
        param_Weight_Acc_Bias = (Double) parameters.get("Weight Acc Bias");
        param_Offset = (Double) parameters.get("Offset");
        addSeries("Alt");
        addSeries("AltV");
        addSeries("AccBiasZ");
        if (param_Weight_GPS_Pos != 0.0)
            addSeries("BaroOffset");
    }

    @Override
    public void process(double time, Map<String, Object> update) {
        boolean act = false;
        Number gpsPosNum = (Number) update.get(param_Fields_GPS[0]);
        Number gpsVelNum = (Number) update.get(param_Fields_GPS[1]);
        Number gpsEPVNum = (Number) update.get(param_Fields_GPS[2]);
        if (gpsVelNum != null && gpsVelNum != null && gpsEPVNum != null) {
            double epv = gpsEPVNum.doubleValue();
            gpsValid = epv < 10.0;
            altGPS = gpsPosNum.doubleValue();
            corrGPSPos = altGPS - x[0];
            corrGPSVel = -gpsVelNum.doubleValue() - x[1];
            wGPS = Math.min(1.0, 1.0 / gpsEPVNum.doubleValue());
            act = true;
        }
        Number baroNum = (Number) update.get(param_Field_Baro);
        if (baroNum != null) {
            altBaro = baroNum.doubleValue();
            corrBaro = altBaro - x[0];
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
        if (act) {
            if (!inited) {
                if (param_Weight_GPS_Pos == 0.0) {
                    // no GPS
                    if (!Double.isNaN(altBaro)) {
                        x[0] = altBaro;
                        inited = true;
                    }
                } else if (!Double.isNaN(altBaro) && !Double.isNaN(altGPS)) {
                    // use GPS
                    if (!Double.isNaN(altBaro) && !Double.isNaN(altGPS)) {
                        baroOffset = altBaro - altGPS;
                        x[0] = altGPS;
                        corrGPSPos = 0.0;
                        corrGPSVel = 0.0;
                        inited = true;
                    }
                }
            } else {
                SimpleMatrix accNED = rot.mult(acc);
                if (!Double.isNaN(timePrev)) {
                    double dt = time - timePrev;
                    corrAcc = -accNED.get(2) - G - x[2];
                    // Baro offset correction
                    if (gpsValid)
                        baroOffset -= corrGPSPos * param_Weight_GPS_Pos * wGPS * dt;
                    // Accelerometer bias correction
                    double accBiasCorr = (corrBaro - baroOffset) * param_Weight_Baro * param_Weight_Baro;
                    if (gpsValid) {
                        accBiasCorr += corrGPSPos * param_Weight_GPS_Pos * param_Weight_GPS_Pos * wGPS * wGPS;
                        accBiasCorr += corrGPSVel * param_Weight_GPS_Vel * wGPS;
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
                    if (gpsValid) {
                        correct(dt, 0, corrGPSPos, param_Weight_GPS_Pos * wGPS);
                        correct(dt, 1, corrGPSVel, param_Weight_GPS_Vel * wGPS);
                    }
                    correct(dt, 0, corrBaro - baroOffset, param_Weight_Baro);
                    correct(dt, 2, corrAcc, param_Weight_Acc);
                    addPoint(0, time, x[0] + param_Offset);
                    addPoint(1, time, x[1]);
                    addPoint(2, time, accBias[2]);
                    if (param_Weight_GPS_Pos != 0.0)
                        addPoint(3, time, baroOffset);
                }
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
