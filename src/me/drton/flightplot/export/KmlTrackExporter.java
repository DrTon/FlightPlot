package me.drton.flightplot.export;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Created by ada on 23.12.13.
 */
public class KmlTrackExporter extends AbstractTrackExporter implements FlightModeChangeListener{

    private static final String LINE_STYLE_YELLOW = "yellow";
    private static final String LINE_STYLE_BLUE = "blue";
    private static final String LINE_STYLE_RED = "red";

    private int nextTrackNumber = 1;
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    public KmlTrackExporter(TrackReader trackReader) {
        super(trackReader);
    }

    @Override
    public void flightModeChanged(FlightMode newFlightMode) {
        try{
            splitTrack(newFlightMode);
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    private void splitTrack(FlightMode newFlightMode) throws IOException {
        if(getConfiguration().isSplitTracksByFlightMode()){
            if(this.trackStarted){
                endTrackPart();
                startTrackPart(determineStyleByFlightMode(newFlightMode));
            }
            else {
                startTrackPart(determineStyleByFlightMode(newFlightMode));
                this.trackStarted = true;
            }
        }
    }

    protected String determineStyleByFlightMode(FlightMode flightMode){
        if(null == flightMode) {
            return LINE_STYLE_YELLOW;
        }

        switch(flightMode){
            case AUTO:
                return LINE_STYLE_RED;
            case STABILIZED:
                return LINE_STYLE_BLUE;
            case MANUAL:
            default:
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

    protected void startTrackPart() throws IOException {
        startTrackPart(LINE_STYLE_YELLOW);
    }

    protected void startTrackPart(String style) throws IOException {
        startTrackPart(style, String.format("Part %d", this.nextTrackNumber));
    }

    protected void startTrackPart(String styleId, String name) throws IOException {
        writer.write("<Placemark>\n");
        writer.write("<name>" + name + "</name>\n");
        writer.write("<description></description>\n");
        writer.write("<styleUrl>#" + styleId + "</styleUrl>\n");
        writer.write("<gx:Track id=\"ID\">\n");
        writer.write("<altitudeMode>absolute</altitudeMode>\n");
        writer.write("<gx:interpolate>0</gx:interpolate>\n");
        this.nextTrackNumber++;
    }

    protected void writePoint(TrackPoint point) throws IOException {
        writer.write(String.format("<when>%s</when>\n", dateFormatter.format(point.time)));
        writer.write(
                String.format(Locale.ROOT, "<gx:coord>%.10f %.10f %.0f</gx:coord>\n", point.lon, point.lat, point.alt));
    }

    protected void endTrackPart() throws IOException{
        writer.write("</gx:Track>\n");
        writer.write("</Placemark>\n");
    }

    protected void writeEnd() throws IOException{
        writer.write("</Document>\n");
        writer.write("</kml>");
    }


}
