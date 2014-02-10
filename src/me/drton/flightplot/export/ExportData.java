package me.drton.flightplot.export;

import me.drton.flightplot.LogReader;

/**
 * Created by ada on 04.02.14.
 */
public class ExportData {
    private long chartRangeFrom;
    private long chartRangeTo;
    private LogReader logReader;

    public long getChartRangeFrom() {
        return chartRangeFrom;
    }

    public void setChartRangeFrom(long chartRangeFrom) {
        this.chartRangeFrom = chartRangeFrom;
    }

    public long getChartRangeTo() {
        return chartRangeTo;
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
