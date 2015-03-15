package me.drton.flightplot;

import java.awt.*;

/**
 * Created by ada on 22.12.14.
 */
public class ColorSupplier {
    private Color[] paintSequence;
    private int[] paintUsage;

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

        paintUsage = new int[paintSequence.length];
    }

    public Color[] getPaintSequence() {
        return paintSequence;
    }

    public Color getPaint(int idx) {
        return paintSequence[idx];
    }

    public void resetColorsUsed() {
        for (int i = 0; i < paintSequence.length; i++) {
            paintUsage[i] = 0;
        }
    }

    public void markColorUsed(Color color) {
        for (int i = 0; i < paintSequence.length; i++) {
            if (color.equals(paintSequence[i])) {
                markColorUsed(i);
            }
        }
    }

    public void markColorUsed(int color_idx) {
        paintUsage[color_idx]++;
    }

    /**
     * Select color with minimal usage
     *
     * @param field
     * @return
     */
    public Color getNextColor(String field) {
        int minUsage = -1;
        int color_idx = 0;
        for (int i = 0; i < paintSequence.length; i++) {
            if (paintUsage[i] < minUsage || minUsage < 0) {
                minUsage = paintUsage[i];
                color_idx = i;
            }
            if (minUsage == 0) {
                markColorUsed(color_idx);
                break;
            }
        }
        return paintSequence[color_idx];
    }
}
