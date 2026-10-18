package com.dieold.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.dieold.game.render.IsometricProjection;
import com.dieold.game.util.AssetResolver;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validates genuine production atlases, region indexing, and isometric projection math.
 */
public class AssetAndRenderPipelineTest {

    @BeforeAll
    public static void initHeadlessGdx() {
        if (Gdx.app == null) {
            HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
            new HeadlessApplication(new ApplicationAdapter() {}, config);

            GL20 glMock = (GL20) Proxy.newProxyInstance(
                GL20.class.getClassLoader(),
                new Class<?>[]{GL20.class},
                (proxy, method, args) -> {
                    Class<?> returnType = method.getReturnType();
                    if (returnType == boolean.class) return false;
                    if (returnType == int.class) return 0;
                    if (returnType == float.class) return 0f;
                    return null;
                }
            );
            Gdx.gl = glMock;
            Gdx.gl20 = glMock;
        }
    }

    @Test
    @DisplayName("Verify IsometricProjection world-to-screen and screen-to-world roundtrip")
    public void testIsometricProjectionRoundtrip() {
        Vector2 screen = new Vector2();
        Vector2 worldOut = new Vector2();

        float[] testPoints = {0f, 0f, 5.5f, 3.2f, -4.0f, 7.8f, 16.0f, 16.0f};
        for (int i = 0; i < testPoints.length; i += 2) {
            float wx = testPoints[i];
            float wy = testPoints[i + 1];

            IsometricProjection.worldToScreen(wx, wy, 0f, screen);
            IsometricProjection.screenToWorld(screen.x, screen.y, worldOut);

            assertEquals(wx, worldOut.x, 1e-3, "Roundtrip X failed for point (" + wx + ", " + wy + ")");
            assertEquals(wy, worldOut.y, 1e-3, "Roundtrip Y failed for point (" + wx + ", " + wy + ")");
        }
    }

    @Test
    @DisplayName("Verify Characters Atlas contains all 8 directions for Player Youth actions")
    public void testPlayerYouthAtlasRegions() {
        TextureAtlas atlas = new TextureAtlas(AssetResolver.resolve("assets/atlases/characters.atlas"));
        String[] actions = {"idle", "sprint", "deflect_stance", "attack_light_combo", "hit_react", "death"};

        for (String action : actions) {
            for (int dir = 0; dir < 8; dir++) {
                String key = "player_youth_" + action + "_dir" + dir;
                Array<AtlasRegion> regions = atlas.findRegions(key);
                assertNotNull(regions, "Region key should not be null: " + key);
                assertTrue(regions.size > 0, "Missing frames for player action: " + key);
            }
        }
        atlas.dispose();
    }

    @Test
    @DisplayName("Verify Characters Atlas contains all 8 directions for Foundry Hound actions")
    public void testFoundryHoundAtlasRegions() {
        TextureAtlas atlas = new TextureAtlas(AssetResolver.resolve("assets/atlases/characters.atlas"));
        String[] actions = {"idle", "trot", "pounce_telegraph", "hit_react", "death"};

        for (String action : actions) {
            for (int dir = 0; dir < 8; dir++) {
                String key = "foundry_hound_" + action + "_dir" + dir;
                Array<AtlasRegion> regions = atlas.findRegions(key);
                assertNotNull(regions, "Region key should not be null: " + key);
                assertTrue(regions.size > 0, "Missing frames for hound action: " + key);
            }
        }
        atlas.dispose();
    }

    @Test
    @DisplayName("Verify Tileset Atlas contains Obsidian Foundry floor, walls, and pillar regions")
    public void testTilesetAtlasRegions() {
        TextureAtlas atlas = new TextureAtlas(AssetResolver.resolve("assets/atlases/tileset.atlas"));

        assertNotNull(atlas.findRegion("floor_basalt_flagstone"), "Missing floor_basalt_flagstone");
        assertNotNull(atlas.findRegion("floor_molten_heat_vent"), "Missing floor_molten_heat_vent");
        assertNotNull(atlas.findRegion("floor_obsidian_border"), "Missing floor_obsidian_border");
        assertNotNull(atlas.findRegion("floor_rune_circle"), "Missing floor_rune_circle");
        assertNotNull(atlas.findRegion("wall_cast_iron_nw"), "Missing wall_cast_iron_nw");
        assertNotNull(atlas.findRegion("wall_cast_iron_ne"), "Missing wall_cast_iron_ne");
        assertNotNull(atlas.findRegion("wall_corner_outer"), "Missing wall_corner_outer");
        assertNotNull(atlas.findRegion("wall_furnace_vent"), "Missing wall_furnace_vent");
        assertNotNull(atlas.findRegion("pillar_basalt_column"), "Missing pillar_basalt_column");
        assertNotNull(atlas.findRegion("floor_magma_fissure"), "Missing floor_magma_fissure");
        assertNotNull(atlas.findRegion("floor_iron_grate"), "Missing floor_iron_grate");
        assertNotNull(atlas.findRegion("prop_forge_anvil"), "Missing prop_forge_anvil");
        assertNotNull(atlas.findRegion("prop_slag_cauldron"), "Missing prop_slag_cauldron");
        assertNotNull(atlas.findRegion("prop_chrono_chest"), "Missing prop_chrono_chest");
        assertNotNull(atlas.findRegion("prop_chain_spool"), "Missing prop_chain_spool");

        atlas.dispose();
    }

    @Test
    @DisplayName("Verify UI Atlas contains Chrono-Pendant, meters, dash pips, and runic sockets")
    public void testUIAtlasRegions() {
        TextureAtlas atlas = new TextureAtlas(AssetResolver.resolve("assets/atlases/ui.atlas"));

        assertNotNull(atlas.findRegion("chrono_dial_base"), "Missing chrono_dial_base");
        assertNotNull(atlas.findRegion("chrono_needle"), "Missing chrono_needle");
        assertNotNull(atlas.findRegion("chrono_glass_overlay"), "Missing chrono_glass_overlay");
        assertNotNull(atlas.findRegion("meter_frame"), "Missing meter_frame");
        assertNotNull(atlas.findRegion("meter_health_fill"), "Missing meter_health_fill");
        assertNotNull(atlas.findRegion("meter_ghost_fill"), "Missing meter_ghost_fill");
        assertNotNull(atlas.findRegion("meter_poise_fill"), "Missing meter_poise_fill");
        assertNotNull(atlas.findRegion("dash_pip_ready"), "Missing dash_pip_ready");
        assertNotNull(atlas.findRegion("dash_pip_empty"), "Missing dash_pip_empty");
        assertNotNull(atlas.findRegion("socket_empty"), "Missing socket_empty");
        assertNotNull(atlas.findRegion("socket_active_cyan"), "Missing socket_active_cyan");
        assertNotNull(atlas.findRegion("socket_active_amber"), "Missing socket_active_amber");
        assertNotNull(atlas.findRegion("socket_active_purple"), "Missing socket_active_purple");

        atlas.dispose();
    }

    @Test
    @DisplayName("Verify VFX Atlas contains deflect sparks, slash arcs, and dust puffs")
    public void testVFXAtlasRegions() {
        TextureAtlas atlas = new TextureAtlas(AssetResolver.resolve("assets/atlases/vfx.atlas"));

        Array<AtlasRegion> sparks = atlas.findRegions("deflect_spark");
        Array<AtlasRegion> slashes = atlas.findRegions("slash_arc");
        Array<AtlasRegion> dust = atlas.findRegions("dust_puff");

        assertNotNull(sparks, "Missing deflect_spark");
        assertTrue(sparks.size >= 4, "Expected >= 4 deflect spark frames, got " + sparks.size);

        assertNotNull(slashes, "Missing slash_arc");
        assertTrue(slashes.size >= 4, "Expected >= 4 slash arc frames, got " + slashes.size);

        assertNotNull(dust, "Missing dust_puff");
        assertTrue(dust.size >= 4, "Expected >= 4 dust puff frames, got " + dust.size);

        atlas.dispose();
    }
}
