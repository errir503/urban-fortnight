layout(std140, binding = 3) uniform FogParametersFS {
    // The color of the shader fog
    vec4 fog_color;

    // The starting position of the shader fog
    float fog_start;

    // The ending position of the shader fog
    float fog_end;

    // The fog rendering style
    int fog_mode;
};