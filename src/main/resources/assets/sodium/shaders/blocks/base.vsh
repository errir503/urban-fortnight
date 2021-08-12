#import <sodium:include/fog.glsl>

// STRUCTS
struct DrawParameters {
    // Older AMD drivers can't handle vec3 in std140 layouts correctly
    // The alignment requirement is 16 bytes (4 float components) anyways, so we're not wasting extra memory with this,
    // only fixing broken drivers.
    vec4 Offset;
};

// INPUTS
in vec4 a_Pos; // The position of the vertex around the model origin
in vec4 a_Color; // The color of the vertex
in vec2 a_TexCoord; // The block texture coordinate of the vertex
in vec2 a_LightCoord; // The light texture coordinate of the vertex

// UNIFORMS
uniform mat4 u_ModelViewProjectionMatrix;

uniform float u_ModelScale;
uniform float u_ModelOffset;

uniform float u_TextureScale;

layout(std140) uniform ubo_DrawParameters {
    DrawParameters Chunks[256];
};

uniform vec3 u_CameraTranslation;

// OUTPUTS
out vec4 v_Color;
out vec2 v_TexCoord;
out vec2 v_LightCoord;

#ifdef USE_FOG
out float v_FragDistance;
#endif

vec3 getVertexPosition() {
    vec3 vertexPosition = a_Pos.xyz * u_ModelScale + u_ModelOffset;
    vec3 chunkOffset = Chunks[int(a_Pos.w)].Offset.xyz; // AMD drivers also need this manually inlined

    return chunkOffset + vertexPosition + u_CameraTranslation;
}