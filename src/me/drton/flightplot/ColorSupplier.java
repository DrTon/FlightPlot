package me.drton.flightplot;

import java.awt.*;

/**
 * Created by ada on 22.12.14.
 */
public class ColorSupplier {
    public Paint[] paintSequence;
    public int paintIndex;
    public int fillPaintIndex;

    {
        paintSequence = new Paint[]{
                Color.RED,
                Color.GREEN,
                Color.BLUE,
                Color.CYAN,
                Color.MAGENTA,
                Color.BLACK,
                Color.LIGHT_GRAY,
                Color.ORANGE,
                Color.RED.darker(),
                Color.GREEN.darker(),
                Color.BLUE.darker(),
                Color.CYAN.darker(),
                Color.MAGENTA.darker(),
                Color.ORANGE.darker(),
        };
    }

    public Paint getNextPaint() {
        Paint result = paintSequence[paintIndex % paintSequence.length];
        paintIndex++;
        return result;
    }

    public Paint getNextFillPaint() {
        Paint result = paintSequence[fillPaintIndex % paintSequence.length];
        fillPaintIndex++;
        return result;
    }
}
