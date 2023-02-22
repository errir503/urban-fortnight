package me.jellysquid.mods.sodium.client.gl.device;

import me.jellysquid.mods.sodium.client.gl.tessellation.GlIndexType;

public interface DrawCommandList extends AutoCloseable {
    void multiDrawElementsBaseVertex(MultiDrawBatch batch, GlIndexType indexType);

    void endTessellating();

    void flush();

    @Override
    default void close() {
        this.flush();
    }
}
