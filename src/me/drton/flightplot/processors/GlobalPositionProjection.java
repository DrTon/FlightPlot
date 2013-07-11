package me.drton.flightplot.processors;

import me.drton.flightplot.processors.tools.GlobalPositionProjector;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.util.HashMap;
import java.util.Map;

/**
 * User: ton Date: 11.07.13 Time: 22:14
 */
public class GlobalPositionProjection extends PlotProcessor {
    private GlobalPositionProjector positionProjector = new GlobalPositionProjector();
    private String[] param_Fields;
    private XYSeries seriesX;
    private XYSeries seriesY;

    @Override
    public Map<String, Object> getDefaultParameters() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("Fields", "GPS.Lat GPS.Lon");
        return params;
    }

    @Override
    public void init() {
        positionProjector.reset();
        param_Fields = ((String) parameters.get("Fields")).split(WHITESPACE_RE);
        seriesX = createSeries("X");
        seriesY = createSeries("Y");
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
            seriesX.add(time, xy[0]);
            seriesY.add(time, xy[1]);
        }
    }

    @Override
    public XYSeriesCollection getSeriesCollection() {
        XYSeriesCollection seriesCollection = new XYSeriesCollection();
        seriesCollection.addSeries(seriesX);
        seriesCollection.addSeries(seriesY);
        return seriesCollection;
    }
}
