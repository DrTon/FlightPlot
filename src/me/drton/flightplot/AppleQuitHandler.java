package me.drton.flightplot;

/**
 * User: ton Date: 22.06.13 Time: 17:51
 */
public class AppleQuitHandler {
    public AppleQuitHandler(final Runnable callback) {
        new com.apple.eawt.Application().setQuitHandler(new com.apple.eawt.QuitHandler() {
            @Override
            public void handleQuitRequestWith(com.apple.eawt.AppEvent.QuitEvent quitEvent,
                                              com.apple.eawt.QuitResponse quitResponse) {
                callback.run();
            }
        });
    }
}
