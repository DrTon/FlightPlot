package me.drton.flightplot.export;

/**
 * Created by ada on 25.01.14.
 */
public class GpxExportFormat implements ExportFormat {
    @Override
    public String getFormatName() {
        return "GPS Exchange Format (GPX)";
    }

    @Override
    public String getFileExtension() {
        return ".gpx";
    }

    @Override
    public String getFileExtensionName() {
        return "GPX";
    }

    @Override
    public TrackExporter getTrackExporter(TrackReader trackReader) {
        return new GpxTrackExporter(trackReader);
    }
}
