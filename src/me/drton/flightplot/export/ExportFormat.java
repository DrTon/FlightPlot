package me.drton.flightplot.export;

/**
 * Created by ada on 25.01.14.
 */
public interface ExportFormat {
    String getFormatName();
    String getFileExtension();
    String getFileExtensionName();
    TrackExporter getTrackExporter(TrackReader trackReader);
}
