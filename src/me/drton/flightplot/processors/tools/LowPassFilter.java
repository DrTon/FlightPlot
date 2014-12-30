package me.drton.flightplot.processors.tools;

/**
 * User: ton Date: 09.03.13 Time: 15:07
 */
public class LowPassFilter {
    private double inLast = 0.0;
    private double valueFiltered = 0.0;
    private double tLast = Double.NaN;
    private double f = 1.0;
    private double rc_inv = f * 2 * Math.PI;

    public void setF(double f) {
        this.f = f;
        this.rc_inv = f * 2 * Math.PI;
    }

    public void setT(double t) {
        if (t == 0.0) {
            this.f = 0.0;
            this.rc_inv = 0.0;
        } else {
            this.f = 1 / t;
            this.rc_inv = f * 2 * Math.PI;
        }
    }

    public void reset() {
        tLast = Double.NaN;
    }

    public double getOutput(double t, double in) {
        if (rc_inv == 0.0) {
            this.valueFiltered = in;
            return in;
        } else {
            if (Double.isNaN(tLast)) {
                this.tLast = t;
                this.inLast = in;
                this.valueFiltered = in;
                return in;
            } else {
                double dt = t - tLast;
                this.valueFiltered += (1.0 - Math.exp(-dt * rc_inv)) * (inLast - valueFiltered);
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
