package me.drton.flightplot;

import org.jfree.chart.plot.ValueMarker;

/**
 * Created by ton on 29.09.15.
 */
public class TaggedValueMarker extends ValueMarker {
    public final int tag;

    public TaggedValueMarker(int tag, double value) {
        super(value);
        this.tag = tag;
    }
}
