package me.drton.flightplot.processors.tools;

/**
 * User: ton Date: 11.07.13 Time: 22:11
 */
public class GlobalPositionProjector {
    private boolean inited;
    private static double r_earth = 6371000.0;
    private double phi_1;
    private double sin_phi_1;
    private double cos_phi_1;
    private double lambda_0;
    private double scale;

    public void reset() {
        inited = false;
    }

    public boolean isInited() {
        return inited;
    }

    public void init(double lat_0, double lon_0) {
        phi_1 = lat_0 / 180.0 * Math.PI;
        lambda_0 = lon_0 / 180.0 * Math.PI;
        sin_phi_1 = Math.sin(phi_1);
        cos_phi_1 = Math.cos(phi_1);
        double lat1 = phi_1;
        double lon1 = lambda_0;
        double lat2 = phi_1 + 0.5 / 180 * Math.PI;
        double lon2 = lambda_0 + 0.5 / 180 * Math.PI;
        double sin_lat_2 = Math.sin(lat2);
        double cos_lat_2 = Math.cos(lat2);
        double d = Math.acos(Math.sin(lat1) * sin_lat_2 + Math.cos(lat1) * cos_lat_2 * Math.cos(lon2 - lon1)) * r_earth;
        double k_bar = 0;
        double c = Math.acos(sin_phi_1 * sin_lat_2 + cos_phi_1 * cos_lat_2 * Math.cos(lon2 - lambda_0));
        if (0 != c)
            k_bar = c / Math.sin(c);
        double x2 = k_bar * (cos_lat_2 * Math.sin(lon2 - lambda_0)); //Projection of point 2 on plane
        double y2 = k_bar * ((cos_phi_1 * sin_lat_2 - sin_phi_1 * cos_lat_2 * Math.cos(lon2 - lambda_0)));
        double rho = Math.sqrt(Math.pow(x2, 2) + Math.pow(y2, 2));
        scale = d / rho;
        inited = true;
    }

    public double[] project(double lat, double lon) {
        if (!inited)
            throw new RuntimeException("Not initialized");
        double phi = lat / 180.0 * Math.PI;
        double lambda = lon / 180.0 * Math.PI;
        double sin_phi = Math.sin(phi);
        double cos_phi = Math.cos(phi);
        double k_bar = 0;
        double c = Math.acos(sin_phi_1 * sin_phi + cos_phi_1 * cos_phi * (1 - Math.pow((lambda - lambda_0), 2) / 2));
        if (0 != c)
            k_bar = c / Math.sin(c);
        double y = k_bar * (cos_phi * (lambda - lambda_0)) *
                scale;
        double x = k_bar * ((cos_phi_1 * sin_phi - sin_phi_1 * cos_phi * (1 - Math.pow((lambda - lambda_0), 2) / 2))) *
                scale;
        return new double[]{x, y};
    }
}
