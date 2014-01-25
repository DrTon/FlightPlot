package me.drton.flightplot.export;

import java.util.prefs.Preferences;

/**
 * Created by ada on 25.01.14.
 */
public class ReaderConfiguration {
    private double samplesPerSecond;
    private final static String SAMPLES_PER_SECOND_SETTING = "samplesPerSecond";

    public void saveConfiguration(Preferences preferences){
        preferences.putDouble(SAMPLES_PER_SECOND_SETTING, this.samplesPerSecond);
    }

    public void loadConfiguration(Preferences preferences){
        this.samplesPerSecond = preferences.getDouble(SAMPLES_PER_SECOND_SETTING, 10);
    }

    public double getSamplesPerSecond() {
        return samplesPerSecond;
    }

    public void setSamplesPerSecond(double samplesPerSecond) {
        this.samplesPerSecond = samplesPerSecond;
    }
}
