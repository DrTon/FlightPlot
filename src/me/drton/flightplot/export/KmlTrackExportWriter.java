package me.drton.flightplot.export;

import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Created by ada on 23.12.13.
 */
public class KmlTrackExportWriter {
    private final Writer writer;
    private final String title;

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
        writer.write("<name>Tracks</name>\n");
        writer.write("<description></description>\n");
        writer.write("<Style id=\"default\">\n");
        writer.write("<LineStyle>\n");
        writer.write("<color>7f00ffff</color>\n");
        writer.write("<width>4</width>\n");
        writer.write("</LineStyle>\n");
        writer.write("<PolyStyle>\n");
        writer.write("<color>7f00ff00</color>\n");
        writer.write("</PolyStyle>\n");
        writer.write("</Style>\n");
        writer.write("<Placemark>\n");
        writer.write("<name>" + title + "</name>\n");
        writer.write("<description></description>\n");
        writer.write("<styleUrl>#default</styleUrl>\n");
        writer.write("<gx:Track id=\"ID\">\n");
        writer.write("<altitudeMode>absolute</altitudeMode>\n");
    }

    public void writePoint(TrackPoint point) throws IOException {
        writer.write(String.format("<when>%s</when>\n", dateFormatter.format(point.time)));
        writer.write(
                String.format(Locale.ROOT, "<gx:coord>%.10f %.10f %.0f</gx:coord>\n", point.lon, point.lat, point.alt));
    }

    public void writeEnd() throws IOException {
        writer.write("</gx:Track>\n");
        writer.write("</Placemark>\n");
        writer.write("</Document>\n");
        writer.write("</kml>");
    }
}
