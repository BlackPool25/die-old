package com.dieold.game.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;

/**
 * 2.5D Foreground Wall Cutaway Shader:
 * Applies an ordered 4x4 Bayer dithering matrix to dissolve a circular aperture
 * in tall foreground walls and basalt columns when combatants move behind them,
 * preserving 100% visual clarity of the player and enemies.
 */
public class CutawayShader implements Disposable {

    public static final String VERTEX_SHADER =
        """
        attribute vec4 a_position;
        attribute vec4 a_color;
        attribute vec2 a_texCoord0;

        uniform mat4 u_projTrans;

        varying vec4 v_color;
        varying vec2 v_texCoords;
        varying vec2 v_worldPos;

        void main() {
            v_color = a_color;
            v_texCoords = a_texCoord0;
            v_worldPos = a_position.xy;
            gl_Position = u_projTrans * a_position;
        }
        """;

    public static final String FRAGMENT_SHADER =
        """
        #ifdef GL_ES
        precision mediump float;
        #endif

        varying vec4 v_color;
        varying vec2 v_texCoords;
        varying vec2 v_worldPos;

        uniform sampler2D u_texture;
        uniform vec2 u_targetPos;
        uniform float u_apertureRadius;
        uniform float u_cutawayActive;

        float getBayer4x4(vec2 screenPos) {
            int x = int(mod(screenPos.x, 4.0));
            int y = int(mod(screenPos.y, 4.0));
            int index = y * 4 + x;

            if (index == 0) return 0.0 / 16.0;
            if (index == 1) return 8.0 / 16.0;
            if (index == 2) return 2.0 / 16.0;
            if (index == 3) return 10.0 / 16.0;
            if (index == 4) return 12.0 / 16.0;
            if (index == 5) return 4.0 / 16.0;
            if (index == 6) return 14.0 / 16.0;
            if (index == 7) return 6.0 / 16.0;
            if (index == 8) return 3.0 / 16.0;
            if (index == 9) return 11.0 / 16.0;
            if (index == 10) return 1.0 / 16.0;
            if (index == 11) return 9.0 / 16.0;
            if (index == 12) return 15.0 / 16.0;
            if (index == 13) return 7.0 / 16.0;
            if (index == 14) return 13.0 / 16.0;
            return 5.0 / 16.0;
        }

        void main() {
            vec4 texColor = texture2D(u_texture, v_texCoords);
            if (texColor.a < 0.01) discard;

            if (u_cutawayActive > 0.5) {
                float dist = distance(v_worldPos, u_targetPos);
                if (dist < u_apertureRadius) {
                    float dissolveFactor = 1.0 - (dist / u_apertureRadius);
                    float dither = getBayer4x4(gl_FragCoord.xy);
                    if (dither < dissolveFactor) {
                        discard;
                    }
                }
            }

            gl_FragColor = texColor * v_color;
        }
        """;

    private ShaderProgram shaderProgram;
    private boolean compiled = false;

    public CutawayShader() {
        initShader();
    }

    private void initShader() {
        try {
            if (Gdx.gl != null) {
                ShaderProgram.pedantic = false;
                shaderProgram = new ShaderProgram(VERTEX_SHADER, FRAGMENT_SHADER);
                if (shaderProgram.isCompiled()) {
                    compiled = true;
                } else {
                    if (Gdx.app != null) {
                        Gdx.app.error("CutawayShader", "Shader compilation failed:\n" + shaderProgram.getLog());
                    }
                }
            }
        } catch (Throwable t) {
            // Headless mock environment fallback
            compiled = false;
        }
    }

    public boolean isCompiled() {
        return compiled;
    }

    public ShaderProgram getShaderProgram() {
        return shaderProgram;
    }

    public void bindAperture(float targetScreenX, float targetScreenY, float apertureRadius, boolean active) {
        if (!compiled || shaderProgram == null) return;

        shaderProgram.setUniformf("u_targetPos", targetScreenX, targetScreenY);
        shaderProgram.setUniformf("u_apertureRadius", apertureRadius);
        shaderProgram.setUniformf("u_cutawayActive", active ? 1.0f : 0.0f);
    }

    @Override
    public void dispose() {
        if (shaderProgram != null) {
            shaderProgram.dispose();
            shaderProgram = null;
            compiled = false;
        }
    }
}
