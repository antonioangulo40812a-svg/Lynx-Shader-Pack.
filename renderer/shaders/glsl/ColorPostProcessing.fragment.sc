$input v_texcoord0

#include <bgfx_shader.sh>

SAMPLER2D(s_Texture0, 0);
SAMPLER2D(s_DepthTexture, 1);

uniform vec4 u_PostProcessParams; // x: DoF Intensity, y: Aberration Scale, z: Focal Distance, w: Blur Threshold

// Función para el efecto Profundidad de Campo (DoF)
vec4 applyFakeDoF(SAMPLER2D_ARG(tex), vec2 uv, float depth, float focalDist) {
    float focusRange = 0.15;
    float blurFactor = clamp(abs(depth - focalDist) / focusRange, 0.0, 1.0);
    
    if (blurFactor < 0.01) {
        return texture2D(tex, uv);
    }

    vec4 col = vec4(0.0);
    float totalWeight = 0.0;
    float radius = blurFactor * 0.005;

    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            vec2 offset = vec2(float(x), float(y)) * radius;
            col += texture2D(tex, uv + offset);
            totalWeight += 1.0;
        }
    }
    return col / totalWeight;
}

// Función para Aberración Cromática
vec3 applyChromaticAberration(SAMPLER2D_ARG(tex), vec2 uv, float intensity) {
    vec2 distFromCenter = uv - vec2(0.5);
    vec2 offset = distFromCenter * intensity * 0.02;

    float r = texture2D(tex, uv - offset).r;
    float g = texture2D(tex, uv).g;
    float b = texture2D(tex, uv + offset).b;

    return vec3(r, g, b);
}

void main() {
    vec2 uv = v_texcoord0;
    float depth = texture2D(s_DepthTexture, uv).r;

    float dofAmount = u_PostProcessParams.x;
    float caAmount = u_PostProcessParams.y;
    float focalDist = u_PostProcessParams.z;

    // 1. Aplicar Aberración Cromática
    vec3 color = applyChromaticAberration(SAMPLER2D_PARAM(s_Texture0), uv, caAmount);

    // 2. Aplicar Profundidad de Campo (DoF)
    vec4 dofColor = applyFakeDoF(SAMPLER2D_PARAM(s_Texture0), uv, depth, focalDist);
    color = mix(color, dofColor.rgb, dofAmount);

    gl_FragColor = vec4(color, 1.0);
}
