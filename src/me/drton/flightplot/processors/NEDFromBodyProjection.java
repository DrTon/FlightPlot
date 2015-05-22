package me.drton.flightplot.processors;

import me.drton.flightplot.processors.tools.LowPassFilter;
import me.drton.jmavlib.conversion.RotationConversion;
import org.la4j.Matrix;
import org.la4j.matrix.dense.Basic2DMatrix;
import org.la4j.Vector;
import org.la4j.vector.dense.BasicVector;

import java.util.HashMap;
import java.util.Map;

/**
 * User: ton Date: 14.09.13 Time: 23:45
 */
public class NEDFromBodyProjection extends PlotProcessor {
    private String[] param_Fields;
    private String[] param_Fields_Att;
    private double param_Scale;
    private double param_Offset;
    private boolean param_Backward;
    private double[] param_Att_Offsets;
    private boolean[] show;
    private LowPassFilter[] lowPassFilters;
    private Matrix r;
    private Vector v;

    @Override
    public Map<String, Object> getDefaultParameters() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("Fields", "IMU.AccX IMU.AccY IMU.AccZ");
        params.put("Fields Att", "ATT.Roll ATT.Pitch ATT.Yaw");
        params.put("Att Offsets", "0.0 0.0 0.0");
        params.put("Show", "XYZ");
        params.put("LPF", 0.0);
        params.put("Scale", 1.0);
        params.put("Offset", 0.0);
        params.put("Backward", false);
        return params;
    }

    @Override
    public void init() {
        param_Fields = ((String) parameters.get("Fields")).split(WHITESPACE_RE);
        param_Fields_Att = ((String) parameters.get("Fields Att")).split(WHITESPACE_RE);
        String[] attOffsStr = ((String) parameters.get("Att Offsets")).split(WHITESPACE_RE);
        param_Scale = (Double) parameters.get("Scale");
        param_Offset = (Double) parameters.get("Offset");
        param_Backward = (Boolean) parameters.get("Backward");
        String showStr = ((String) parameters.get("Show")).toUpperCase();
        show = new boolean[]{false, false, false};
        lowPassFilters = new LowPassFilter[3];
        v = new BasicVector(3);
        r = new Basic2DMatrix(3, 3);
        param_Att_Offsets = new double[3];
        for (int i = 0; i < 3; i++) {
            if (attOffsStr.length > i) {
                param_Att_Offsets[i] = Double.parseDouble(attOffsStr[i]);
            } else {
                param_Att_Offsets[i] = 0.0;
            }
            String axisName = "XYZ".substring(i, i + 1);
            show[i] = showStr.contains(axisName);
            if (show[i]) {
                LowPassFilter lowPassFilter = new LowPassFilter();
                lowPassFilter.setF((Double) parameters.get("LPF"));
                lowPassFilters[i] = lowPassFilter;
                addSeries(axisName);
            }
        }
    }

    @Override
    public void process(double time, Map<String, Object> update) {
        int seriesIdx = 0;
        Number roll = (Number) update.get(param_Fields_Att[0]);
        Number pitch = (Number) update.get(param_Fields_Att[1]);
        Number yaw = (Number) update.get(param_Fields_Att[2]);
        boolean act = false;
        if (roll != null && pitch != null && yaw != null) {
            // Update rotation matrix
            r = RotationConversion.rotationMatrixByEulerAngles(roll.doubleValue() + param_Att_Offsets[0],
                    pitch.doubleValue() + param_Att_Offsets[1], yaw.doubleValue() + param_Att_Offsets[2]);
            if (param_Backward) {
                r = r.transpose();
            }
            act = true;
        }
        for (int i = 0; i < 3; i++) {
            Number vNum = (Number) update.get(param_Fields[i]);
            if (vNum != null) {
                // Update source vector
                v.set(i, vNum.doubleValue());
                act = true;
            }
        }
        if (act) {
            Vector vNED = r.multiply(v);
            for (int i = 0; i < 3; i++) {
                if (show[i]) {
                    double out = lowPassFilters[i].getOutput(time, vNED.get(i));
                    addPoint(seriesIdx, time, out * param_Scale + param_Offset);
                    seriesIdx++;
                }
            }
        }
    }
}
