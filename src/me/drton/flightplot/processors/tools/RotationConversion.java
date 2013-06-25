package me.drton.flightplot.processors.tools;

import org.ejml.simple.SimpleMatrix;

import java.io.IOException;

import static java.lang.Math.cos;
import static java.lang.Math.sin;

/**
 * User: ton Date: 02.06.13 Time: 20:20
 */
public class RotationConversion {
    public static SimpleMatrix rotationMatrixByEulerAngles(double roll, double pitch, double yaw) {
        SimpleMatrix R = new SimpleMatrix(3, 3);
        R.set(0, 0, cos(pitch) * cos(yaw));
        R.set(0, 1, sin(roll) * sin(pitch) * cos(yaw) - cos(roll) * sin(yaw));
        R.set(0, 2, cos(roll) * sin(pitch) * cos(yaw) + sin(roll) * sin(yaw));
        R.set(1, 0, cos(pitch) * sin(yaw));
        R.set(1, 1, sin(roll) * sin(pitch) * sin(yaw) + cos(roll) * cos(yaw));
        R.set(1, 2, cos(roll) * sin(pitch) * sin(yaw) - sin(roll) * cos(yaw));
        R.set(2, 0, -sin(pitch));
        R.set(2, 1, sin(roll) * cos(pitch));
        R.set(2, 2, cos(roll) * cos(pitch));
        return R;
    }
}
