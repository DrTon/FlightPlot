package me.drton.flightplot.processors.tools;

/**
 * User: ton Date: 20.06.13 Time: 7:13
 */
public class PID {
    public static enum MODE {
        DERIVATIVE_SET,
        DERIVATIVE_CALC,
        DERIVATIVE_CALC_NO_SP,
    }

    private double kP = 0.0;
    private double kI = 0.0;
    private double kD = 0.0;
    private double limit = 0.0;
    private double integral = 0.0;
    private double errorLast = 0.0;
    private MODE mode = MODE.DERIVATIVE_CALC;

    public void setK(double kP, double kI, double kD, double limit, MODE mode) {
        this.kP = kP;
        this.kI = kI;
        this.kD = kD;
        this.limit = limit;
        this.mode = mode;
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

    public double getOutput(double err, double dt) {
        if (mode != MODE.DERIVATIVE_CALC)
            throw new RuntimeException("Can't use this method in mode " + mode);
        double d = (err - errorLast) / dt;
        errorLast = err;
        double pd = err * kP + d * kD;
        integral += pd / kP * kI * dt;
        integral = limitValue(integral);
        return limitValue(pd + integral);
    }

    public double getOutput(double err, double derivative, double dt) {
        if (mode != MODE.DERIVATIVE_SET)
            throw new RuntimeException("Can't use this method in mode " + mode);
        errorLast = err;
        double pd = err * kP + derivative * kD;
        integral += pd / kP * kI * dt;
        integral = limitValue(integral);
        return limitValue(pd + integral);
    }

    public double getOutput(double sp, double value, double derivative, double dt, double awuW) {
        double err = sp - value;
        double d;
        if (mode == MODE.DERIVATIVE_SET) {
            d = derivative;
            errorLast = err;
        } else if (mode == MODE.DERIVATIVE_CALC) {
            d = (err - errorLast) / dt;
            errorLast = err;
        } else if (mode == MODE.DERIVATIVE_CALC_NO_SP) {
            d = (-value - errorLast) / dt;
            errorLast = -value;
        } else {
            d = 0.0;
            errorLast = 0.0;
        }
        double pd = err * kP + d * kD;
        double i = integral + err * kI * dt * awuW;
        if (limit == 0.0 || (Math.abs(i) < limit && Math.abs(pd + i) < limit))
            integral = limitValue(i);
        return limitValue(pd + integral);
    }
}
