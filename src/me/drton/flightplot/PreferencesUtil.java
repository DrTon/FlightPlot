package me.drton.flightplot;

import java.awt.*;
import java.util.prefs.Preferences;

/**
 * Created by ada on 25.01.14.
 */
public class PreferencesUtil {

    public static void saveWindowPreferences(Component window, Preferences windowPreferences) {
        Dimension size = window.getSize();
        windowPreferences.putInt("Width", size.width);
        windowPreferences.putInt("Height", size.height);
        Point location = window.getLocation();
        windowPreferences.putInt("X", location.x);
        windowPreferences.putInt("Y", location.y);
    }

    public static void loadWindowPreferences(Component window, Preferences windowPreferences, int defaultWidth,
                                      int defaultHeight) {
        if (defaultWidth > 0) {
            window.setSize(windowPreferences.getInt("Width", defaultWidth),
                    windowPreferences.getInt("Height", defaultHeight));
        }
        window.setLocation(windowPreferences.getInt("X", 0), windowPreferences.getInt("Y", 0));
    }
}
