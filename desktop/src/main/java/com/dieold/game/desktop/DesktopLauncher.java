package com.dieold.game.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.dieold.game.DieOldGame;

/**
 * Desktop launcher for Die Old targeting 1080p viewport with LWJGL3 backend.
 */
public class DesktopLauncher {
    public static void main(String[] arg) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Die Old — 2.5D Isometric Combat Engine Playground");
        config.setWindowedMode(1920, 1080);
        config.useVsync(true);
        config.setForegroundFPS(60);
        new Lwjgl3Application(new DieOldGame(), config);
    }
}
