package me.drton.flightplot.export;

/**
 * Generalized flight mode
 * <p/>
 * Note: From the exports point of view it's a good idea to generalize the flight mode. However, this currently results
 * in loosing hardware specific flight modes.
 *
 * @author ada
 */
public enum FlightMode {
    MANUAL, AUTO, STABILIZED
}
