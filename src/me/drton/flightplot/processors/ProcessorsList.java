package me.drton.flightplot.processors;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * User: ton Date: 15.06.13 Time: 12:21
 */
public class ProcessorsList {
    private Map<String, Class<? extends PlotProcessor>> processors
            = new HashMap<String, Class<? extends PlotProcessor>>();

    public ProcessorsList() throws InstantiationException, IllegalAccessException {
        addProcessorClass(Simple.class);
        addProcessorClass(Derivative.class);
        addProcessorClass(Abs.class);
        addProcessorClass(ATan2.class);
        addProcessorClass(PosPIDControlSimulator.class);
        addProcessorClass(PosRatePIDControlSimulator.class);
        addProcessorClass(PositionEstimator.class);
        addProcessorClass(GlobalPositionProjection.class);
        addProcessorClass(LandDetector.class);
        addProcessorClass(BinaryOperator.class);
        addProcessorClass(NEDFromBodyProjection.class);
        addProcessorClass(Integral.class);
        addProcessorClass(Battery.class);
        addProcessorClass(PositionEstimatorKF.class);
        addProcessorClass(EulerFromQuaternion.class);
    }

    private void addProcessorClass(Class<? extends PlotProcessor> processorClass) {
        processors.put(processorClass.getSimpleName(), processorClass);
    }

    public Set<String> getProcessorsList() {
        return processors.keySet();
    }

    public PlotProcessor getProcessorInstance(String processorName)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Class<? extends PlotProcessor> procClass = processors.get(processorName);
        if (procClass != null) {
            return procClass.newInstance();
        } else {
            return null;
        }
    }
}
