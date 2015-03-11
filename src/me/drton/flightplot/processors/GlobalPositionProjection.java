package me.drton.flightplot.processors;

import me.drton.jmavlib.geo.GlobalPositionProjector;
import me.drton.jmavlib.geo.LatLonAlt;

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
        positionProjector.reset();
        param_Fields = ((String) parameters.get("Fields")).split(WHITESPACE_RE);
        String[] ref = ((String) parameters.get("Ref")).split(WHITESPACE_RE);
        if (ref.length >= 2) {
            positionProjector.init(new LatLonAlt(Double.parseDouble(ref[0]), Double.parseDouble(ref[1]), 0.0));
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
            LatLonAlt latLonAlt = new LatLonAlt(latNum.doubleValue(), lonNum.doubleValue(), 0.0);
            if (!positionProjector.isInited()) {
                positionProjector.init(latLonAlt);
            }
            double[] xyz = positionProjector.project(latLonAlt);
            addPoint(0, time, xyz[0]);
            addPoint(1, time, xyz[1]);
        }
    }
}
