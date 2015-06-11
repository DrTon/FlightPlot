package me.drton.flightplot.processors;

import me.drton.jmavlib.conversion.RotationConversion;
import me.drton.jmavlib.geo.GlobalPositionProjector;
import me.drton.jmavlib.geo.LatLonAlt;
import me.drton.jmavlib.processing.DelayLine;

import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * User: ton Date: 28.06.13 Time: 13:40
 */
public class PositionEstimator extends PlotProcessor {
    private static final double G = 9.81;
    private String[] param_Fields_GPS;
    private String param_Field_Baro;
    private String[] param_Fields_Acc;
    private String[] param_Fields_Att;
    private double[][] param_W_GPS;
    private double param_W_Baro;
    private double param_W_Acc_Bias;
    /*
    private String[] param_Fields_Flow;
    private double param_W_Flow;
    private double param_Flow_K;
    private double param_Flow_Offs_X;
    private double param_Flow_Offs_Y;
    private double param_Flow_Q_Min;
    */

    private double timePrev;
    private double[][] est;   // [axis][order]
    private double[][] corrGPS;  // [axis][order]
    /*
    private double[] corrFlow;  // X, Y, Z
    private double corrFlowW;
    private double[] flowAng;
    */
    private double corrBaro;
    private double[] wGPS;
    private double baro;
    private double baroOffset;
    private double[][] gps;     // [axis][order]
    private Vector3d acc;
    private Matrix3d rot;
    private GlobalPositionProjector positionProjector;
    private Vector3d accBias;
    private DelayLine<double[][]> delayLineGPS;
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
        params.put("Delay GPS", 0.2);
        params.put("W XY GPS P", 1.0);
        params.put("W XY GPS V", 2.0);
        params.put("W Z Baro", 0.5);
        params.put("W Z GPS P", 0.005);
        params.put("W Acc Bias", 0.05);
        params.put("Show", "XYZ");
        params.put("Offsets", "0.0 0.0 0.0");
        params.put("Z Downside", false);
        /*
        params.put("Fields Flow", "FLOW.RawX FLOW.RawY FLOW.Q");
        params.put("W XY Flow", 5.0);
        params.put("Flow K", 0.0165);
        params.put("Flow Offs X", 0.0);
        params.put("Flow Offs Y", 0.0);
        params.put("Flow Q Min", 0.5);
        */
        return params;
    }

    @Override
    public void init() {
        timePrev = Double.NaN;
        gpsInited = false;
        baroInited = false;
        rot = null;
        est = new double[3][2];
        corrGPS = new double[3][2];
        corrBaro = 0.0;
        wGPS = new double[3];
        acc = new Vector3d();
        rot = new Matrix3d();
        accBias = new Vector3d();
        gps = new double[3][2];
        baro = 0.0;
        baroOffset = 0.0;
        /*
        corrFlow = new double[]{0.0, 0.0, 0.0};
        corrFlowW = 0.0;
        flowAng = new double[]{0.0, 0.0};
        */
        delayLineGPS = new DelayLine<double[][]>();
        positionProjector = new GlobalPositionProjector();
        param_Fields_GPS = ((String) parameters.get("Fields GPS")).split(WHITESPACE_RE);
        param_Fields_Acc = ((String) parameters.get("Fields Acc")).split(WHITESPACE_RE);
        param_Fields_Att = ((String) parameters.get("Fields Att")).split(WHITESPACE_RE);
        param_Field_Baro = (String) parameters.get("Field Baro");
        param_W_GPS = new double[3][2];
        param_W_GPS[0][0] = (Double) parameters.get("W XY GPS P");
        param_W_GPS[1][0] = param_W_GPS[0][0];
        param_W_GPS[0][1] = (Double) parameters.get("W XY GPS V");
        param_W_GPS[1][1] = param_W_GPS[0][1];
        param_W_GPS[2][0] = (Double) parameters.get("W Z GPS P");
        param_W_Baro = (Double) parameters.get("W Z Baro");
        param_W_Acc_Bias = (Double) parameters.get("W Acc Bias");
        /*
        param_Fields_Flow = ((String) parameters.get("Fields Flow")).split(WHITESPACE_RE);
        param_W_Flow = (Double) parameters.get("W XY Flow");
        param_Flow_K = (Double) parameters.get("Flow K");
        param_Flow_Offs_X = (Double) parameters.get("Flow Offs X");
        param_Flow_Offs_Y = (Double) parameters.get("Flow Offs Y");
        param_Flow_Q_Min = (Double) parameters.get("Flow Q Min");
        */
        delayLineGPS.setDelay((Double) parameters.get("Delay GPS"));
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
                addSeries("CorrGPS_V_" + axisName);
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
            rot.set(RotationConversion.rotationMatrixByEulerAngles(roll.doubleValue(), pitch.doubleValue(),
                    yaw.doubleValue()));
            act = true;
        }
        // Baro
        Number baroNum = (Number) update.get(param_Field_Baro);
        if (baroNum != null) {
            baro = baroNum.doubleValue();
            if (!baroInited) {
                baroInited = true;
                est[2][0] = -baro;
            }
            corrBaro = -baro - baroOffset - est[2][0];
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
        if (latNum != null && lonNum != null && altNum != null && velGPSNum[0] != null && velGPSNum[1] != null &&
                velGPSNum[2] != null) {
            double lat = latNum.doubleValue();
            double lon = lonNum.doubleValue();
            double eph = ephNum.doubleValue();
            double epv = epvNum.doubleValue();
            double alt = altNum.doubleValue();
            if (!gpsInited && baroInited) {
                gpsInited = true;
                positionProjector.init(new LatLonAlt(lat, lon, alt));
                est[2][0] = -alt;
                baroOffset = alt - baro;
            }
            if (gpsInited) {
                double[] gpsProj = positionProjector.project(new LatLonAlt(lat, lon, alt));
                gps[0][0] = gpsProj[0];
                gps[1][0] = gpsProj[1];
                gps[2][0] = gpsProj[2];
                for (int axis = 0; axis < 3; axis++) {
                    gps[axis][1] = velGPSNum[axis].doubleValue();
                }
                double[][] outOld = delayLineGPS.getOutput(time);
                if (outOld != null) {
                    for (int axis = 0; axis < 3; axis++) {
                        for (int posVel = 0; posVel < 2; posVel++) {
                            corrGPS[axis][posVel] = gps[axis][posVel] - outOld[axis][posVel];
                        }
                    }
                }
                wGPS[0] = 2.0 / Math.max(2.0, eph);
                wGPS[1] = wGPS[0];
                wGPS[2] = 4.0 / Math.max(4.0, epv);
                act = true;
            }
        }
        // Flow
        /*
        Number flowX = (Number) update.get(param_Fields_Flow[0]);
        Number flowY = (Number) update.get(param_Fields_Flow[1]);
        Number flowQ = (Number) update.get(param_Fields_Flow[2]);
        if (flowX != null && flowY != null && flowQ != null) {
            double flowQuality = flowQ.doubleValue() / 255.0;
            if (z < -0.31 && flowQuality > param_Flow_Q_Min && rot.get(2, 2) > 0.7) {
                // rotation-compensated flow, in radians, body frame
                flowAng[0] = -(flowX.doubleValue() - param_Flow_Offs_X) * param_Flow_K;
                flowAng[1] = -(flowY.doubleValue() - param_Flow_Offs_Y) * param_Flow_K;
                // distance to surface
                double dist = -z / rot.get(2, 2);
                addPoint(6, time, dist);
                // measurements vector { flow_x, flow_y, vz }
                // in non-orthogonal basis { body_front, body_right, global_downside }
                SimpleMatrix m = new SimpleMatrix(3, 1);
                m.set(0, flowAng[0] * dist);
                m.set(1, flowAng[1] * dist);
                m.set(2, vz);
                // transform matrix from non-orthogonal measurements vector basis to NED
                SimpleMatrix C = new SimpleMatrix(rot);
                C.set(2, 0, 0.0);
                C.set(2, 1, 0.0);
                C.set(2, 2, 1.0);
                // velocity in NED
                SimpleMatrix v = C.mult(m);
                addPoint(0, time, v.get(0));
                addPoint(1, time, v.get(1));
                corrFlow[0] = v.get(0) - est[0][1];
                corrFlow[1] = v.get(1) - est[1][1];
                // adjust correction weight depending on distance to surface and tilt
                double flowQWeight = (flowQuality - param_Flow_Q_Min) / (1.0 - param_Flow_Q_Min);
                corrFlowW = rot.get(2, 2) * flowQWeight;
                act = true;
            } else {
                corrFlow[0] = 0.0;
                corrFlow[1] = 0.0;
            }
        }
        */
        // Acceleration
        Number accX = (Number) update.get(param_Fields_Acc[0]);
        Number accY = (Number) update.get(param_Fields_Acc[1]);
        Number accZ = (Number) update.get(param_Fields_Acc[2]);
        if (accX != null && accY != null && accZ != null) {
            acc.setX(accX.doubleValue());
            acc.setY(accY.doubleValue());
            acc.setZ(accZ.doubleValue());
            acc.sub(accBias);
            act = true;
        }
        if (act) {
            if (!Double.isNaN(timePrev) && rot != null) {
                double dt = time - timePrev;
                double dBaro = corrGPS[2][0] * param_W_GPS[2][0] * wGPS[2] * dt;
                baroOffset -= dBaro;
                corrBaro += dBaro;
                double[] accBiasCorrArr = new double[3];
                for (int axis = 0; axis < 3; axis++) {
                    double wPos = param_W_GPS[axis][0] * param_W_GPS[axis][0] * wGPS[axis] * wGPS[axis];
                    double wVel = param_W_GPS[axis][1] * wGPS[axis];
                    accBiasCorrArr[axis] = -corrGPS[axis][0] * wPos - corrGPS[axis][1] * wVel;
                }
                accBiasCorrArr[2] = -corrBaro * param_W_Baro * param_W_Baro;
                Matrix3d rotT = new Matrix3d(rot);
                rotT.transpose();
                Vector3d b = new Vector3d(accBiasCorrArr);
                rotT.transform(b);
                b.scale(param_W_Acc_Bias * dt);
                Vector3d accNED = new Vector3d(acc);
                rot.transform(accNED);
                accNED.setZ(accNED.getZ() + G);
                accBias.add(b);
                double accNEDArr[] = new double[3];
                accNED.get(accNEDArr);
                predict(est, dt, accNEDArr);
                for (int axis = 0; axis < 3; axis++) {
                    correct(est[axis], dt, 0, corrGPS[axis][0], param_W_GPS[axis][0] * wGPS[axis]);
                    correct(est[axis], dt, 1, corrGPS[axis][1], param_W_GPS[axis][1] * wGPS[axis]);
                    //correct(estX, dt, 1, corrFlow[0], param_W_Flow * corrFlowW);
                    //correct(estY, dt, 1, corrFlow[1], param_W_Flow * corrFlowW);
                }
                correct(est[2], dt, 0, corrBaro, param_W_Baro);

                delayLineGPS.getOutput(time, deepCopy(est));

                if (gpsInited && baroInited) {
                    int seriesIdx = 0;
                    for (int axis = 0; axis < 3; axis++) {
                        if (show[axis]) {
                            addPoint(seriesIdx++, time, est[axis][0] * scales[axis] + offsets[axis]);
                            addPoint(seriesIdx++, time, est[axis][1] * scales[axis]);
                            addPoint(seriesIdx++, time, corrGPS[axis][1]);
                        }
                    }
                }
            }
            timePrev = time;
        }
    }

    private void predict(double[][] q, double dt, double[] acc) {
        for (int axis = 0; axis < 3; axis++) {
            q[axis][0] += q[axis][1] * dt + acc[axis] * dt * dt / 2.0;
            q[axis][1] += acc[axis] * dt;
        }
    }

    private void correct(double[] q, double dt, int i, double e, double w) {
        double ewdt = w * e * dt;
        if (Double.isNaN(ewdt)) {
            return;
        }
        q[i] += ewdt;
        if (i == 0) {
            q[1] += w * ewdt;
        }
    }

    public static double[][] deepCopy(double[][] original) {
        if (original == null) {
            return null;
        }

        final double[][] result = new double[original.length][];
        for (int i = 0; i < original.length; i++) {
            result[i] = Arrays.copyOf(original[i], original[i].length);
        }
        return result;
    }
}
