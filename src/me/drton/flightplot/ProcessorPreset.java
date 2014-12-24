package me.drton.flightplot;

import me.drton.flightplot.processors.PlotProcessor;
import org.json.JSONObject;

import java.io.IOException;
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
    private Map<String, String> parameters;

    public ProcessorPreset(String title, String processorType, Map<String, String> parameters) {
        this.title = title;
        this.processorType = processorType;
        this.parameters = parameters;
    }

    public ProcessorPreset(PlotProcessor processor) {
        this.title = processor.getTitle();
        this.processorType = processor.getProcessorType();
        this.parameters = processor.getSerializedParameters();
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

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public void pack(Preferences preferences) throws BackingStoreException {
        Preferences p = preferences.node(title);
        p.clear();
        p.put("ProcessorType", processorType);
        Preferences params = p.node("Parameters");
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            params.put(entry.getKey(), entry.getValue());
        }
    }

    public static ProcessorPreset unpack(Preferences preferences) throws BackingStoreException {
        String processorType = preferences.get("ProcessorType", null);
        if (processorType == null)
            return null;
        Preferences paramsPref = preferences.node("Parameters");
        Map<String, String> params = new HashMap<String, String>();
        for (String key : paramsPref.keys()) {
            String v = paramsPref.get(key, null);
            if (v != null)
                params.put(key, v);
        }
        return new ProcessorPreset(preferences.name(), processorType, params);
    }

    public JSONObject packJSONObject() throws IOException {
        JSONObject json = new JSONObject();
        json.put("Title", title);
        json.put("ProcessorType", processorType);
        json.put("Parameters", new JSONObject(parameters));
        return json;
    }

    public static ProcessorPreset unpackJSONObject(JSONObject json) throws IOException {
        JSONObject jsonParameters = json.getJSONObject("Parameters");
        Map<String, String> parameters = new HashMap<String, String>();
        for (Object key : jsonParameters.keySet()) {
            String keyStr = (String) key;
            parameters.put(keyStr, jsonParameters.get(keyStr).toString());
        }
        return new ProcessorPreset(json.getString("Title"), json.getString("ProcessorType"), parameters);
    }
}
