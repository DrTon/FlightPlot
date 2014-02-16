package me.drton.flightplot.export;

import me.drton.flightplot.FormatErrorException;
import me.drton.flightplot.PX4LogReader;

import java.io.EOFException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ada on 23.12.13.
 */
public abstract class AbstractTrackReader implements TrackReader {

    private final PX4LogReader reader;
    private long nextMinTime = 0;
    private long timeGap = 0;
    private ReaderConfiguration configuration = new ReaderConfiguration();

    public AbstractTrackReader(PX4LogReader reader) throws IOException, FormatErrorException {
        this.reader = reader;
        reset();
        initFromConfig();
    }

    public void reset() throws IOException, FormatErrorException {
        reader.seek(0);
    }

    protected long readUpdate(Map<String, Object> data) throws IOException, FormatErrorException {
        long logTime = 0;
        while (true) {
            logTime = reader.readUpdate(data);
            if(logTime >= this.nextMinTime) {
                if(0 == this.nextMinTime){
                    this.nextMinTime = logTime;
                }
                this.nextMinTime += this.timeGap;
                return logTime;
            }
        }
    }

    @Override
    public void setConfiguration(ReaderConfiguration configuration) {
        this.configuration = configuration;
        initFromConfig();
    }

    protected ReaderConfiguration getConfiguration(){
        return this.configuration;
    }

    private void initFromConfig(){
        this.timeGap = (long)Math.floor(1000000 / this.configuration.getSamplesPerSecond());
    }
}
