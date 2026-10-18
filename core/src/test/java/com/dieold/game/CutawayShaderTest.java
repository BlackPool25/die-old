package com.dieold.game;

import com.dieold.game.render.CutawayShader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Headless test validating the 4x4 Bayer dithered cutaway shader source, matrix values, and lifecycle.
 */
public class CutawayShaderTest {

    @Test
    @DisplayName("Verify Cutaway GLSL shaders contain 4x4 Bayer dithering matrix and required uniforms")
    public void testCutawayShaderGLSLIntegrity() {
        assertNotNull(CutawayShader.VERTEX_SHADER);
        assertNotNull(CutawayShader.FRAGMENT_SHADER);

        // Verify vertex shader attributes
        assertTrue(CutawayShader.VERTEX_SHADER.contains("a_position"));
        assertTrue(CutawayShader.VERTEX_SHADER.contains("a_color"));
        assertTrue(CutawayShader.VERTEX_SHADER.contains("a_texCoord0"));
        assertTrue(CutawayShader.VERTEX_SHADER.contains("u_projTrans"));

        // Verify fragment shader uniforms and Bayer dithering implementation
        assertTrue(CutawayShader.FRAGMENT_SHADER.contains("u_targetPos"));
        assertTrue(CutawayShader.FRAGMENT_SHADER.contains("u_apertureRadius"));
        assertTrue(CutawayShader.FRAGMENT_SHADER.contains("u_cutawayActive"));
        assertTrue(CutawayShader.FRAGMENT_SHADER.contains("getBayer4x4"));
        assertTrue(CutawayShader.FRAGMENT_SHADER.contains("gl_FragCoord"));
        assertTrue(CutawayShader.FRAGMENT_SHADER.contains("discard"));

        // Verify Bayer matrix values (0..15 normalized by 16.0)
        assertTrue(CutawayShader.FRAGMENT_SHADER.contains("0.0 / 16.0"));
        assertTrue(CutawayShader.FRAGMENT_SHADER.contains("8.0 / 16.0"));
        assertTrue(CutawayShader.FRAGMENT_SHADER.contains("15.0 / 16.0"));

        // Verify lifecycle safety in headless environment
        CutawayShader shader = new CutawayShader();
        assertNotNull(shader);
        // In headless mode without GL context, compilation gracefully evaluates to false
        assertDoesNotThrow(shader::dispose);
    }
}
