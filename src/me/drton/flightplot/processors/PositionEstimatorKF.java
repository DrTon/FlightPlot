package me.drton.flightplot.processors;

import me.drton.flightplot.processors.tools.DelayLine;
import me.drton.flightplot.processors.tools.GlobalPositionProjector;
import me.drton.flightplot.processors.tools.RotationConversion;
import org.ejml.simple.SimpleMatrix;

import java.util.HashMap;
import java.util.Map;

/**
 * User: ton Date: 30.05.14 Time: 23:49
 */
public class PositionEstimatorKF extends PlotProcessor {
    private static final double G = 9.81;
    private String[] param_Fields_GPS;
    private String param_Field_Baro;
    private String[] param_Fields_Acc;
    private String[] param_Fields_Att;
    private double param_Var_Acc;
    private double param_Var_Baro;
    private double param_Var_Baro_Offs;
    private double param_Var_GPS_VH;
    private double param_Var_GPS_VV;

    private double timePrev;

    /*     0      1      2      3      4      5      6
       x:  x      y      z      vx     vy     vz     baro_offs
       z:  gps_x  gps_y  gps_z  gps_vx gps_vy gps_vz baro

       baro_offs = baro + z
     */

    private static final int Z_S_IDX = 2;
    private static final int BARO_OFFS_S_IDX = 6;
    private static final int BARO_O_IDX = 6;

    private SimpleMatrix I;         // unity matrix
    private SimpleMatrix x;         // state
    private SimpleMatrix y;         // innovation
    private SimpleMatrix P;         // covariance
    private SimpleMatrix F;         // transition matrix
    private SimpleMatrix H;         // observation matrix
    private SimpleMatrix z;         // observation
    private SimpleMatrix R;         // covariance of the observation noise

    private double gpsRefAlt;
    private double gpsEPH;
    private double gpsEPV;
    private double gpsLast;
    private double gpsTimeout;
    private SimpleMatrix acc = new SimpleMatrix(3, 1);
    private SimpleMatrix rot;
    private GlobalPositionProjector positionProjector = new GlobalPositionProjector();
    private double[] accBias;
    private DelayLine[][] delayLinesGPS;
    private boolean gpsInited;
    private boolean baroInited;
    private boolean[] show;
    private double[] offsets;
    private double[] scales;

    @Override
    public Map<String, Object> getDefaultParameters() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("Fields GPS", "GPS.Lat GPS.Lon GPS.Alt GPS.VelN GPS.VelE GPS.VelD GPS.EPH GPS.EPV");
        params.put("Field Baro", "SENS.BaroAlt");
        params.put("Fields Acc", "IMU.AccX IMU.AccY IMU.AccZ");
        params.put("Fields Att", "ATT.Roll ATT.Pitch ATT.Yaw");
        params.put("Delay GPS", 0.1);
        params.put("Var Acc", 1.0);
        params.put("Var Baro", 1.0);
        params.put("Var Baro Offs", 0.1);
        params.put("Var GPS VH", 1.0);
        params.put("Var GPS VV", 1.0);
        params.put("Show", "XYZ");
        params.put("Offsets", "0.0 0.0 0.0");
        params.put("Z Downside", false);
        return params;
    }

    @Override
    public void init() {
        super.init();
        timePrev = Double.NaN;
        I = new SimpleMatrix(7, 7);
        x = new SimpleMatrix(7, 1);
        y = new SimpleMatrix(7, 1);
        P = new SimpleMatrix(7, 7);
        F = new SimpleMatrix(7, 7);
        H = new SimpleMatrix(7, 7);
        z = new SimpleMatrix(7, 1);
        R = new SimpleMatrix(7, 7);

        for (int i = 0; i < 7; i++) {
            P.set(i, i, 1.0);
            I.set(i, i, 1.0);
            F.set(i, i, 1.0);
        }

        H.set(BARO_O_IDX, Z_S_IDX, -1);
        H.set(BARO_O_IDX, BARO_OFFS_S_IDX, 1);

        accBias = new double[]{0.0, 0.0, 0.0};
        baroInited = false;
        gpsInited = false;
        gpsRefAlt = 0.0;
        gpsEPH = 1.0;
        gpsEPV = 1.0;
        gpsLast = 0.0;
        gpsTimeout = 0.3;
        delayLinesGPS = new DelayLine[3][2];
        positionProjector.reset();
        param_Fields_GPS = ((String) parameters.get("Fields GPS")).split(WHITESPACE_RE);
        param_Fields_Acc = ((String) parameters.get("Fields Acc")).split(WHITESPACE_RE);
        param_Fields_Att = ((String) parameters.get("Fields Att")).split(WHITESPACE_RE);
        param_Field_Baro = (String) parameters.get("Field Baro");
        param_Var_Acc = (Double) parameters.get("Var Acc");
        param_Var_Baro = (Double) parameters.get("Var Baro");
        param_Var_Baro_Offs = (Double) parameters.get("Var Baro Offs");
        param_Var_GPS_VH = (Double) parameters.get("Var GPS VH");
        param_Var_GPS_VV = (Double) parameters.get("Var GPS VV");

        R.set(0, 0, 1.0);
        R.set(1, 1, 1.0);
        R.set(2, 2, 1.0);
        R.set(3, 3, param_Var_GPS_VH * param_Var_GPS_VH);
        R.set(4, 4, param_Var_GPS_VH * param_Var_GPS_VH);
        R.set(5, 5, param_Var_GPS_VV * param_Var_GPS_VV);

        R.set(6, 6, param_Var_Baro * param_Var_Baro);

        double delayGPS = (Double) parameters.get("Delay GPS");
        for (int axis = 0; axis < 3; axis++) {
            for (int posVel = 0; posVel < 2; posVel++) {
                delayLinesGPS[axis][posVel] = new DelayLine();
                delayLinesGPS[axis][posVel].setDelay(delayGPS);
                delayLinesGPS[axis][posVel].getOutput(0.0, 0.0);
            }
        }
        show = new boolean[]{false, false, false};
        offsets = new double[]{0.0, 0.0, 0.0};
        scales = new double[]{1.0, 1.0, 1.0};
        String showStr = (String) parameters.get("Show");
        String[] offsetsStr = ((String) parameters.get("Offsets")).split(WHITESPACE_RE);
        if (!(Boolean) parameters.get("Z Downside")) {
            scales[2] = -1.0;
        }
        for (int i = 0; i < 3; i++) {
            String axisName = "XYZ".substring(i, i + 1);
            show[i] = showStr.contains(axisName);
            if (show[i]) {
                addSeries(axisName);
                addSeries("V" + axisName);
                addSeries("P" + axisName);
                addSeries("PV" + axisName);
            }
            if (offsetsStr.length > i) {
                offsets[i] = Double.parseDouble(offsetsStr[i]);
            } else {
                offsets[i] = 0.0;
            }
        }
    }

    @Override
    public void process(double time, Map<String, Object> update) {
        boolean act = false;
        // Attitude
        Number roll = (Number) update.get(param_Fields_Att[0]);
        Number pitch = (Number) update.get(param_Fields_Att[1]);
        Number yaw = (Number) update.get(param_Fields_Att[2]);
        if (roll != null && pitch != null && yaw != null) {
            rot = RotationConversion.rotationMatrixByEulerAngles(roll.doubleValue(), pitch.doubleValue(),
                    yaw.doubleValue());
            act = true;
        }

        // Baro
        boolean baroUpdated = false;
        Number baroNum = (Number) update.get(param_Field_Baro);
        if (baroNum != null) {
            double baro = baroNum.doubleValue();
            if (!baroInited) {
                baroInited = true;
                // Set initial baro offset
                x.set(6, baro);
            }
            z.set(BARO_O_IDX, baro);
            baroUpdated = true;
        }

        // GPS
        Number latNum = (Number) update.get(param_Fields_GPS[0]);
        Number lonNum = (Number) update.get(param_Fields_GPS[1]);
        Number altNum = (Number) update.get(param_Fields_GPS[2]);
        Number[] velGPSNum = new Number[]{
                (Number) update.get(param_Fields_GPS[3]), (Number) update.get(param_Fields_GPS[4]),
                (Number) update.get(param_Fields_GPS[5])};
        Number ephNum = (Number) update.get(param_Fields_GPS[6]);
        Number epvNum = (Number) update.get(param_Fields_GPS[7]);
        boolean gpsUpdated = false;
        if (latNum != null && lonNum != null && altNum != null && velGPSNum[0] != null && velGPSNum[1] != null &&
                velGPSNum[2] != null) {
            double lat = latNum.doubleValue();
            double lon = lonNum.doubleValue();
            gpsEPH = ephNum.doubleValue();
            gpsEPV = epvNum.doubleValue();
            double alt = altNum.doubleValue();
            if (!gpsInited && baroInited) {
                gpsInited = true;
                positionProjector.init(lat, lon);
                gpsRefAlt = alt + z.get(2);
            }
            if (gpsInited && (time < 110 || time > 120)) {
                double[] gpsXY = positionProjector.project(lat, lon);
                z.set(0, gpsXY[0]);
                z.set(1, gpsXY[1]);
                z.set(2, -(alt - gpsRefAlt));
                for (int axis = 0; axis < 3; axis++) {
                    z.set(3 + axis, velGPSNum[axis].doubleValue());
                }
                gpsLast = time;
                gpsUpdated = true;
                act = true;
            }
        }
        // Acceleration
        Number accX = (Number) update.get(param_Fields_Acc[0]);
        Number accY = (Number) update.get(param_Fields_Acc[1]);
        Number accZ = (Number) update.get(param_Fields_Acc[2]);
        if (accX != null && accY != null && accZ != null) {
            acc.set(0, 0, accX.doubleValue() - accBias[0]);
            acc.set(1, 0, accY.doubleValue() - accBias[1]);
            acc.set(2, 0, accZ.doubleValue() - accBias[2]);
            act = true;
        }
        if (act && gpsInited) {
            if (!Double.isNaN(timePrev)) {
                double dt = time - timePrev;

                F.set(0, 3, dt);
                F.set(1, 4, dt);
                F.set(2, 5, dt);

                SimpleMatrix accNED = rot.mult(acc);
                accNED.set(2, accNED.get(2) + G);

                SimpleMatrix u = new SimpleMatrix(7, 1);
                u.set(3, accNED.get(0) * dt);
                u.set(4, accNED.get(1) * dt);
                u.set(5, accNED.get(2) * dt);

                // Process noise
                SimpleMatrix Q = new SimpleMatrix(7, 7);
                for (int i = 0; i < 3; i++) {
                    Q.set(i, i, dt * dt * dt * dt / 4.0 * param_Var_Acc * param_Var_Acc);
                    Q.set(3 + i, i, dt * dt * dt / 2.0 * param_Var_Acc * param_Var_Acc);
                    Q.set(i, 3 + i, dt * dt * dt / 2.0 * param_Var_Acc * param_Var_Acc);
                    Q.set(3 + i, 3 + i, dt * dt * param_Var_Acc * param_Var_Acc);
                }
                Q.set(BARO_OFFS_S_IDX, BARO_OFFS_S_IDX, dt * dt * param_Var_Baro_Offs * param_Var_Baro_Offs);

                // Prediction
                predict(u, Q);

                // Update observation matrix according to available sensors
                // GPS
                if (gpsUpdated) {
                    for (int i = 0; i < 6; i++) {
                        H.set(i, i, 1.0);
                    }
                } else if (time > gpsLast + gpsTimeout) {
                    for (int i = 0; i < 6; i++) {
                        H.set(i, i, 0.0);
                    }
                }

                // Update innovation
                SimpleMatrix y_new = z.minus(H.mult(x));
                // GPS
                if (gpsUpdated) {
                    for (int i = 0; i < 6; i++) {
                        y.set(i, y_new.get(i));
                    }
                    R.set(0, 0, gpsEPH * gpsEPH);
                    R.set(1, 1, gpsEPH * gpsEPH);
                    R.set(2, 2, gpsEPV * gpsEPV);
                }
                // Baro
                if (baroUpdated) {
                    y.set(BARO_OFFS_S_IDX, y_new.get(BARO_OFFS_S_IDX));
                }

                // Correction
                correct();

                int seriesIdx = 0;
                for (int i = 0; i < 3; i++) {
                    if (show[i]) {
                        addPoint(seriesIdx++, time, x.get(i) * scales[i]);
                        addPoint(seriesIdx++, time, x.get(i + 3) * scales[i]);
                        addPoint(seriesIdx++, time, Math.sqrt(P.get(i, i) / dt));
                        addPoint(seriesIdx++, time, Math.sqrt(P.get(i + 3, i + 3) / dt));
                    }
                }
            }
            timePrev = time;
        }
    }

    private void predict(SimpleMatrix u, SimpleMatrix Q) {
        x = F.mult(x).plus(u);
        P = F.mult(P).mult(F.transpose()).plus(Q);
    }

    private void correct() {
        // Innovation covariance
        SimpleMatrix S = H.mult(P).mult(H.transpose()).plus(R);

        // Gain
        SimpleMatrix K = P.mult(H.transpose()).mult(S.invert());
/*
        System.out.println("x:\n" + x);
        System.out.println("P:\n" + P);
        System.out.println("y:\n" + y);
        System.out.println("H:\n" + H);
        System.out.println("S:\n" + S);
        System.out.println("K:\n" + K);
*/
        // Correction
        x = x.plus(K.mult(y));
        P = I.minus(K.mult(H)).mult(P);
    }
}
