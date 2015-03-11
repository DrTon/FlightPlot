package me.drton.flightplot;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ada on 22.12.14.
 */
public class ColorSupplier {
    public Color[] paintSequence;
    public int paintIndex;
    public int fillPaintIndex;

    private Map<String, Color> paintForFields = new HashMap<String, Color>();

    {
        paintSequence = new Color[]{
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

    public Color getNextColor(String field) {
        Color result = paintForFields.get(field);
        if (null == result) {
            result = paintSequence[paintIndex % paintSequence.length];
            paintForFields.put(field, result);
            paintIndex++;
        }
        return result;
    }

    public void updatePaintForField(String field, Color color) {
        paintForFields.put(field, color);
    }
}
