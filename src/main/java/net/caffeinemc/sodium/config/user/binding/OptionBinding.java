package net.caffeinemc.sodium.config.user.binding;

public interface OptionBinding<S, T> {
    void setValue(S storage, T value);

    T getValue(S storage);
}
