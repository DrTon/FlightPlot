package me.drton.flightplot.export;

import me.drton.flightplot.FormatErrorException;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by ada on 14.01.14.
 */
public abstract class AbstractTrackExporter {

    private final TrackReader trackReader;

    private Set<TrackAnalyzer> analyzers;

    public AbstractTrackExporter(TrackReader trackReader) {
        this.trackReader = trackReader;
        initAnalyzers();
    }

    private void initAnalyzers(){
        analyzers = new HashSet<TrackAnalyzer>();
        if(this instanceof FlightModeChangeListener){
            analyzers.add(new FlightModeReader((FlightModeChangeListener)this));
        }
    }

    protected TrackPoint readNextPoint() throws IOException, FormatErrorException{
        TrackPoint point = this.trackReader.readNextPoint();
        if(null != point){
            feedAnalyzers(point);
        }
        return point;
    }

    private void feedAnalyzers(TrackPoint point){
        for (TrackAnalyzer analyzer : this.analyzers){
            analyzer.inputTrackPoint(point);
        }
    }

}
