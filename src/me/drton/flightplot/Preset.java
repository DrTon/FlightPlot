package me.drton.flightplot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * User: ton Date: 22.06.13 Time: 15:05
 */
public class Preset {
    private String title;
    private List<ProcessorPreset> processorPresets;

    public Preset() {
        this.title = "";
        this.processorPresets = new ArrayList<ProcessorPreset>();
    }

    public Preset(String title, List<ProcessorPreset> processorPresets) {
        this.title = title;
        this.processorPresets = processorPresets;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<ProcessorPreset> getProcessorPresets() {
        return processorPresets;
    }

    public void setProcessorPresets(List<ProcessorPreset> processorPresets) {
        this.processorPresets = processorPresets;
    }

    public void pack(Preferences preferences) throws BackingStoreException {
        Preferences p = preferences.node(title);
        p.clear();
        for (ProcessorPreset pp : processorPresets) {
            pp.pack(p);
        }
    }

    public static Preset unpack(Preferences preferences) throws BackingStoreException {
        String title = preferences.name();
        List<ProcessorPreset> processorPresets = new ArrayList<ProcessorPreset>();
        for (String k : preferences.childrenNames()) {
            processorPresets.add(ProcessorPreset.unpack(preferences.node(k)));
        }
        return new Preset(title, processorPresets);
    }

    public JSONObject packJSONObject() throws IOException {
        JSONObject json = new JSONObject();
        json.put("Title", title);
        JSONArray jsonProcessorPresets = new JSONArray();
        for (ProcessorPreset pp : processorPresets) {
            jsonProcessorPresets.put(pp.packJSONObject());
        }
        json.put("ProcessorPresets", jsonProcessorPresets);
        return json;
    }

    public static Preset unpackJSONObject(JSONObject json) throws IOException {
        JSONArray jsonProcessorPresets = json.getJSONArray("ProcessorPresets");
        List<ProcessorPreset> processorPresets = new ArrayList<ProcessorPreset>();
        for (int i = 0; i < jsonProcessorPresets.length(); i++) {
            processorPresets.add(ProcessorPreset.unpackJSONObject(jsonProcessorPresets.getJSONObject(i)));
        }
        return new Preset(json.getString("Title"), processorPresets);
    }

    @Override
    public String toString() {
        return title;
    }
}
