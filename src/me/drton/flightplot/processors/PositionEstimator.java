package me.drton.flightplot.processors;

import me.drton.flightplot.processors.tools.DelayLine;
import me.drton.flightplot.processors.tools.GlobalPositionProjector;
import me.drton.flightplot.processors.tools.RotationConversion;
import org.ejml.simple.SimpleMatrix;

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
    private double[] param_W_Acc;
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
    private double[] corrAcc;   // X, Y, Z
    private double corrBaro;
    private double[] wGPS;
    private double baro;
    private double baroOffset;
    private double[][] gps;     // [axis][order]
    private SimpleMatrix acc = new SimpleMatrix(3, 1);
    private SimpleMatrix rot;
    private GlobalPositionProjector positionProjector = new GlobalPositionProjector();
    private double[] accBias;
    private DelayLine[][] delayLinesGPS;
    private boolean gpsInited;
    private boolean baroInited;

    @Override
    public Map<String, Object> getDefaultParameters() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("Fields GPS", "GPS.Lat GPS.Lon GPS.Alt GPS.VelN GPS.VelE GPS.VelD GPS.EPH GPS.EPV");
        params.put("Field Baro", "SENS.BaroAlt");
        params.put("Fields Acc", "IMU.AccX IMU.AccY IMU.AccZ");
        params.put("Fields Att", "ATT.Roll ATT.Pitch ATT.Yaw");
        params.put("Delay GPS", 0.1);
        params.put("W XY Acc", 20.0);
        params.put("W XY GPS P", 1.0);
        params.put("W XY GPS V", 2.0);
        params.put("W Z Acc", 20.0);
        params.put("W Z Baro", 0.5);
        params.put("W Z GPS P", 0.005);
        params.put("W Acc Bias", 0.05);
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
        super.init();
        timePrev = Double.NaN;
        gpsInited = false;
        baroInited = false;
        est = new double[3][3];
        corrGPS = new double[3][3];
        corrAcc = new double[]{0.0, 0.0, 0.0};
        corrBaro = 0.0;
        wGPS = new double[3];
        accBias = new double[]{0.0, 0.0, 0.0};
        gps = new double[3][2];
        baro = 0.0;
        baroOffset = 0.0;
        /*
        corrFlow = new double[]{0.0, 0.0, 0.0};
        corrFlowW = 0.0;
        flowAng = new double[]{0.0, 0.0};
        */
        delayLinesGPS = new DelayLine[3][2];
        positionProjector.reset();
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
        param_W_Acc = new double[3];
        param_W_Acc[0] = (Double) parameters.get("W XY Acc");
        param_W_Acc[1] = param_W_Acc[0];
        param_W_Acc[2] = (Double) parameters.get("W Z Acc");
        param_W_Acc_Bias = (Double) parameters.get("W Acc Bias");
        /*
        param_Fields_Flow = ((String) parameters.get("Fields Flow")).split(WHITESPACE_RE);
        param_W_Flow = (Double) parameters.get("W XY Flow");
        param_Flow_K = (Double) parameters.get("Flow K");
        param_Flow_Offs_X = (Double) parameters.get("Flow Offs X");
        param_Flow_Offs_Y = (Double) parameters.get("Flow Offs Y");
        param_Flow_Q_Min = (Double) parameters.get("Flow Q Min");
        */
        double delayGPS = (Double) parameters.get("Delay GPS");
        for (int axis = 0; axis < 3; axis++) {
            for (int posVel = 0; posVel < 2; posVel++) {
                delayLinesGPS[axis][posVel] = new DelayLine();
                delayLinesGPS[axis][posVel].setDelay(delayGPS);
                delayLinesGPS[axis][posVel].getOutput(0.0, 0.0);
            }
        }
        addSeries("X");
        addSeries("Y");
        addSeries("Z");
        addSeries("VX");
        addSeries("VY");
        addSeries("VZ");
        //addSeries("Dist");
        //addSeries("FlowVX");
        //addSeries("FlowVY");
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
                positionProjector.init(lat, lon);
                est[2][0] = -alt;
                baroOffset = alt - baro;
            }
            double[] gpsXY = positionProjector.project(lat, lon);
            gps[0][0] = gpsXY[0];
            gps[1][0] = gpsXY[1];
            gps[2][0] = -alt;
            for (int axis = 0; axis < 3; axis++) {
                gps[axis][1] = velGPSNum[axis].doubleValue();
            }
            for (int axis = 0; axis < 3; axis++) {
                for (int posVel = 0; posVel < 2; posVel++) {
                    corrGPS[axis][posVel] = gps[axis][posVel] -
                            delayLinesGPS[axis][posVel].getOutput(time, est[axis][posVel]);
                }
            }
            wGPS[0] = 2.0 / Math.max(2.0, eph);
            wGPS[1] = wGPS[0];
            wGPS[2] = 4.0 / Math.max(4.0, epv);
            act = true;
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
            acc.set(0, 0, accX.doubleValue() - accBias[0]);
            acc.set(1, 0, accY.doubleValue() - accBias[1]);
            acc.set(2, 0, accZ.doubleValue() - accBias[2]);
            act = true;
        }
        if (act) {
            if (!Double.isNaN(timePrev)) {
                double dt = time - timePrev;
                double dBaro = corrGPS[2][0] * param_W_GPS[2][0] * wGPS[2] * dt;
                baroOffset -= dBaro;
                corrBaro += dBaro;
                SimpleMatrix accBiasCorrV = new SimpleMatrix(3, 1);
                for (int axis = 0; axis < 2; axis++) {
                    double wPos = param_W_GPS[axis][0] * param_W_GPS[axis][0] * wGPS[axis] * wGPS[axis];
                    double wVel = param_W_GPS[axis][1] * wGPS[axis];
                    accBiasCorrV.set(axis, corrGPS[axis][0] * wPos + corrGPS[axis][1] * wVel);
                }
                accBiasCorrV.set(2, (corrBaro * param_W_Baro));
                SimpleMatrix b = rot.transpose().mult(accBiasCorrV).scale(param_W_Acc_Bias * dt);
                SimpleMatrix accNED = rot.mult(acc);
                accNED.set(2, accNED.get(2) + G);
                for (int axis = 0; axis < 3; axis++) {
                    accBias[axis] -= b.get(axis);
                    corrAcc[axis] = accNED.get(axis) - est[axis][2];
                }
                predict(est, dt);
                for (int axis = 0; axis < 3; axis++) {
                    correct(est[axis], dt, 0, corrGPS[axis][0], param_W_GPS[axis][0] * wGPS[axis]);
                    correct(est[axis], dt, 1, corrGPS[axis][1], param_W_GPS[axis][1] * wGPS[axis]);
                    //correct(estX, dt, 1, corrFlow[0], param_W_Flow * corrFlowW);
                    //correct(estY, dt, 1, corrFlow[1], param_W_Flow * corrFlowW);
                    correct(est[axis], dt, 2, corrAcc[axis], param_W_Acc[axis]);
                }
                correct(est[2], dt, 0, corrBaro, param_W_Baro);
                if (gpsInited && baroInited) {
                    addPoint(0, time, est[0][0]);
                    addPoint(1, time, est[1][0]);
                    addPoint(2, time, est[2][0]);
                    addPoint(3, time, est[0][1]);
                    addPoint(4, time, est[1][1]);
                    addPoint(5, time, est[2][1]);
                }
            }
            timePrev = time;
        }
    }

    private void predict(double[][] q, double dt) {
        for (int axis = 0; axis < 3; axis++) {
            q[axis][0] += q[axis][1] * dt + q[axis][2] * dt * dt / 2.0;
            q[axis][1] += q[axis][2] * dt;
        }
    }

    private void correct(double[] q, double dt, int i, double e, double w) {
        double ewdt = w * e * dt;
        if (Double.isNaN(ewdt))
            return;
        q[i] += ewdt;
        if (i == 0) {
            q[1] += w * ewdt;
            q[2] += w * w * ewdt / 3.0;
        } else if (i == 1) {
            q[2] += w * ewdt;
        }
    }
}
