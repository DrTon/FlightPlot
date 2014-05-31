package me.drton.flightplot.processors;

import me.drton.flightplot.Quaternion;
import org.ejml.simple.SimpleMatrix;

import java.util.HashMap;
import java.util.Map;

/**
 * User: ton Date: 25.05.14 Time: 13:16
 */
public class AttPosEstimator extends PlotProcessor {
    private String[] param_Fields_Gyro;
    private String[] param_Fields_Acc;
    private double param_Weight_Att_Acc;
    private double param_Weight_Att_Mag;
    private double param_Weight_Gyro_Bias;
    private double timePrev;

    private SimpleMatrix gyro;
    private SimpleMatrix gyroBias;
    private SimpleMatrix acc;
    private SimpleMatrix mag;
    private SimpleMatrix magEarth;
    private Quaternion att;

    @Override
    public Map<String, Object> getDefaultParameters() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("Fields Gyro", "IMU.GyroX IMU.GyroY IMU.GyroZ");
        params.put("Fields Acc", "IMU.AccX IMU.AccY IMU.AccZ");
        params.put("Fields Mag", "IMU.MagX IMU.MagY IMU.MagZ");
        params.put("Weight Att Acc", 0.1);
        params.put("Weight Att Mag", 0.1);
        params.put("Weight Gyro Bias", 0.1);
        return params;
    }

    @Override
    public void init() {
        super.init();
        timePrev = 0.0;
        gyro = new SimpleMatrix(3, 1);
        gyroBias = new SimpleMatrix(3, 1);
        acc = new SimpleMatrix(3, 1);
        mag = new SimpleMatrix(3, 1);
        magEarth = new SimpleMatrix(3, 1);
        magEarth.set(0, 0.5);
        magEarth.set(1, 0.0);
        magEarth.set(2, 0.5);
        att = new Quaternion(1.0, 0.0, 0.0, 0.0);
        param_Fields_Gyro = ((String) parameters.get("Fields Gyro")).split(WHITESPACE_RE);
        param_Fields_Acc = ((String) parameters.get("Fields Acc")).split(WHITESPACE_RE);
        param_Weight_Att_Acc = (Double) parameters.get("Weight Att Acc");
        param_Weight_Att_Mag = (Double) parameters.get("Weight Att Mag");
        param_Weight_Gyro_Bias = (Double) parameters.get("Weight Gyro Bias");
        addSeries("Roll");
        addSeries("Pitch");
        addSeries("Yaw");
    }

    private static SimpleMatrix mul(SimpleMatrix a, SimpleMatrix b) {
        SimpleMatrix q = new SimpleMatrix(4, 1);
        q.set(0, a.get(0) * b.get(0) - a.get(1) * b.get(1) - a.get(2) * b.get(2) - a.get(3) * b.get(3));
        q.set(1, a.get(0) * b.get(1) + a.get(1) * b.get(0) + a.get(2) * b.get(3) - a.get(3) * b.get(2));
        q.set(2, a.get(0) * b.get(2) - a.get(1) * b.get(3) + a.get(2) * b.get(0) + a.get(3) * b.get(1));
        q.set(3, a.get(0) * b.get(3) + a.get(1) * b.get(2) - a.get(2) * b.get(1) + a.get(3) * b.get(0));
        return q;
    }

    private static SimpleMatrix cross(SimpleMatrix a, SimpleMatrix b) {
        SimpleMatrix v = new SimpleMatrix(3, 1);
        v.set(0, a.get(1) * b.get(2) - b.get(1) * a.get(2));
        v.set(1, a.get(2) * b.get(0) - b.get(2) * a.get(0));
        v.set(2, a.get(0) * b.get(1) - b.get(0) * a.get(1));
        return v;
    }

    private boolean getVector(Map<String, Object> update, String[] params, SimpleMatrix v) {
        boolean updated = false;
        for (int i = 0; i < params.length; i++) {
            Number val = (Number) update.get(params[i]);
            if (val != null) {
                v.set(i, val.doubleValue());
                updated = true;
            }
        }
        return updated;
    }

    private static Quaternion angleAxisToQuaternion(SimpleMatrix v) {
        double angle = Math.sqrt(v.get(0) * v.get(0) + v.get(1) * v.get(1) + v.get(2) * v.get(2));
        if (angle <= 0.0) {
            return new Quaternion(1.0, 0.0, 0.0, 0.0);
        }
        SimpleMatrix axisSin = v.scale(1.0 / angle * Math.sin(0.5 * angle));
        return new Quaternion(Math.cos(0.5 * angle), axisSin.get(0), axisSin.get(1), axisSin.get(2));
    }

    @Override
    public void process(double time, Map<String, Object> update) {
        if (timePrev != 0.0) {
            double dt = time - timePrev;
            boolean gyroUpdated = getVector(update, param_Fields_Gyro, gyro);
            boolean accUpdated = getVector(update, param_Fields_Acc, acc);
            att = att.mul(angleAxisToQuaternion(gyro.minus(gyroBias).scale(dt)));
            SimpleMatrix R = att.dcm();
            SimpleMatrix accNorm = acc.scale(-1.0 / 9.81);   // TODO normalize here?
            SimpleMatrix corrAcc = cross(R.extractVector(false, 2), accNorm);
            att = angleAxisToQuaternion(corrAcc.scale(dt * param_Weight_Att_Acc)).mul(att);
            gyroBias = gyroBias.minus(corrAcc.scale(dt * param_Weight_Att_Acc * param_Weight_Gyro_Bias));
            SimpleMatrix corrMag = R.transpose().mult(cross(R.extractVector(false, 0), magEarth));
            corrMag.set(0, 0.0);
            corrMag.set(1, 0.0);
            att = angleAxisToQuaternion(corrMag.scale(dt * param_Weight_Att_Mag)).mul(att);
            R = att.dcm();
            addPoint(0, time, Math.atan2(R.get(2, 1), R.get(2, 2)));
            addPoint(1, time, Math.asin(-R.get(2, 0)));
            addPoint(2, time, Math.atan2(R.get(1, 0), R.get(0, 0)));
        }
        timePrev = time;
    }
}
