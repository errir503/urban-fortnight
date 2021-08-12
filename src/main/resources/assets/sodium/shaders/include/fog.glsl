#ifdef USE_FOG
vec4 _linearFog(vec4 fragColor, float fragDistance, vec4 fogColor, float fogStart, float fogEnd) {
    vec4 result = mix(fogColor, fragColor,
    smoothstep(fogEnd, fogStart, fragDistance));
    result.a = fragColor.a;

    return result;
}
#else
vec4 _linearFog(vec4 fragColor, float fragDistance, vec4 fogColor, float fogStart, float fogEnd) {
    return fragColor;
}
#endif