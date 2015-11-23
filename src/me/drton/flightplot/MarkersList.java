package me.drton.flightplot;

import me.drton.flightplot.Marker;

import java.util.ArrayList;

/**
 * Created by ton on 29.09.15.
 */
public class MarkersList extends ArrayList<Marker> implements PlotItem {
    private final String title;

    public MarkersList(String title) {
        this.title = title;
    }

    @Override
    public String getTitle() {
        return title;
    }

    public String getFullTitle(String processorTitle) {
        return processorTitle + (title.isEmpty() ? "" : (":" + title));
    }

    public void addMarker(double time, String label) {
        add(new Marker(time, label));
    }
}
