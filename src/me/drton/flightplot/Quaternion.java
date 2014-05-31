package me.drton.flightplot;

import org.ejml.simple.SimpleMatrix;

public class Quaternion {
    public final double x0, x1, x2, x3;

    // create a new object with the given components
    public Quaternion(double x0, double x1, double x2, double x3) {
        this.x0 = x0;
        this.x1 = x1;
        this.x2 = x2;
        this.x3 = x3;
    }

    // return a string representation of the invoking object
    public String toString() {
        return x0 + " + " + x1 + "i + " + x2 + "j + " + x3 + "k";
    }

    // return the quaternion norm
    public double norm() {
        return Math.sqrt(x0 * x0 + x1 * x1 + x2 * x2 + x3 * x3);
    }

    // return the quaternion conjugate
    public Quaternion conjugate() {
        return new Quaternion(x0, -x1, -x2, -x3);
    }

    // return a new Quaternion whose value is (this + b)
    public Quaternion plus(Quaternion b) {
        Quaternion a = this;
        return new Quaternion(a.x0 + b.x0, a.x1 + b.x1, a.x2 + b.x2, a.x3 + b.x3);
    }

    // return a new Quaternion whose value is (this * b)
    public Quaternion mul(Quaternion b) {
        Quaternion a = this;
        double y0 = a.x0 * b.x0 - a.x1 * b.x1 - a.x2 * b.x2 - a.x3 * b.x3;
        double y1 = a.x0 * b.x1 + a.x1 * b.x0 + a.x2 * b.x3 - a.x3 * b.x2;
        double y2 = a.x0 * b.x2 - a.x1 * b.x3 + a.x2 * b.x0 + a.x3 * b.x1;
        double y3 = a.x0 * b.x3 + a.x1 * b.x2 - a.x2 * b.x1 + a.x3 * b.x0;
        return new Quaternion(y0, y1, y2, y3);
    }

    // return a new Quaternion whose value is the inverse of this
    public Quaternion inversed() {
        double d = x0 * x0 + x1 * x1 + x2 * x2 + x3 * x3;
        return new Quaternion(x0 / d, -x1 / d, -x2 / d, -x3 / d);
    }

    // return a / b
    public Quaternion div(Quaternion b) {
        Quaternion a = this;
        return a.inversed().mul(b);
    }
    
    public SimpleMatrix dcm() {
        SimpleMatrix R = new SimpleMatrix(3, 3);
        double aSq = x0 * x0;
        double bSq = x1 * x1;
        double cSq = x2 * x2;
        double dSq = x3 * x3;
        R.set(0, 0, aSq + bSq - cSq - dSq);
        R.set(0, 1, 2.0f * (x1 * x2 - x0 * x3));
        R.set(0, 2, 2.0f * (x0 * x2 + x1 * x3));
        R.set(1, 0, 2.0f * (x1 * x2 + x0 * x3));
        R.set(1, 1, aSq - bSq + cSq - dSq);
        R.set(1, 2, 2.0f * (x2 * x3 - x0 * x1));
        R.set(2, 0, 2.0f * (x1 * x3 - x0 * x2));
        R.set(2, 1, 2.0f * (x0 * x1 + x2 * x3));
        R.set(2, 2, aSq - bSq - cSq + dSq);
        return R;
    }

    // sample client for testing
    public static void main(String[] args) {
        Quaternion a = new Quaternion(3.0, 1.0, 0.0, 0.0);
        System.out.println("a = " + a);

        Quaternion b = new Quaternion(0.0, 5.0, 1.0, -2.0);
        System.out.println("b = " + b);

        System.out.println("norm(a)  = " + a.norm());
        System.out.println("conj(a)  = " + a.conjugate());
        System.out.println("a + b    = " + a.plus(b));
        System.out.println("a * b    = " + a.mul(b));
        System.out.println("b * a    = " + b.mul(a));
        System.out.println("a / b    = " + a.div(b));
        System.out.println("a^-1     = " + a.inversed());
        System.out.println("a^-1 * a = " + a.inversed().mul(a));
        System.out.println("a * a^-1 = " + a.mul(a.inversed()));
    }
}
