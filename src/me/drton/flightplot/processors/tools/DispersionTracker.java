package me.drton.flightplot.processors.tools;

/**
 * Created by ton on 22.04.17.
 */
public class DispersionTracker {
    private HighPassFilter hpf = new HighPassFilter();
    private LowPassFilter lpf = new LowPassFilter();
    private LowPassFilter lpfDisp = new LowPassFilter();

    public void setCutoffFreq(double hpfFreq, double lpfFreq) {
        hpf.setF(hpfFreq);
        lpf.setF(lpfFreq);
        lpfDisp.setF(hpfFreq);
    }

    public double getOutput(double t, double in) {
        double filtered = lpf.getOutput(t, hpf.getOutput(t, in));
        return lpfDisp.getOutput(t, filtered * filtered);
    }
}
