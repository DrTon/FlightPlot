package me.drton.flightplot.export;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Created by ada on 16.02.14.
 */
public class GPXTrackExporter extends AbstractTrackExporter {

    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    protected void writeStart() throws IOException {
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        writer.write("<gpx xmlns=\"http://www.topografix.com/GPX/1/1\" version=\"1.1\" creator=\"FlightPlot\" \n" +
                "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n" +
                "    xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">\n");
        writer.write("<metadata>\n");
        writer.write(String.format("<name>%s</name>\n", this.title));
        writer.write("</metadata>\n");
        writer.write("<trk>\n");
    }

    @Override
    protected void writeTrackPartStart(String trackPartName) throws IOException {
        writer.write("<trkseg>\n");
    }

    @Override
    protected void writePoint(TrackPoint point) throws IOException {
        writer.write(String.format(Locale.ROOT, "<trkpt lat=\"%.10f\" lon=\"%.10f\">\n", point.lat, point.lon));
        writer.write(String.format(Locale.ROOT, "<ele>%.2f</ele>\n", point.alt));
        writer.write(String.format("<time>%s</time>\n", dateFormatter.format(point.time / 1000)));
        writer.write("</trkpt>\n");
    }

    protected void writeTrackPartEnd() throws IOException {
        writer.write("</trkseg>\n");
    }

    protected void writeEnd() throws IOException {
        writer.write("</trk>\n");
        writer.write("</gpx>\n");
    }

    @Override
    public String getName() {
        return "GPX";
    }

    @Override
    public String getDescription() {
        return "GPS Exchange Format (GPX)";
    }

    @Override
    public String getFileExtension() {
        return "gpx";
    }
}
