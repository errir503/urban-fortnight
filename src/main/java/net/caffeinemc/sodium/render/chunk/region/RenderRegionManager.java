package net.caffeinemc.sodium.render.chunk.region;

import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSortedSet;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import net.caffeinemc.gfx.api.buffer.ImmutableBuffer;
import net.caffeinemc.gfx.api.buffer.ImmutableBufferFlags;
import net.caffeinemc.gfx.api.buffer.MappedBufferFlags;
import net.caffeinemc.gfx.api.device.RenderDevice;
import net.caffeinemc.gfx.util.buffer.BufferPool;
import net.caffeinemc.gfx.util.buffer.streaming.SectionedStreamingBuffer;
import net.caffeinemc.gfx.util.buffer.streaming.StreamingBuffer;
import net.caffeinemc.sodium.SodiumClientMod;
import net.caffeinemc.sodium.render.buffer.arena.ArenaBuffer;
import net.caffeinemc.sodium.render.buffer.arena.BufferSegment;
import net.caffeinemc.sodium.render.buffer.arena.PendingUpload;
import net.caffeinemc.sodium.render.chunk.RenderSection;
import net.caffeinemc.sodium.render.chunk.compile.tasks.TerrainBuildResult;
import net.caffeinemc.sodium.render.chunk.state.ChunkRenderData;
import net.caffeinemc.sodium.render.terrain.format.TerrainVertexType;
import net.caffeinemc.sodium.util.IntPool;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.profiler.Profiler;

public class RenderRegionManager {
    // these constants have been found from experimentation
    private static final int PRUNE_SAMPLE_SIZE = 100;
    private static final double PRUNE_RATIO_THRESHOLD = .35;
    private static final float PRUNE_PERCENT_MODIFIER = -.2f;
    private static final float DEFRAG_THRESHOLD = 0.000008f; // this may look dumb, but keep in mind that 1.0 is the absolute maximum
    
    private final Long2ReferenceMap<RenderRegion> regions = new Long2ReferenceOpenHashMap<>();
    private final IntPool idPool = new IntPool();
    private final BufferPool<ImmutableBuffer> bufferPool;

    private final RenderDevice device;
    private final TerrainVertexType vertexType;
    private final StreamingBuffer stagingBuffer;

    public RenderRegionManager(RenderDevice device, TerrainVertexType vertexType) {
        this.device = device;
        this.vertexType = vertexType;
    
        this.bufferPool = new BufferPool<>(
                device,
                PRUNE_SAMPLE_SIZE,
                c -> device.createBuffer(
                        c,
                        EnumSet.noneOf(ImmutableBufferFlags.class)
                )
        );

        var maxInFlightFrames = SodiumClientMod.options().advanced.cpuRenderAheadLimit + 1;
        this.stagingBuffer = new SectionedStreamingBuffer(
                device,
                1,
                0x80000, // start with 512KiB per section and expand from there if needed
                maxInFlightFrames,
                EnumSet.of(
                        MappedBufferFlags.EXPLICIT_FLUSH,
                        MappedBufferFlags.CLIENT_STORAGE
                )
        );
    }

    public RenderRegion getRegion(long regionId) {
        return this.regions.get(regionId);
    }

    public void cleanup() {
        Iterator<RenderRegion> it = this.regions.values()
                .iterator();

        while (it.hasNext()) {
            RenderRegion region = it.next();

            if (region.isEmpty()) {
                this.deleteRegion(region);
                it.remove();
            }
        }
    
        long activeSize = this.getDeviceAllocatedMemoryActive();
        long reserveSize = this.bufferPool.getDeviceAllocatedMemory();
        
        if ((double) reserveSize / activeSize > PRUNE_RATIO_THRESHOLD) {
            this.prune();
        }
    }
    
    public void prune() {
        this.bufferPool.prune(PRUNE_PERCENT_MODIFIER);
    }

    public void uploadChunks(Iterator<TerrainBuildResult> queue, int frameIndex, @Deprecated RenderUpdateCallback callback) {
        Profiler profiler = MinecraftClient.getInstance().getProfiler();
        
        profiler.push("chunk_upload");
        
        // we have to use a list with a varied size here, because the upload method can create new regions
        ReferenceList<RenderRegion> writtenRegions = new ReferenceArrayList<>(Math.max(this.getRegionTableSize(), 16));
        
        for (var entry : this.setupUploadBatches(queue)) {
            this.uploadGeometryBatch(entry.getLongKey(), entry.getValue(), frameIndex);

            for (TerrainBuildResult result : entry.getValue()) {
                RenderSection section = result.render();

                if (section.getData() != null) {
                    callback.accept(section, section.getData(), result.data());
                }

                section.setData(result.data());
                section.setLastAcceptedBuildTime(result.buildTime());

                result.delete();
    
                RenderRegion region = section.getRegion();
                if (region != null) {
                    // expand list as needed
                    int currentSize = writtenRegions.size();
                    int requiredSize = region.getId() + 1;
                    if (currentSize < requiredSize) {
                        writtenRegions.size(Math.max(requiredSize, currentSize * 2));
                    }
                    writtenRegions.set(region.getId(), region);
                }
            }
        }
    
        profiler.swap("chunk_defrag");
        
        // check if we need to defragment any of the regions we just modified
        for (RenderRegion region : writtenRegions) {
            // null entries will exist due to the nature of the ID based table
            if (region == null) {
                continue;
            }
            
            ArenaBuffer arenaBuffer = region.getVertexBuffer();
            if (arenaBuffer.getFragmentation() >= DEFRAG_THRESHOLD) {
                LongSortedSet removedSegments = arenaBuffer.compact();
            
                if (removedSegments == null) {
                    continue;
                }
            
                // fix existing sections' buffer segment locations after the defrag
                for (RenderSection section : region.getSections()) {
                    long currentBufferSegment = section.getUploadedGeometrySegment();
                    int currentSegmentOffset = BufferSegment.getOffset(currentBufferSegment);
                    int currentSegmentLength = BufferSegment.getLength(currentBufferSegment);
                
                    for (long prevFreedSegment : removedSegments.headSet(currentBufferSegment)) {
                        currentSegmentOffset -= BufferSegment.getLength(prevFreedSegment);
                    }
                
                    long newBufferSegment = BufferSegment.createKey(currentSegmentLength, currentSegmentOffset);
                    section.setBufferSegment(newBufferSegment); // TODO: in the future, if something extra happens when this method is called, we should check if cur = new
                }
            }
        }
    
        profiler.pop();
    }

    public int getRegionTableSize() {
        return this.idPool.capacity();
    }

    public interface RenderUpdateCallback {
        void accept(RenderSection section, ChunkRenderData prev, ChunkRenderData next);
    }

    private void uploadGeometryBatch(long regionKey, List<TerrainBuildResult> results, int frameIndex) {
        List<PendingUpload> uploads = new ReferenceArrayList<>(results.size());

        for (TerrainBuildResult result : results) {
            var section = result.render();
            var geometry = result.geometry();

            // De-allocate all storage for the meshes we're about to replace
            // This will allow it to be cheaply re-allocated later
            section.ensureGeometryDeleted();

            var vertices = geometry.vertices();
    
            // Only submit an upload job if there is data in the first place
            if (vertices != null) {
                uploads.add(new PendingUpload(section, vertices.buffer()));
            }
        }

        // If we have nothing to upload, don't attempt to allocate a region
        if (uploads.isEmpty()) {
            return;
        }

        RenderRegion region = this.regions.get(regionKey);

        if (region == null) {
            region = new RenderRegion(
                    this.device,
                    this.stagingBuffer,
                    this.bufferPool,
                    this.vertexType,
                    this.idPool.create()
            );
            
            this.regions.put(regionKey, region);
        }
        
        region.submitUploads(uploads, frameIndex);
    }

    private Iterable<Long2ReferenceMap.Entry<List<TerrainBuildResult>>> setupUploadBatches(Iterator<TerrainBuildResult> renders) {
        var batches = new Long2ReferenceOpenHashMap<List<TerrainBuildResult>>();

        while (renders.hasNext()) {
            TerrainBuildResult result = renders.next();
            RenderSection render = result.render();

            // TODO: this is kinda gross, maybe find a way to make the Future dispose of the result when cancelled?
            if (render.isDisposed() || result.buildTime() <= render.getLastAcceptedBuildTime()) {
                result.delete();
                continue;
            }

            var batch = batches.computeIfAbsent(render.getRegionKey(), key -> new ReferenceArrayList<>());
            batch.add(result);
        }

        return batches.long2ReferenceEntrySet();
    }

    public void delete() {
        for (RenderRegion region : this.regions.values()) {
            region.delete();
        }
        this.regions.clear();
        
        this.bufferPool.delete();
        this.stagingBuffer.delete();
    }

    private void deleteRegion(RenderRegion region) {
        var id = region.getId();
        region.delete();

        this.idPool.free(id);
    }
    
    private long getDeviceAllocatedMemoryActive() {
        long sum = 0L;
        for (RenderRegion region : this.regions.values()) {
            long deviceAllocatedMemory = region.getDeviceAllocatedMemory();
            sum += deviceAllocatedMemory;
        }
        return sum;
    }
    
    public int getDeviceBufferObjects() {
        return this.regions.size() + this.bufferPool.getDeviceBufferObjects();
    }
    
    public long getDeviceUsedMemory() {
        // the buffer pool doesn't actively use any memory
        long sum = 0L;
        for (RenderRegion region : this.regions.values()) {
            long deviceUsedMemory = region.getDeviceUsedMemory();
            sum += deviceUsedMemory;
        }
        return sum;
    }
    
    public long getDeviceAllocatedMemory() {
        return this.getDeviceAllocatedMemoryActive() + this.bufferPool.getDeviceAllocatedMemory();
    }
}
