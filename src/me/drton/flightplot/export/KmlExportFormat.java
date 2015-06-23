package me.drton.flightplot.export;

/**
 * Created by ada on 25.01.14.
 */
public class KmlExportFormat implements ExportFormat {
    @Override
    public String getFormatName() {
        return "Google Earth Track (KML)";
    }

    @Override
    public String getFileExtension() {
        return "kml";
    }

    @Override
    public String getFileExtensionName() { return "KML"; }

    @Override
    public TrackExporter getTrackExporter(TrackReader trackReader) {
        return new KmlTrackExporter(trackReader);
    }
}
