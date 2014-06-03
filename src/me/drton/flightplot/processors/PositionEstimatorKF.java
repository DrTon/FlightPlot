package me.drton.flightplot.processors;

import me.drton.jmavlib.conversion.RotationConversion;
import me.drton.jmavlib.geo.GlobalPositionProjector;
import me.drton.jmavlib.geo.LatLonAlt;
import me.drton.jmavlib.processing.DelayLine;
import org.la4j.LinearAlgebra;
import org.la4j.matrix.Matrix;
import org.la4j.matrix.dense.Basic2DMatrix;
import org.la4j.vector.Vector;
import org.la4j.vector.dense.BasicVector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    private double param_Delay_GPS;

    private double timePrev;

    /*     0      1      2      3      4      5      6
       x:  x      y      z      vx     vy     vz     baro_offs
       z:  gps_x  gps_y  gps_z  gps_vx gps_vy gps_vz baro

       baro_offs = baro + z
     */

    private static final int Z_S_IDX = 2;
    private static final int BARO_OFFS_S_IDX = 6;
    private static final int BARO_O_IDX = 6;

    private Matrix I;         // unity matrix
    private Vector x;         // state
    private Vector y;         // innovation
    private Matrix P;         // covariance
    private Matrix F;         // transition matrix
    private Matrix H;         // observation matrix
    private Vector z;         // observation
    private Matrix R;         // covariance of the observation noise

    private double gpsRefAlt;
    private double gpsEPH;
    private double gpsEPV;
    private double gpsLast;
    private double gpsTimeout;
    private Vector acc;
    private Matrix rot;
    private GlobalPositionProjector positionProjector = new GlobalPositionProjector();
    private Vector accBias;
    private List<DelayLine.Tick<Vector>> xBuffer;
    private double bufferLen = 0.5;
    private boolean gpsInited;
    private boolean baroInited;
    private boolean[] show;
    private double[] offsets;
    private double[] scales;
    private LinearAlgebra.InverterFactory inverter = LinearAlgebra.InverterFactory.GAUSS_JORDAN;

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
        I = new Basic2DMatrix(7, 7);
        x = new BasicVector(7);
        y = new BasicVector(7);
        P = new Basic2DMatrix(7, 7);
        F = new Basic2DMatrix(7, 7);
        H = new Basic2DMatrix(7, 7);
        z = new BasicVector(7);
        R = new Basic2DMatrix(7, 7);

        for (int i = 0; i < 7; i++) {
            P.set(i, i, 1.0);
            I.set(i, i, 1.0);
            F.set(i, i, 1.0);
        }

        H.set(BARO_O_IDX, Z_S_IDX, -1);
        H.set(BARO_O_IDX, BARO_OFFS_S_IDX, 1);

        acc = new BasicVector(3);
        accBias = new BasicVector(3);
        baroInited = false;
        gpsInited = false;
        gpsRefAlt = 0.0;
        gpsEPH = 1.0;
        gpsEPV = 1.0;
        gpsLast = 0.0;
        gpsTimeout = 0.3;
        xBuffer = new ArrayList<DelayLine.Tick<Vector>>();
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

        param_Delay_GPS = (Double) parameters.get("Delay GPS");

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
                positionProjector.init(new LatLonAlt(lat, lon, alt));
                gpsRefAlt = alt + z.get(2);
            }
            if (gpsInited && (time < 110 || time > 120)) {
                double[] gpsXY = positionProjector.project(new LatLonAlt(lat, lon, alt));
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
            acc.set(0, accX.doubleValue() - accBias.get(0));
            acc.set(1, accY.doubleValue() - accBias.get(1));
            acc.set(2, accZ.doubleValue() - accBias.get(2));
            act = true;
        }
        if (act && gpsInited) {
            if (!Double.isNaN(timePrev)) {
                double dt = time - timePrev;

                F.set(0, 3, dt);
                F.set(1, 4, dt);
                F.set(2, 5, dt);

                Vector accNED = rot.multiply(acc);
                accNED.set(2, accNED.get(2) + G);

                Vector u = new BasicVector(7);
                u.set(3, accNED.get(0) * dt);
                u.set(4, accNED.get(1) * dt);
                u.set(5, accNED.get(2) * dt);

                // Process noise
                Matrix Q = new Basic2DMatrix(7, 7);
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
                Vector y_new = z.subtract(H.multiply(x));
                // GPS
                if (gpsUpdated) {
                    Vector xGPS = getOldState(time - param_Delay_GPS);
                    if (xGPS != null) {
                        Vector yGPS = z.subtract(H.multiply(xGPS));
                        for (int i = 0; i < 6; i++) {
                            y.set(i, yGPS.get(i));
                        }
                        R.set(0, 0, gpsEPH * gpsEPH);
                        R.set(1, 1, gpsEPH * gpsEPH);
                        R.set(2, 2, gpsEPV * gpsEPV);
                    }
                }
                // Baro
                if (baroUpdated) {
                    y.set(BARO_OFFS_S_IDX, y_new.get(BARO_OFFS_S_IDX));
                }

                // Correction
                correct();

                // Store new state to buffer
                xBuffer.add(new DelayLine.Tick<Vector>(time, x.copy()));
                // Remove too old states
                while (!xBuffer.isEmpty() && xBuffer.get(0).time < time - bufferLen) {
                    xBuffer.remove(0);
                }

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

    private Vector getOldState(double time) {
        int i = xBuffer.size() - 1;
        DelayLine.Tick<Vector> tick = null;
        while (i >= 0) {
            tick = xBuffer.get(i);
            if (tick.time <= time) {
                break;
            }
            i--;
        }
        return tick != null ? tick.value : null;
    }

    private void predict(Vector u, Matrix Q) {
        x = F.multiply(x).add(u);
        P = F.multiply(P).multiply(F.transpose()).add(Q);
    }

    private void correct() {
        // Innovation covariance
        Matrix S = H.multiply(P).multiply(H.transpose()).add(R);

        // Gain
        Matrix K = P.multiply(H.transpose()).multiply(S.withInverter(inverter).inverse());
/*
        System.out.println("x:\n" + x);
        System.out.println("P:\n" + P);
        System.out.println("y:\n" + y);
        System.out.println("H:\n" + H);
        System.out.println("S:\n" + S);
        System.out.println("K:\n" + K);
*/
        // Correction
        x = x.add(K.multiply(y));
        P = I.subtract(K.multiply(H)).multiply(P);
    }
}
