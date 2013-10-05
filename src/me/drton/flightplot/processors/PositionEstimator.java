package me.drton.flightplot.processors;

import me.drton.flightplot.processors.tools.GlobalPositionProjector;
import me.drton.flightplot.processors.tools.RotationConversion;
import org.ejml.simple.SimpleMatrix;

import java.util.HashMap;
import java.util.Map;

/**
 * User: ton Date: 28.06.13 Time: 13:40
 */
public class PositionEstimator extends PlotProcessor {
    private String[] param_Fields_GPS;
    private String[] param_Fields_Flow;
    private String[] param_Fields_Acc;
    private String[] param_Fields_Att;
    private String[] param_Fields_Z;
    private double param_Weight_GPS_Pos;
    private double param_Weight_GPS_Vel;
    private double param_Weight_Flow;
    private double param_Weight_Acc;
    private double param_Flow_K;
    private double param_Flow_Offs_X;
    private double param_Flow_Offs_Y;
    private double param_Flow_Q_Min;

    private double timePrev;
    private double[] estX;   // Pos, Vel, Acc
    private double[] estY;   // Pos, Vel, Acc
    private double[] corrGPSPos;  // X, Y
    private double[] corrGPSVel;  // VX, VY
    private double[] corrFlow;  // X, Y
    private double corrFlowW;
    private double[] corrAcc;   // X, Y
    private double[] att;
    private double z;
    private double vz;
    private double[] flowAng;
    private SimpleMatrix acc = new SimpleMatrix(3, 1);
    private SimpleMatrix R;
    private GlobalPositionProjector positionProjector = new GlobalPositionProjector();
    private static final double saturationDist = 1.0;

    @Override
    public Map<String, Object> getDefaultParameters() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("Fields GPS", "GPS.Lat GPS.Lon GPS.VelN GPS.VelE");
        params.put("Fields Flow", "FLOW.RawX FLOW.RawY FLOW.Q");
        params.put("Fields Acc", "IMU.AccX IMU.AccY IMU.AccZ");
        params.put("Fields Att", "ATT.Roll ATT.Pitch ATT.Yaw");
        params.put("Fields Z", "LPOS.Z LPOS.VZ");
        params.put("Weight GPS Pos", 1.0);
        params.put("Weight GPS Vel", 2.0);
        params.put("Weight Flow", 5.0);
        params.put("Weight Acc", 10.0);
        params.put("Flow K", 0.0165);
        params.put("Flow Offs X", 0.0);
        params.put("Flow Offs Y", 0.0);
        params.put("Flow Q Min", 0.5);
        return params;
    }

    @Override
    public void init() {
        super.init();
        timePrev = Double.NaN;
        estX = new double[]{0.0, 0.0, 0.0};
        estY = new double[]{0.0, 0.0, 0.0};
        corrGPSPos = new double[]{0.0, 0.0};
        corrGPSVel = new double[]{0.0, 0.0};
        corrFlow = new double[]{0.0, 0.0};
        corrFlowW = 0.0;
        corrAcc = new double[]{0.0, 0.0};
        z = 0.0;
        vz = 0.0;
        att = new double[]{0.0, 0.0, 0.0};
        flowAng = new double[]{0.0, 0.0};
        positionProjector.reset();
        param_Fields_GPS = ((String) parameters.get("Fields GPS")).split(WHITESPACE_RE);
        param_Fields_Flow = ((String) parameters.get("Fields Flow")).split(WHITESPACE_RE);
        param_Fields_Acc = ((String) parameters.get("Fields Acc")).split(WHITESPACE_RE);
        param_Fields_Att = ((String) parameters.get("Fields Att")).split(WHITESPACE_RE);
        param_Fields_Z = ((String) parameters.get("Fields Z")).split(WHITESPACE_RE);
        param_Weight_GPS_Pos = (Double) parameters.get("Weight GPS Pos");
        param_Weight_GPS_Vel = (Double) parameters.get("Weight GPS Vel");
        param_Weight_Flow = (Double) parameters.get("Weight Flow");
        param_Weight_Acc = (Double) parameters.get("Weight Acc");
        param_Flow_K = (Double) parameters.get("Flow K");
        param_Flow_Offs_X = (Double) parameters.get("Flow Offs X");
        param_Flow_Offs_Y = (Double) parameters.get("Flow Offs Y");
        param_Flow_Q_Min = (Double) parameters.get("Flow Q Min");
        addSeries("FlowVX");
        addSeries("FlowVY");
        addSeries("X");
        addSeries("VX");
        addSeries("Y");
        addSeries("VY");
        addSeries("Dist");
    }

    @Override
    public void process(double time, Map<String, Object> update) {
        boolean act = false;
        // Attitude
        Number roll = (Number) update.get(param_Fields_Att[0]);
        Number pitch = (Number) update.get(param_Fields_Att[1]);
        Number yaw = (Number) update.get(param_Fields_Att[2]);
        if (roll != null && pitch != null && yaw != null) {
            att[0] = roll.doubleValue();
            att[1] = pitch.doubleValue();
            att[2] = yaw.doubleValue();
            R = RotationConversion.rotationMatrixByEulerAngles(att[0], att[1], att[2]);
            act = true;
        }
        // Altitude
        Number zNum = (Number) update.get(param_Fields_Z[0]);
        Number vzNum = (Number) update.get(param_Fields_Z[1]);
        if (zNum != null) {
            z = zNum.doubleValue();
            vz = vzNum.doubleValue();
        }
        // GPS
        Number latNum = (Number) update.get(param_Fields_GPS[0]);
        Number lonNum = (Number) update.get(param_Fields_GPS[1]);
        Number velNNum = (Number) update.get(param_Fields_GPS[2]);
        Number velENum = (Number) update.get(param_Fields_GPS[3]);
        if (latNum != null && lonNum != null && velNNum != null && velENum != null) {
            double lat = latNum.doubleValue();
            double lon = lonNum.doubleValue();
            double velN = velNNum.doubleValue();
            double velE = velENum.doubleValue();
            if (!positionProjector.isInited()) {
                positionProjector.init(lat, lon);
            }
            double[] gpsXY = positionProjector.project(lat, lon);
            corrGPSPos[0] = gpsXY[0] - estX[0];
            corrGPSPos[1] = gpsXY[1] - estY[0];
            corrGPSVel[0] = velN - estX[1];
            corrGPSVel[1] = velE - estY[1];
            act = true;
        }
        // Flow
        Number flowX = (Number) update.get(param_Fields_Flow[0]);
        Number flowY = (Number) update.get(param_Fields_Flow[1]);
        Number flowQ = (Number) update.get(param_Fields_Flow[2]);
        if (flowX != null && flowY != null && flowQ != null) {
            double flowQuality = flowQ.doubleValue() / 255.0;
            if (z < -0.31 && flowQuality > param_Flow_Q_Min && R.get(2, 2) > 0.7) {
                // rotation-compensated flow, in radians, body frame
                flowAng[0] = -(flowX.doubleValue() - param_Flow_Offs_X) * param_Flow_K;
                flowAng[1] = -(flowY.doubleValue() - param_Flow_Offs_Y) * param_Flow_K;
                // distance to surface
                double dist = -z / R.get(2, 2);
                addPoint(6, time, dist);
                // measurements vector { flow_x, flow_y, vz }
                // in non-orthogonal basis { body_front, body_right, global_downside }
                SimpleMatrix m = new SimpleMatrix(3, 1);
                m.set(0, flowAng[0] * dist);
                m.set(1, flowAng[1] * dist);
                m.set(2, vz);
                // transform matrix from non-orthogonal measurements vector basis to NED
                SimpleMatrix C = new SimpleMatrix(R);
                C.set(2, 0, 0.0);
                C.set(2, 1, 0.0);
                C.set(2, 2, 1.0);
                // velocity in NED
                SimpleMatrix v = C.mult(m);
                addPoint(0, time, v.get(0));
                addPoint(1, time, v.get(1));
                corrFlow[0] = v.get(0) - estX[1];
                corrFlow[1] = v.get(1) - estY[1];
                // adjust correction weight depending on distance to surface and tilt
                double flowQWeight = (flowQuality - param_Flow_Q_Min) / (1.0 - param_Flow_Q_Min);
                corrFlowW = R.get(2, 2) * flowQWeight;
                act = true;
            } else {
                corrFlow[0] = 0.0;
                corrFlow[1] = 0.0;
            }
        }
        // Acceleration
        Number accX = (Number) update.get(param_Fields_Acc[0]);
        Number accY = (Number) update.get(param_Fields_Acc[1]);
        Number accZ = (Number) update.get(param_Fields_Acc[2]);
        if (accX != null && accY != null && accZ != null) {
            acc.set(0, 0, accX.doubleValue());
            acc.set(1, 0, accY.doubleValue());
            acc.set(2, 0, accZ.doubleValue());
            act = true;
        }
        if (act) {
            SimpleMatrix accNED = R.mult(acc);
            if (!Double.isNaN(timePrev)) {
                double dt = time - timePrev;
                corrAcc[0] = accNED.get(0) - estX[2];
                corrAcc[1] = accNED.get(1) - estY[2];
                predict(estX, dt);
                predict(estY, dt);
                correct(estX, dt, 0, corrGPSPos[0], param_Weight_GPS_Pos);
                correct(estY, dt, 0, corrGPSPos[1], param_Weight_GPS_Pos);
                correct(estX, dt, 1, corrGPSVel[0], param_Weight_GPS_Vel);
                correct(estY, dt, 1, corrGPSVel[1], param_Weight_GPS_Vel);
                correct(estX, dt, 1, corrFlow[0], param_Weight_Flow * corrFlowW);
                correct(estY, dt, 1, corrFlow[1], param_Weight_Flow * corrFlowW);
                correct(estX, dt, 2, corrAcc[0], param_Weight_Acc);
                correct(estY, dt, 2, corrAcc[1], param_Weight_Acc);
                addPoint(2, time, estX[0]);
                addPoint(3, time, estX[1]);
                addPoint(4, time, estY[0]);
                addPoint(5, time, estY[1]);
            }
            timePrev = time;
        }
    }

    private void predict(double[] q, double dt) {
        q[0] += q[1] * dt + q[2] * dt * dt / 2.0;
        q[1] += q[2] * dt;
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
