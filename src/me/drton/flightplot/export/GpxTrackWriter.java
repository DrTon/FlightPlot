package me.drton.flightplot.export;

import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Created by ada on 23.12.13.
 */
public class GpxTrackWriter {

    private final Writer writer;
    private final String title;
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    public GpxTrackWriter(Writer writer, String title) {
        this.writer = writer;
        this.title = title;
    }

    public void writeStart() throws IOException {
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        writer.write("<gpx xmlns=\"http://www.topografix.com/GPX/1/1\" version=\"1.1\" creator=\"FlightPlot\" \n" +
                "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n" +
                "    xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">\n");
        writer.write("<metadata>\n");
        writer.write(String.format("<name>%s</name>\n", this.title));
        writer.write("</metadata>\n");
        writer.write("<trk>\n");
    }

    public void startTrackPart() throws IOException {
        writer.write("<trkseg>\n");
    }

    public void writePoint(TrackPoint point) throws IOException {
        writer.write(String.format(Locale.ROOT, "<trkpt lat=\"%.10f\" lon=\"%.10f\">\n", point.lat, point.lon));
        writer.write(String.format(Locale.ROOT, "<ele>%.0f</ele>\n", point.alt));
        writer.write(String.format("<time>%s</time>\n", dateFormatter.format(point.time)));
        writer.write("</trkpt>\n");
    }

    public void endTrackPart() throws IOException{
        writer.write("</trkseg>\n");
    }

    public void writeEnd() throws IOException{
        writer.write("</trk>\n");
        writer.write("</gpx>\n");
    }

}
