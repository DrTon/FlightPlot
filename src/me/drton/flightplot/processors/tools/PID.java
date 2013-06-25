package me.drton.flightplot.processors.tools;

/**
 * User: ton Date: 20.06.13 Time: 7:13
 */
public class PID {
    private double kP = 0.0;
    private double kI = 0.0;
    private double kD = 0.0;
    private double limit = 0.0;
    private double integral = 0.0;
    private double errorLast = 0.0;

    public void setK(double kP, double kI, double kD, double limit) {
        this.kP = kP;
        this.kI = kI;
        this.kD = kD;
        this.limit = limit;
    }

    public void reset() {
        errorLast = 0.0;
        integral = 0.0;
    }

    public double getIntegral() {
        return integral;
    }

    public void setIntegral(double integral) {
        this.integral = integral;
    }

    private double limitValue(double value) {
        if (limit == 0.0)
            return value;
        else
            return Math.max(-limit, Math.min(limit, value));
    }

    public double getOutput(double err, double errDer, boolean useDer, double dt) {
        double d = useDer ? errDer : (err - errorLast) / dt;
        errorLast = err;
        double pd = err * kP + d * kD;
        integral += pd / kP * kI * dt;
        integral = limitValue(integral);
        return limitValue(pd + integral);
    }

    public double getOutput(double err, double errDer, boolean useDer, double dt, double awuW) {
        double d = useDer ? errDer : (err - errorLast) / dt;
        errorLast = err;
        double pd = err * kP + d * kD;
        integral += pd / kP * kI * dt * awuW;
        integral = limitValue(integral);
        return limitValue(pd + integral);
    }

    public double getOutputDNoSP(double sp, double v, double dt) {
        double err = sp - v;
        double d = (-v - errorLast) / dt;
        errorLast = -v;
        double pd = err * kP + d * kD;
        integral += err * kI * dt;
        integral = limitValue(integral);
        return limitValue(pd + integral);
    }
}
