package me.drton.flightplot.processors;

import me.drton.flightplot.processors.tools.DelayLine;
import me.drton.flightplot.processors.tools.LowPassFilter;
import org.jfree.data.xy.XYSeriesCollection;

import java.util.HashMap;
import java.util.Map;

/**
 * User: ton Date: 15.06.13 Time: 12:04
 */
public class Simple extends PlotProcessor {
    protected String[] param_Fields;
    protected double param_Scale;
    protected double param_Offset;
    protected DelayLine delayLine = new DelayLine();
    protected LowPassFilter lowPassFilter = new LowPassFilter();
    protected XYSeriesCollection seriesCollection;

    @Override
    public Map<String, Object> getDefaultParameters() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("Fields", "ATT.Pitch ATT.Roll");
        params.put("Delay", 0.0);
        params.put("LPF", 0.0);
        params.put("Scale", 1.0);
        params.put("Offset", 0.0);
        return params;
    }

    @Override
    public void init() {
        delayLine.reset();
        delayLine.setDelay((Double) parameters.get("Delay"));
        lowPassFilter.reset();
        lowPassFilter.setF((Double) parameters.get("LPF"));
        param_Fields = ((String) parameters.get("Fields")).split(WHITESPACE_RE);
        param_Scale = (Double) parameters.get("Scale");
        param_Offset = (Double) parameters.get("Offset");
        seriesCollection = new XYSeriesCollection();
        for (String field : param_Fields) {
            seriesCollection.addSeries(createSeries(field));
        }
    }

    @Override
    public void process(double time, Map<String, Object> update) {
        for (int i = 0; i < param_Fields.length; i++) {
            String field = param_Fields[i];
            Object v = update.get(field);
            if (v != null && v instanceof Number) {
                double in = ((Number) v).doubleValue();
                double filtered = lowPassFilter.getOutput(time, in);
                double out = delayLine.getOutput(time, filtered);
                seriesCollection.getSeries(i).add(time, out * param_Scale + param_Offset);
            }
        }
    }

    @Override
    public XYSeriesCollection getSeriesCollection() {
        return seriesCollection;
    }
}
