#ifdef GL_ES
precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords;
varying vec2 v_worldPos;

uniform sampler2D u_texture;
uniform vec2 u_targetPos;       // Target character feet screen coordinates
uniform float u_apertureRadius; // Dissolve circle radius in screen pixels (e.g. 54.0)
uniform float u_cutawayActive;   // 1.0 = active, 0.0 = inactive

// 4x4 Ordered Bayer Dithering Matrix
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
