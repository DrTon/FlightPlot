package me.drton.flightplot;

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

    @Override
    public String toString() {
        return title;
    }
}
