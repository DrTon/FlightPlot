package me.drton.flightplot;

import me.drton.flightplot.processors.PlotProcessor;

import java.util.HashMap;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * User: ton Date: 22.06.13 Time: 15:08
 */
public class ProcessorPreset {
    private String title;
    private String processorType;
    private Map<String, Object> parameters;

    public ProcessorPreset(String title, String processorType, Map<String, Object> parameters) {
        this.title = title;
        this.processorType = processorType;
        this.parameters = parameters;
    }

    public ProcessorPreset(PlotProcessor processor) {
        this.title = processor.getTitle();
        this.processorType = processor.getProcessorType();
        this.parameters = processor.getParameters();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getProcessorType() {
        return processorType;
    }

    public void setProcessorType(String processorType) {
        this.processorType = processorType;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public void pack(Preferences preferences) throws BackingStoreException {
        Preferences p = preferences.node(title);
        p.clear();
        p.put("ProcessorType", processorType);
        Preferences params = p.node("Parameters");
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            params.put(entry.getKey(), entry.getValue().toString());
        }
    }

    public static ProcessorPreset unpack(Preferences preferences) throws BackingStoreException {
        String processorType = preferences.get("ProcessorType", null);
        if (processorType == null)
            return null;
        Preferences paramsPref = preferences.node("Parameters");
        Map<String, Object> params = new HashMap<String, Object>();
        for (String key : paramsPref.keys()) {
            String v = paramsPref.get(key, null);
            if (v != null)
                params.put(key, v);
        }
        return new ProcessorPreset(preferences.name(), processorType, params);
    }
}
