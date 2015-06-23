package me.drton.flightplot.export;

import me.drton.jmavlib.log.FormatErrorException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by ada on 14.01.14.
 */
public abstract class AbstractTrackExporter implements TrackExporter {

    private final TrackReader trackReader;
    private Set<TrackAnalyzer> analyzers;

    protected ExporterConfiguration configuration = new ExporterConfiguration();
    protected boolean trackStarted;
    protected String title;
    protected Writer writer;
    protected float altOffset;

    public AbstractTrackExporter(TrackReader trackReader) {
        this.trackReader = trackReader;
        initAnalyzers();
    }

    private void initAnalyzers() {
        analyzers = new HashSet<TrackAnalyzer>();
        if (this instanceof FlightModeChangeListener) {
            analyzers.add(new FlightModeReader((FlightModeChangeListener) this));
        }
    }

    protected TrackPoint readNextPoint() throws IOException, FormatErrorException {
        TrackPoint point = this.trackReader.readNextPoint();
        if (null != point) {
            if (altOffset != 0) {
                point = new TrackPoint(point.lat, point.lon, (float)(point.alt + altOffset), point.time);
            }
            feedAnalyzers(point);
        }
        return point;
    }

    private void feedAnalyzers(TrackPoint point) {
        for (TrackAnalyzer analyzer : this.analyzers) {
            analyzer.inputTrackPoint(point);
        }
    }

    public void exportToFile(File file, String title) throws IOException {
        this.writer = initWriter(file, title);
        altOffset = configuration.getAltOffset();
        try {
            writeStart();
            TrackPoint point = readNextPoint();
            if (!this.trackStarted) {
                startTrackPart();
                this.trackStarted = true;
            }
            while (null != point) {
                writePoint(point);
                point = readNextPoint();
            }
            endTrackPart();
            writeEnd();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            this.writer.close();
        }
    }

    protected Writer initWriter(File file, String title) throws IOException {
        Writer fileWriter = new FileWriter(file);
        this.trackStarted = false;
        this.title = title;
        return fileWriter;
    }

    protected abstract void writeStart() throws IOException;

    protected abstract void startTrackPart() throws IOException;

    protected abstract void writePoint(TrackPoint point) throws IOException;

    protected abstract void endTrackPart() throws IOException;

    protected abstract void writeEnd() throws IOException;

    @Override
    public void setConfiguration(ExporterConfiguration configuration) {
        this.configuration = configuration;
    }

    public ExporterConfiguration getConfiguration() {
        return configuration;
    }
}
