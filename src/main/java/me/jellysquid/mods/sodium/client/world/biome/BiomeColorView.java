package me.jellysquid.mods.sodium.client.world.biome;

public interface BiomeColorView {
    int getColor(BiomeColorSource resolver, int blockX, int blockY, int blockZ);
}
