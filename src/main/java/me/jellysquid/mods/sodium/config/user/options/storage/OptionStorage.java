package me.jellysquid.mods.sodium.config.user.options.storage;

public interface OptionStorage<T> {
    T getData();

    void save();
}
