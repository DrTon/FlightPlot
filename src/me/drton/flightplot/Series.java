package me.drton.flightplot;

import java.awt.*;
import java.util.ArrayList;

/**
 * Created by ton on 09.03.15.
 */
public class Series extends ArrayList<XYPoint> {
    private final String title;
    private final double skipOut;
    private Color color = null;
    private Double lastTime = null;
    private Double lastValue = null;

    public Series(String title, double skipOut) {
        this.title = title;
        this.skipOut = skipOut;
    }

    public String getTitle() {
        return title;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }


    public void addPoint(double time, double value) {
        if (lastTime != null && time - lastTime < skipOut) {
            lastValue = value;
            return;
        }
        if (lastValue != null && lastTime != null && time - lastTime > skipOut * 2) {
            add(new XYPoint(lastTime, lastValue));
        }
        lastTime = time;
        lastValue = null;
        add(new XYPoint(time, value));
    }
}
