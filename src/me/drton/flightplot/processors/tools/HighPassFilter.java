package me.drton.flightplot.processors.tools;

/**
 * User: ton Date: 09.03.13 Time: 15:07
 */
public class HighPassFilter {
    private double inLast = 0.0;
    private double valueFiltered = 0.0;
    private double tLast = Double.NaN;
    private double f = 1.0;
    private double rc = 1 / (f * 2 * Math.PI);

    public void setF(double f) {
        this.f = f;
        if (f == 0.0) {
            this.rc = 0;
        } else {
            this.rc = 1 / (f * 2 * Math.PI);
        }
    }

    public void setT(double t) {
        this.rc = t / (2 * Math.PI);
        if (t == 0.0) {
            this.f = 0.0;
        } else {
            this.f = 1 / t;
        }
    }

    public void reset() {
        tLast = Double.NaN;
    }

    public double getOutput(double t, double in) {
        if (rc == 0.0) {
            this.valueFiltered = 0;
            return in;
        } else {
            if (Double.isNaN(tLast)) {
                this.tLast = t;
                this.inLast = in;
                this.valueFiltered = 0;
                return 0;
            } else {
                double dt = t - tLast;
                this.valueFiltered = rc / (rc + dt) * (valueFiltered + in - inLast);
                this.inLast = in;
                this.tLast = t;
                return valueFiltered;
            }
        }
    }

    public void setInput(double in) {
        this.inLast = in;
    }
}
