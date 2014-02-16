package me.drton.flightplot.export;

/**
 * Created by ada on 16.02.14.
 */
public class ConfigurationException extends  Exception {
    public ConfigurationException() {
        super();
    }

    public ConfigurationException(String s) {
        super(s);
    }

    public ConfigurationException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public ConfigurationException(Throwable throwable) {
        super(throwable);
    }
}
