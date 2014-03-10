package me.drton.flightplot.processors;

import me.drton.flightplot.processors.tools.GlobalPositionProjector;

import java.util.HashMap;
import java.util.Map;

/**
 * User: ton Date: 11.07.13 Time: 22:14
 */
public class GlobalPositionProjection extends PlotProcessor {
    private GlobalPositionProjector positionProjector = new GlobalPositionProjector();
    private String[] param_Fields;

    @Override
    public Map<String, Object> getDefaultParameters() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("Fields", "GPS.Lat GPS.Lon");
        params.put("Ref", "");
        return params;
    }

    @Override
    public void init() {
        super.init();
        positionProjector.reset();
        param_Fields = ((String) parameters.get("Fields")).split(WHITESPACE_RE);
        String[] ref = ((String) parameters.get("Ref")).split(WHITESPACE_RE);
        if (ref.length == 2) {
            positionProjector.init(Double.parseDouble(ref[0]), Double.parseDouble(ref[1]));
        }
        addSeries("X");
        addSeries("Y");
    }

    @Override
    public void process(double time, Map<String, Object> update) {
        // GPS
        Number latNum = (Number) update.get(param_Fields[0]);
        Number lonNum = (Number) update.get(param_Fields[1]);
        if (latNum != null && lonNum != null) {
            double lat = latNum.doubleValue();
            double lon = lonNum.doubleValue();
            if (!positionProjector.isInited()) {
                positionProjector.init(lat, lon);
            }
            double[] xy = positionProjector.project(lat, lon);
            addPoint(0, time, xy[0]);
            addPoint(1, time, xy[1]);
        }
    }
}
