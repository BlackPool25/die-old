package com.dieold.game.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

import java.io.File;

/**
 * Resolves asset paths seamlessly whether running from project root, subproject (:core),
 * desktop launcher, or inside the assets folder.
 */
public final class AssetResolver {

    private AssetResolver() {}

    public static FileHandle resolve(String relativePath) {
        // Candidate paths to probe
        String[] candidates = {
            relativePath,
            "../" + relativePath,
            relativePath.startsWith("assets/") ? relativePath.substring(7) : "assets/" + relativePath,
            relativePath.startsWith("assets/") ? "../" + relativePath.substring(7) : "../assets/" + relativePath,
            "../../" + relativePath
        };

        for (String candidate : candidates) {
            File f = new File(candidate);
            if (f.exists()) {
                if (Gdx.files != null) {
                    return Gdx.files.absolute(f.getAbsolutePath());
                } else {
                    return new FileHandle(f);
                }
            }
        }

        // Fallback to internal handle
        return (Gdx.files != null) ? Gdx.files.internal(relativePath) : new FileHandle(relativePath);
    }
}
