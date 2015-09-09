package me.drton.flightplot.export;

import me.drton.jmavlib.log.FormatErrorException;
import me.drton.jmavlib.log.LogReader;

import java.io.EOFException;
import java.io.IOException;
import java.util.Map;

/**
 * Created by ada on 23.12.13.
 */
public abstract class AbstractTrackReader implements TrackReader {
    protected final LogReader reader;
    private long timeNext = 0;
    protected final TrackReaderConfiguration config;

    public AbstractTrackReader(LogReader reader, TrackReaderConfiguration config) throws IOException, FormatErrorException {
        this.reader = reader;
        this.config = config;
        this.reader.seek(this.config.getTimeStart());
    }

    protected long readUpdate(Map<String, Object> data) throws IOException, FormatErrorException {
        long t;
        while (true) {
            t = reader.readUpdate(data);
            if (t > config.getTimeEnd()) {
                throw new EOFException("Reached configured export limit.");
            }
            if (t >= timeNext) {
                if (timeNext == 0) {
                    timeNext = t;
                }
                timeNext += config.getTimeInterval();
                return t;
            }
        }
    }
}
