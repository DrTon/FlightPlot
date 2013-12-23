package me.drton.flightplot.export;

/**
 * Created by ada on 23.12.13.
 */
public class KmlTrackPoint {

    private Float alt = null;
    private Double lon = null;
    private Double lat = null;
    private long timeInSeconds = 0;

    public Float getAlt() {
        return alt;
    }

    public void setAlt(Float alt) {
        this.alt = alt;
    }

    public Double getLon() {
        return lon;
    }

    public void setLon(Double lon) {
        this.lon = lon;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public long getTimeInSeconds() {
        return timeInSeconds;
    }

    public void setTimeInSeconds(long timeInSeconds) {
        this.timeInSeconds = timeInSeconds;
    }
}
