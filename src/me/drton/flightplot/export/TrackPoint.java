package me.drton.flightplot.export;

/**
 * Created by ada on 23.12.13.
 */
public class TrackPoint {
    public final double lat;    /// latitude
    public final double lon;    /// longitude
    public double alt;    /// altitude AMLS
    public final long time;     /// unix time in milliseconds
    public FlightMode flightMode = FlightMode.MANUAL;  /// flight mode

    // TODO: make flight mode mandatory?
    public TrackPoint(double lat, double lon, float alt, long time) {
        this.lat = lat;
        this.lon = lon;
        this.alt = alt;
        this.time = time;
    }
}
