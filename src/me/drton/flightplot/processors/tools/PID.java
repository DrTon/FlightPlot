package me.drton.flightplot.processors.tools;

/**
 * User: ton Date: 20.06.13 Time: 7:13
 */
public class PID {
    private double kP = 0.0;
    private double kI = 0.0;
    private double kD = 0.0;
    private double errorLast = 0.0;

    public PID() {
    }

    public void setK(double kP, double kI, double kD) {
        this.kP = kP;
        this.kI = kI;
        this.kD = kD;
    }

    public void reset() {
        errorLast = 0.0;
    }

    public double getOutput(double err, double errDer, boolean useDer, double dt) {
        double d = useDer ? errDer : (err - errorLast) / dt;
        errorLast = err;
        return err * kP + d * kD;
    }

    public double getOutputDNoSP(double sp, double v, double dt) {
        double err = sp - v;
        double d = (-v - errorLast) / dt;
        errorLast = -v;
        return err * kP + d * kD;
    }
}
