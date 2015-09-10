package me.drton.flightplot.export;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Created by ada on 23.12.13.
 */
public class KMLTrackExporter extends AbstractTrackExporter {
    private static final String LINE_STYLE_RED = "red";
    private static final String LINE_STYLE_GREEN = "green";
    private static final String LINE_STYLE_BLUE = "blue";
    private static final String LINE_STYLE_CYAN = "cyan";
    private static final String LINE_STYLE_MAGENTA = "magenta";
    private static final String LINE_STYLE_YELLOW = "yellow";
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    protected String getStyleForFlightMode(String flightMode) {
        if (flightMode == null) {
            return LINE_STYLE_YELLOW;
        }
        if ("MANUAL".equals(flightMode)) {
            return LINE_STYLE_RED;
        } else if ("ALTCTL".equals(flightMode)) {
            return LINE_STYLE_YELLOW;
        } else if ("POSCTL".equals(flightMode)) {
            return LINE_STYLE_GREEN;
        } else if ("AUTO_MISSION".equals(flightMode)) {
            return LINE_STYLE_BLUE;
        } else if ("AUTO_LOITER".equals(flightMode)) {
            return LINE_STYLE_CYAN;
        } else if ("AUTO_RTL".equals(flightMode)) {
            return LINE_STYLE_MAGENTA;
        } else if ("AUTO_ACRO".equals(flightMode)) {
            return LINE_STYLE_RED;
        } else if ("AUTO_OFFBOARD".equals(flightMode)) {
            return LINE_STYLE_BLUE;
        } else {
            return LINE_STYLE_YELLOW;
        }
    }

    protected void writeStart() throws IOException {
        // TODO: maybe make some settings configurable
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        writer.write("<kml xmlns=\"http://www.opengis.net/kml/2.2\" xmlns:gx=\"http://www.google.com/kml/ext/2.2\">\n");
        writer.write("<Document>\n");
        writer.write("<name>" + this.title + "</name>\n");
        writer.write("<description></description>\n");
        writer.write("<Style id=\"" + LINE_STYLE_YELLOW + "\">\n");
        writer.write("<LineStyle>\n");
        writer.write("<color>7f00ffff</color>\n");
        writer.write("<width>4</width>\n");
        writer.write("</LineStyle>\n");
        writer.write("</Style>\n");
        writer.write("<Style id=\"" + LINE_STYLE_BLUE + "\">\n");
        writer.write("<LineStyle>\n");
        writer.write("<color>7fff0000</color>\n");
        writer.write("<width>4</width>\n");
        writer.write("</LineStyle>\n");
        writer.write("</Style>\n");
        writer.write("<Style id=\"" + LINE_STYLE_RED + "\">\n");
        writer.write("<LineStyle>\n");
        writer.write("<color>7f0000ff</color>\n");
        writer.write("<width>4</width>\n");
        writer.write("</LineStyle>\n");
        writer.write("</Style>\n");
    }

    @Override
    protected void writeTrackPartStart(String trackPartName) throws IOException {
        String styleId = getStyleForFlightMode(flightMode);
        writer.write("<Placemark>\n");
        writer.write("<name>" + trackPartName + "</name>\n");
        writer.write("<description></description>\n");
        writer.write("<styleUrl>#" + styleId + "</styleUrl>\n");
        writer.write("<gx:Track id=\"" + trackPartName + "\">\n");
        writer.write("<altitudeMode>absolute</altitudeMode>\n");
        writer.write("<gx:interpolate>0</gx:interpolate>\n");
    }

    protected void writePoint(TrackPoint point) throws IOException {
        writer.write(String.format("<when>%s</when>\n", dateFormatter.format(point.time / 1000)));
        writer.write(String.format(Locale.ROOT, "<gx:coord>%.10f %.10f %.2f</gx:coord>\n", point.lon, point.lat, point.alt));
    }

    protected void writeTrackPartEnd() throws IOException {
        writer.write("</gx:Track>\n");
        writer.write("</Placemark>\n");
    }

    protected void writeEnd() throws IOException {
        writer.write("</Document>\n");
        writer.write("</kml>");
    }

    @Override
    public String getName() {
        return "KML";
    }

    @Override
    public String getDescription() {
        return "Google Earth Track (KML)";
    }

    @Override
    public String getFileExtension() {
        return "kml";
    }
}
