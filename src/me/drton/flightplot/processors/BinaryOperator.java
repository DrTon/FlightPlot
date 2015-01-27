package me.drton.flightplot.processors;

        import me.drton.flightplot.processors.tools.LowPassFilter;

        import java.util.HashMap;
        import java.util.Map;

/**
 * Created by markw on 1/22/15.
 */
public class BinaryOperator extends PlotProcessor {
    protected String param_Field1;
    protected String param_Field2;
    protected String param_Operator;
    protected double param_InScale1;
    protected double param_InScale2;
    protected double param_OutScale;
    protected double param_OutOffset;
    protected LowPassFilter lowPassFilter;

    @Override
    public Map<String, Object> getDefaultParameters() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("InScale1", 1.0);
        params.put("Field1", "BATT.C");
        params.put("Operator", "*");
        params.put("InScale2", 1.0);
        params.put("Field2", "BATT.V");
        params.put("OutScale", 1.0);
        params.put("OutOffset", 0.0);
        params.put("LPF", 0.0);
        return params;
    }

    @Override
    public void init() {
        super.init();
        param_InScale1 = (Double) parameters.get("InScale1");
        param_Field1 = ((String) parameters.get("Field1"));
        param_Operator = ((String) parameters.get("Operator"));
        param_InScale2 = (Double) parameters.get("InScale2");
        param_Field2 = ((String) parameters.get("Field2"));
        param_OutScale = (Double) parameters.get("OutScale");
        param_OutOffset = (Double) parameters.get("OutOffset");
        lowPassFilter = new LowPassFilter();
        lowPassFilter.setF((Double) parameters.get("LPF"));
        addSeries();
    }

    @Override
    public void process(double time, Map<String, Object> update) {
        double s = 0.0;
        Object v1 = update.get(param_Field1);
        Object v2 = update.get(param_Field2);

        if (v1 != null && v1 instanceof Number &&
            v2 != null && v2 instanceof Number) {

            double d1 = ((Number) v1).doubleValue();
            double d2 = ((Number) v2).doubleValue();

            switch (param_Operator.charAt(0)) {
                case '*':
                    s = param_InScale1 * d1 * param_InScale2 * d2;
                    break;
                case '/':
                    s = param_InScale1 * d1 / (param_InScale2 * d2);
                    break;
                case '+':
                    s = param_InScale1 * d1 + param_InScale2 * d2;
                    break;
                case '-':
                    s = param_InScale1 * d1 - param_InScale2 * d2;
                    break;
                default:
            }
        } else {
            return;
        }

        s = lowPassFilter.getOutput(time, s);
        addPoint(0, time, (s * param_OutScale) + param_OutOffset);
    }
}
