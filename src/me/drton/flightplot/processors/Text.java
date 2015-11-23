package me.drton.flightplot.processors;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ton on 29.09.15.
 */
public class Text extends PlotProcessor {
    protected String param_Field;

    @Override
    public Map<String, Object> getDefaultParameters() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("Field", "STATUS_TEXT.text");
        return params;
    }

    @Override
    public void init() {
        param_Field = (String) parameters.get("Field");
        addMarkersList();
    }

    @Override
    public void process(double time, Map<String, Object> update) {
        Object v = update.get(param_Field);
        if (v != null && v instanceof String) {
            addMarker(0, time, (String) v);
        }
    }
}
