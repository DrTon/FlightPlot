package me.drton.flightplot.processors.tools;

import java.util.LinkedList;
import java.util.List;

/**
 * User: ton Date: 01.07.13 Time: 11:16
 */
public class DelayLine {
    private double delay;
    private List<Tick> buffer = new LinkedList<Tick>();
    double value = Double.NaN;

    public void reset() {
        buffer.clear();
        value = Double.NaN;
    }

    public double getDelay() {
        return delay;
    }

    public void setDelay(double delay) {
        this.delay = delay;
    }

    public double getOutput(double time, double in) {
        buffer.add(new Tick(time, in));
        while (!buffer.isEmpty()) {
            Tick tick = buffer.get(0);
            if (time - tick.time < delay)
                break;
            value = tick.value;
            buffer.remove(0);
        }
        return value;
    }

    private static class Tick {
        public final double time;
        public final double value;

        private Tick(double time, double value) {
            this.time = time;
            this.value = value;
        }
    }
}
