package me.jellysquid.mods.sodium.client.render.viewport.frustum;

import org.joml.FrustumIntersection;

public final class SimpleFrustum implements Frustum {
    private final FrustumIntersection frustum;

    public SimpleFrustum(FrustumIntersection frustumIntersection) {
        this.frustum = frustumIntersection;
    }

    @Override
    public boolean testAab(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        return this.frustum.testAab(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
