package com.dieold.game;

import com.badlogic.gdx.Game;
import com.dieold.game.screens.PlaygroundScreen;

/**
 * Main LibGDX Game Application Entrypoint.
 */
public class DieOldGame extends Game {

    @Override
    public void create() {
        setScreen(new PlaygroundScreen());
    }

    @Override
    public void dispose() {
        super.dispose();
        if (screen != null) {
            screen.dispose();
        }
    }
}
