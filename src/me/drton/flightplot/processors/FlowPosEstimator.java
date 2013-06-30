package me.drton.flightplot.processors;

import me.drton.flightplot.processors.tools.RotationConversion;
import org.ejml.simple.SimpleMatrix;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.util.HashMap;
import java.util.Map;

/**
 * User: ton Date: 28.06.13 Time: 13:40
 */
public class FlowPosEstimator extends PlotProcessor {
    private String[] param_Fields_Flow;
    private String[] param_Fields_Acc;
    private String[] param_Fields_Att;
    private String[] param_Fields_Z;
    private double param_Weight_Flow;
    private double param_Weight_Acc;
    private double param_Flow_K;
    private double param_Flow_Offs_X;
    private double param_Flow_Offs_Y;

    private double timePrev;
    private double[] estX;   // Pos, Vel, Acc
    private double[] estY;   // Pos, Vel, Acc
    private double[] corrFlow;  // X, Y
    private double[] corrAcc;   // X, Y
    private double[] att;
    private double z;
    private double vz;
    private double[] flowAng;
    private SimpleMatrix acc = new SimpleMatrix(3, 1);
    private SimpleMatrix R;
    private XYSeries seriesFlowAX;
    private XYSeries seriesFlowAY;
    private XYSeries seriesFlowX;
    private XYSeries seriesFlowY;
    private XYSeries seriesX;
    private XYSeries seriesVX;
    private XYSeries seriesY;
    private XYSeries seriesVY;
    private XYSeries seriesDist;

    @Override
    public Map<String, Object> getDefaultParameters() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("Fields Flow", "FLOW.RawX FLOW.RawY");
        params.put("Fields Acc", "IMU.AccX IMU.AccY IMU.AccZ");
        params.put("Fields Att", "ATT.Roll ATT.Pitch ATT.Yaw");
        params.put("Fields Z", "LPOS.Z LPOS.VZ");
        params.put("Weight Flow", 5.0);
        params.put("Weight Acc", 10.0);
        params.put("Flow K", 0.0165);
        params.put("Flow Offs X", 0.0);
        params.put("Flow Offs Y", 0.0);
        return params;
    }

    @Override
    public void init() {
        timePrev = Double.NaN;
        estX = new double[]{0.0, 0.0, 0.0};
        estY = new double[]{0.0, 0.0, 0.0};
        corrFlow = new double[]{0.0, 0.0};
        corrAcc = new double[]{0.0, 0.0};
        z = 0.0;
        vz = 0.0;
        att = new double[]{0.0, 0.0, 0.0};
        flowAng = new double[]{0.0, 0.0};
        param_Fields_Flow = ((String) parameters.get("Fields Flow")).split(WHITESPACE_RE);
        param_Fields_Acc = ((String) parameters.get("Fields Acc")).split(WHITESPACE_RE);
        param_Fields_Att = ((String) parameters.get("Fields Att")).split(WHITESPACE_RE);
        param_Fields_Z = ((String) parameters.get("Fields Z")).split(WHITESPACE_RE);
        param_Weight_Flow = (Double) parameters.get("Weight Flow");
        param_Weight_Acc = (Double) parameters.get("Weight Acc");
        param_Flow_K = (Double) parameters.get("Flow K");
        param_Flow_Offs_X = (Double) parameters.get("Flow Offs X");
        param_Flow_Offs_Y = (Double) parameters.get("Flow Offs Y");
        seriesFlowAX = createSeries("FlowAX");
        seriesFlowAY = createSeries("FlowAY");
        seriesFlowX = createSeries("FlowX");
        seriesFlowY = createSeries("FlowY");
        seriesX = createSeries("X");
        seriesVX = createSeries("VX");
        seriesY = createSeries("Y");
        seriesVY = createSeries("VY");
        seriesDist = createSeries("Dist");
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
        // Flow
        Number flowX = (Number) update.get(param_Fields_Flow[0]);
        Number flowY = (Number) update.get(param_Fields_Flow[1]);
        if (flowX != null && flowY != null) {
            // rotation-compensated flow, in radians, body frame
            flowAng[0] = -(flowX.doubleValue() - param_Flow_Offs_X) * param_Flow_K;
            flowAng[1] = -(flowY.doubleValue() - param_Flow_Offs_Y) * param_Flow_K;
            // distance to surface
            double dist = -z / R.get(2, 2);
            seriesDist.add(time, dist);
            // calculate X and Y components of vector in body frame from angular flow
            double[] flow = new double[]{0.0, 0.0};
            flow[0] = flowAng[0] * dist;
            flow[1] = flowAng[1] * dist;
            SimpleMatrix A = R.extractMatrix(0, 2, 0, 2);
            SimpleMatrix b = new SimpleMatrix(
                    new double[][]{{flow[0] - R.get(0, 2) * vz}, {flow[1] - R.get(1, 2) * vz}});
            SimpleMatrix x = A.invert().mult(b);
            seriesFlowAX.add(time, flowAng[0]);
            seriesFlowAY.add(time, flowAng[1]);
            seriesFlowX.add(time, x.get(0));
            seriesFlowY.add(time, x.get(1));
            corrFlow[0] = x.get(0) - estX[1];
            corrFlow[1] = x.get(1) - estY[1];
            act = true;
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
                correct(estX, dt, 1, corrFlow[0], param_Weight_Flow);
                correct(estY, dt, 1, corrFlow[1], param_Weight_Flow);
                correct(estX, dt, 2, corrAcc[0], param_Weight_Acc);
                correct(estY, dt, 2, corrAcc[1], param_Weight_Acc);
                seriesX.add(time, estX[0]);
                seriesVX.add(time, estX[1]);
                seriesY.add(time, estY[0]);
                seriesVY.add(time, estY[1]);
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
        q[i] += ewdt;
        if (i == 0) {
            q[1] += w * ewdt;
            q[2] += w * w * ewdt / 3.0;
        } else if (i == 1) {
            q[2] += w * ewdt;
        }
    }

    @Override
    public XYSeriesCollection getSeriesCollection() {
        XYSeriesCollection seriesCollection = new XYSeriesCollection();
        seriesCollection.addSeries(seriesFlowX);
        seriesCollection.addSeries(seriesFlowY);
        seriesCollection.addSeries(seriesFlowAX);
        seriesCollection.addSeries(seriesFlowAY);
        seriesCollection.addSeries(seriesX);
        seriesCollection.addSeries(seriesVX);
        seriesCollection.addSeries(seriesY);
        seriesCollection.addSeries(seriesVY);
        seriesCollection.addSeries(seriesDist);
        return seriesCollection;
    }
}
