package me.jellysquid.mods.sodium.interop.vanilla.options;

import me.jellysquid.mods.sodium.SodiumClientMod;
import me.jellysquid.mods.sodium.config.user.options.storage.OptionStorage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;

public class MinecraftOptionsStorage implements OptionStorage<GameOptions> {
    private final MinecraftClient client;

    public MinecraftOptionsStorage() {
        this.client = MinecraftClient.getInstance();
    }

    @Override
    public GameOptions getData() {
        return this.client.options;
    }

    @Override
    public void save() {
        this.getData().write();

        SodiumClientMod.logger().info("Flushed changes to Minecraft configuration");
    }
}
