package me.drton.flightplot.export;

import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Created by ada on 23.12.13.
 */
public class KmlTrackExportWriter {
    public static final String LINE_STYLE_YELLOW = "yellow";
    public static final String LINE_STYLE_BLUE = "blue";
    public static final String LINE_STYLE_RED = "red";

    private final Writer writer;
    private final String title;
    private int nextTrackNumber = 1;
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    public KmlTrackExportWriter(Writer writer, String title) {
        this.writer = writer;
        this.title = title;
    }

    public void writeStart() throws IOException {
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

    public void startTrackPart() throws IOException {
        startTrackPart(LINE_STYLE_YELLOW);
    }

    public void startTrackPart(String style) throws IOException {
        startTrackPart(style, String.format("Part %d", this.nextTrackNumber));
    }

    public void startTrackPart(String styleId, String name) throws IOException {
        writer.write("<Placemark>\n");
        writer.write("<name>" + name + "</name>\n");
        writer.write("<description></description>\n");
        writer.write("<styleUrl>#" + styleId + "</styleUrl>\n");
        writer.write("<gx:Track id=\"ID\">\n");
        writer.write("<altitudeMode>absolute</altitudeMode>\n");
        writer.write("<gx:interpolate>0</gx:interpolate>\n");
        this.nextTrackNumber++;
    }

    public void writePoint(TrackPoint point) throws IOException {
        writer.write(String.format("<when>%s</when>\n", dateFormatter.format(point.time)));
        writer.write(
                String.format(Locale.ROOT, "<gx:coord>%.10f %.10f %.0f</gx:coord>\n", point.lon, point.lat, point.alt));
    }

    public void endTrackPart() throws IOException{
        writer.write("</gx:Track>\n");
        writer.write("</Placemark>\n");
    }

    public void writeEnd() throws IOException{

        writer.write("</Document>\n");
        writer.write("</kml>");
    }
}
