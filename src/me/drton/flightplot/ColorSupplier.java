package me.drton.flightplot;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Created by ada on 22.12.14.
 */
public class ColorSupplier {
    public Paint[] paintSequence;
    public int paintIndex;
    public int fillPaintIndex;

    private Map<String, Paint> paintForFields = new HashMap<String, Paint>();

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

    public Paint getNextPaint(String field) {
        Paint result = paintForFields.get(field);
        if (null == result) {
            result = paintSequence[paintIndex % paintSequence.length];
            paintForFields.put(field, result);
            paintIndex++;
        }
        return result;
    }

    public Paint getNextFillPaint(String field) {
        Paint result = paintForFields.get(field);
        if (null == result) {
            result = paintSequence[fillPaintIndex % paintSequence.length];
            paintForFields.put(field, result);
            fillPaintIndex++;
        }
        return result;
    }

    public void updatePaintForField(String field, Paint paint) {
        paintForFields.put(field, paint);
    }

    public void savePaintForFields(Preferences prefs) {
        for(Map.Entry<String, Paint> entry : paintForFields.entrySet()) {
            if(entry.getValue() instanceof Color) {
                prefs.put(entry.getKey(), String.valueOf(((Color) entry.getValue()).getRGB()));
            }
        }
    }

    public void loadPaintForFields (Preferences prefs) throws BackingStoreException {
        for(String key : prefs.keys()) {
            int rgb = prefs.getInt(key, Color.RED.getRGB());
            paintForFields.put(key, new Color(rgb));
        }
    }
}
