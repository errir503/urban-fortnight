package me.jellysquid.mods.sodium.render.chunk.compile.tasks;

import me.jellysquid.mods.sodium.render.terrain.TerrainBuildContext;
import me.jellysquid.mods.sodium.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.render.chunk.state.ChunkRenderData;
import me.jellysquid.mods.sodium.util.tasks.CancellationSource;

import java.util.Collections;

/**
 * A build task which does no computation and always return an empty build result. These tasks are created whenever
 * chunk meshes need to be deleted as the only way to change graphics state is to send a message to the main
 * actor thread. In cases where new chunk renders are being created and scheduled, the scheduler will prefer to just
 * synchronously update the render's data to an empty state to speed things along.
 */
@Deprecated
public class EmptyTerrainBuildTask extends AbstractBuilderTask {
    private final RenderSection render;
    private final int frame;

    public EmptyTerrainBuildTask(RenderSection render, int frame) {
        this.render = render;
        this.frame = frame;
    }

    @Override
    public TerrainBuildResult performBuild(TerrainBuildContext context, CancellationSource cancellationSource) {
        return new TerrainBuildResult(this.render, ChunkRenderData.EMPTY, Collections.emptyMap(), this.frame);
    }
}
