package me.drton.flightplot.export;

import me.drton.flightplot.LogReader;

/**
 * Created by ada on 04.02.14.
 */
public class ExportData {
    private long chartRangeFrom;
    private long chartRangeTo;
    private LogReader logReader;

    public long getChartRangeToInSeconds() {
        return (long) Math.min(
                Math.ceil((chartRangeTo - this.logReader.getStartMicroseconds()) / 1000000)
                , getLogSizeInSeconds());
    }

    public long getChartRangeFromInSeconds() {
        return (long) Math.max(
                Math.ceil((chartRangeFrom - this.logReader.getStartMicroseconds()) / 1000000)
                , 0);
    }

    public long getLogSizeInSeconds() {
        return (long) Math.ceil(this.logReader.getSizeMicroseconds() / 1000000);
    }

    public void setChartRangeFrom(long chartRangeFrom) {
        this.chartRangeFrom = chartRangeFrom;
    }

    public void setChartRangeTo(long chartRangeTo) {
        this.chartRangeTo = chartRangeTo;
    }

    public LogReader getLogReader() {
        return logReader;
    }

    public void setLogReader(LogReader logReader) {
        this.logReader = logReader;
    }
}
