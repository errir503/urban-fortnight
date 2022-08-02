package net.caffeinemc.sodium.render.chunk.draw;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import net.caffeinemc.sodium.SodiumClientMod;
import net.caffeinemc.sodium.render.chunk.RenderSection;
import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPass;
import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPassManager;
import net.caffeinemc.sodium.render.chunk.region.RenderRegion;
import net.caffeinemc.sodium.render.chunk.region.RenderRegionManager;
import net.caffeinemc.sodium.render.chunk.state.ChunkPassModel;
import net.caffeinemc.sodium.render.chunk.state.ChunkRenderBounds;
import net.caffeinemc.sodium.render.terrain.quad.properties.ChunkMeshFace;

public class SortedTerrainLists {
    private static final int REGIONS_ESTIMATE = 32; // idk lol
    private static final int SECTIONS_PER_REGION_ESTIMATE = RenderRegion.REGION_SIZE / 2;
    private static final int INITIAL_CACHE_SIZE = 256;
    
    private final RenderRegionManager regionManager;
    private final ChunkRenderPassManager renderPassManager;
    
    public final List<RenderRegion> regions;
    public final IntList[] regionIndices;
    public final List<LongList> uploadedSegments;
    public final List<IntList> sectionCoords;
    public final List<IntList>[] sectionIndices;
    public final List<IntList>[] modelPartCounts;
    public final List<LongList>[] modelPartSegments;
    
    // pools to save on many list allocations
    private final Deque<LongList> uploadedSegmentsListPool;
    private final Deque<IntList> sectionCoordsListPool;
    private final Deque<IntList> sectionIndicesListPool;
    private final Deque<IntList> modelPartCountsListPool;
    private final Deque<LongList> modelPartSegmentsListPool;
    
    private int totalSectionCount;

    @SuppressWarnings("unchecked")
    public SortedTerrainLists(RenderRegionManager regionManager, ChunkRenderPassManager renderPassManager) {
        this.regionManager = regionManager;
        this.renderPassManager = renderPassManager;
        
        int totalPasses = renderPassManager.getRenderPassCount();
    
        this.regions = new ReferenceArrayList<>(REGIONS_ESTIMATE);
        this.uploadedSegments = new ReferenceArrayList<>(REGIONS_ESTIMATE);
        this.sectionCoords = new ReferenceArrayList<>(REGIONS_ESTIMATE);
        this.regionIndices = new IntList[totalPasses];
        this.sectionIndices = new List[totalPasses];
        this.modelPartCounts = new List[totalPasses];
        this.modelPartSegments = new List[totalPasses];
        
        for (int passId = 0; passId < totalPasses; passId++) {
            this.regionIndices[passId] = new IntArrayList(REGIONS_ESTIMATE);
            this.sectionIndices[passId] = new ReferenceArrayList<>(REGIONS_ESTIMATE);
            this.modelPartCounts[passId] = new ReferenceArrayList<>(REGIONS_ESTIMATE);
            this.modelPartSegments[passId] = new ReferenceArrayList<>(REGIONS_ESTIMATE);
        }
        
        this.uploadedSegmentsListPool = new ArrayDeque<>(INITIAL_CACHE_SIZE);
        this.sectionCoordsListPool = new ArrayDeque<>(INITIAL_CACHE_SIZE);
        this.sectionIndicesListPool = new ArrayDeque<>(INITIAL_CACHE_SIZE);
        this.modelPartCountsListPool = new ArrayDeque<>(INITIAL_CACHE_SIZE);
        this.modelPartSegmentsListPool = new ArrayDeque<>(INITIAL_CACHE_SIZE);
    }
    
    private void reset() {
        this.regions.clear();
    
        for (IntList list : this.regionIndices) {
            list.clear();
        }
    
        // flush everything out to the list caches
        this.uploadedSegmentsListPool.addAll(this.uploadedSegments);
        this.uploadedSegments.clear();
    
        this.sectionCoordsListPool.addAll(this.sectionCoords);
        this.sectionCoords.clear();
    
        for (List<IntList> list : this.sectionIndices) {
            this.sectionIndicesListPool.addAll(list);
            list.clear();
        }
        
        for (List<IntList> list : this.modelPartCounts) {
            this.modelPartCountsListPool.addAll(list);
            list.clear();
        }
        
        for (List<LongList> list : this.modelPartSegments) {
            this.modelPartSegmentsListPool.addAll(list);
            list.clear();
        }
        
        this.totalSectionCount = 0;
    }
    
    private LongList getUploadedSegmentsList() {
        LongList cachedList = this.uploadedSegmentsListPool.pollLast();
        if (cachedList != null) {
            cachedList.clear();
            return cachedList;
        } else {
            return new LongArrayList(SECTIONS_PER_REGION_ESTIMATE);
        }
    }
    
    private IntList getSectionCoordsList() {
        IntList cachedList = this.sectionCoordsListPool.pollLast();
        if (cachedList != null) {
            cachedList.clear();
            return cachedList;
        } else {
            return new IntArrayList(SECTIONS_PER_REGION_ESTIMATE * 3); // component count for position (x, y, z)
        }
    }
    
    private IntList getSectionIndicesList() {
        IntList cachedList = this.sectionIndicesListPool.pollLast();
        if (cachedList != null) {
            cachedList.clear();
            return cachedList;
        } else {
            return new IntArrayList(SECTIONS_PER_REGION_ESTIMATE);
        }
    }
    
    private IntList getModelPartCountsList() {
        IntList cachedList = this.modelPartCountsListPool.pollLast();
        if (cachedList != null) {
            cachedList.clear();
            return cachedList;
        } else {
            return new IntArrayList(SECTIONS_PER_REGION_ESTIMATE);
        }
    }
    
    private LongList getModelPartSegmentsList() {
        LongList cachedList = this.modelPartSegmentsListPool.pollLast();
        if (cachedList != null) {
            cachedList.clear();
            return cachedList;
        } else {
            return new LongArrayList(SECTIONS_PER_REGION_ESTIMATE * ChunkMeshFace.COUNT);
        }
    }
    
    public void update(List<RenderSection> sortedSections, ChunkCameraContext camera) {
        this.reset();
        
        if (sortedSections.isEmpty()) {
            return;
        }
        
        boolean useBlockFaceCulling = SodiumClientMod.options().performance.useBlockFaceCulling;
        ChunkRenderPass[] chunkRenderPasses = this.renderPassManager.getAllRenderPasses();
        int totalPasses = chunkRenderPasses.length;
        int regionTableSize = this.regionManager.getRegionTableSize();
    
        // lookup tables indexed by region id
        LongList[] uploadedSegmentsTable = new LongList[regionTableSize];
        IntList[] sectionCoordsTable = new IntList[regionTableSize];
        int[] sequentialRegionIdxTable = new int[regionTableSize];
        
        // lookup tables indexed by region id and pass id
        IntList[][] modelPartCountsTable = new IntList[regionTableSize][totalPasses];
        LongList[][] modelPartSegmentsTable = new LongList[regionTableSize][totalPasses];
        IntList[][] sectionIndicesTable = new IntList[regionTableSize][totalPasses];
        
        // index counters
        int sequentialRegionIdx = 0;
        
        int totalSectionCount = 0;
        
        for (RenderSection section : sortedSections) {
            boolean sectionAdded = false;
    
            int sequentialSectionIdx = 0;
            IntList[] regionModelPartCounts = null;
            LongList[] regionModelPartSegments = null;
            IntList[] regionSectionIndices = null;
    
            for (int passId = 0; passId < totalPasses; passId++) {
                // prior checks to avoid any unnecessary allocation
                ChunkPassModel model = section.getData().models[passId];
                
                // skip if the section has no models for the pass
                if (model == null) {
                    continue;
                }
    
                int visibilityBits = model.getVisibilityBits();

                if (useBlockFaceCulling) {
                    visibilityBits &= calculateCameraVisibilityBits(section.getData().bounds, camera);
                }

                // skip if the section has no *visible* models for the pass
                if (visibilityBits == 0) {
                    continue;
                }
                
                RenderRegion region = section.getRegion();
                int regionId = region.getId();
    
                // lazily allocate everything here
                
                // add per-section data, and make sure to only add once
                // warning: don't use passId in here
                if (!sectionAdded) {
                    LongList regionUploadedSegments = uploadedSegmentsTable[regionId];
                    IntList regionSectionCoords = sectionCoordsTable[regionId];
                    regionModelPartCounts = modelPartCountsTable[regionId];
                    regionModelPartSegments = modelPartSegmentsTable[regionId];
                    regionSectionIndices = sectionIndicesTable[regionId];
    
                    // if one is null, the region hasn't been processed yet
                    if (regionUploadedSegments == null) {
                        regionUploadedSegments = this.getUploadedSegmentsList();
                        uploadedSegmentsTable[regionId] = regionUploadedSegments;
                        this.uploadedSegments.add(regionUploadedSegments);
                        
                        regionSectionCoords = this.getSectionCoordsList();
                        sectionCoordsTable[regionId] = regionSectionCoords;
                        this.sectionCoords.add(regionSectionCoords);
                        
                        this.regions.add(region);
                        
                        // set, then increment
                        sequentialRegionIdxTable[regionId] = sequentialRegionIdx++;
                    }
    
                    // get size before adding, avoiding unnecessary subtraction
                    sequentialSectionIdx = regionUploadedSegments.size();
                    regionUploadedSegments.add(section.getUploadedGeometrySegment());
                    
                    regionSectionCoords.add(section.getChunkX());
                    regionSectionCoords.add(section.getChunkY());
                    regionSectionCoords.add(section.getChunkZ());
                    
                    totalSectionCount++;
                    sectionAdded = true;
                }
    
                // add per-section-pass data
                IntList regionPassModelPartCounts = regionModelPartCounts[passId];
                LongList regionPassModelPartSegments = regionModelPartSegments[passId];
                IntList regionPassSectionIndices = regionSectionIndices[passId];
    
                // if one is null, the region + pass combination hasn't been processed yet
                if (regionPassModelPartCounts == null) {
                    regionPassModelPartCounts = this.getModelPartCountsList();
                    regionModelPartCounts[passId] = regionPassModelPartCounts;
                    this.modelPartCounts[passId].add(regionPassModelPartCounts);
    
                    regionPassModelPartSegments = this.getModelPartSegmentsList();
                    regionModelPartSegments[passId] = regionPassModelPartSegments;
                    this.modelPartSegments[passId].add(regionPassModelPartSegments);
    
                    regionPassSectionIndices = this.getSectionIndicesList();
                    regionSectionIndices[passId] = regionPassSectionIndices;
                    this.sectionIndices[passId].add(regionPassSectionIndices);
                    
                    this.regionIndices[passId].add(sequentialRegionIdxTable[regionId]);
                }
    
                regionPassSectionIndices.add(sequentialSectionIdx);
                regionPassModelPartCounts.add(Integer.bitCount(visibilityBits));
                
                // We want to make sure the direction order is the same, whether the pass reverses
                // the iteration or not. It's faster to do that here, because it'll allow the
                // iteration to effectively prefetch these values, allowing either a fully
                // sequential forwards or backwards iteration.
                //
                // These functions also employ faster ways to iterate through indices of set bits
                // in an integer.
                // warning: don't use visibilityBits after these functions, they are destructive
                boolean reverseOrder = chunkRenderPasses[passId].isTranslucent();
                long[] modelPartSegments = model.getModelPartSegments();
                
                if (reverseOrder) {
                    while (visibilityBits != 0) {
                        int dirIdx = (Integer.SIZE - 1) - Integer.numberOfLeadingZeros(visibilityBits);
                        regionPassModelPartSegments.add(modelPartSegments[dirIdx]);
                        visibilityBits ^= 1 << dirIdx;
                    }
                } else {
                    while (visibilityBits != 0) {
                        int dirIdx = Integer.numberOfTrailingZeros(visibilityBits);
                        regionPassModelPartSegments.add(modelPartSegments[dirIdx]);
                        visibilityBits &= visibilityBits - 1;
                    }
                }
            }
        }
        
        this.totalSectionCount = totalSectionCount;
    }
    
    protected static int calculateCameraVisibilityBits(ChunkRenderBounds bounds, ChunkCameraContext camera) {
        int bits = ChunkMeshFace.UNASSIGNED_BITS;
        
        if (camera.posY > bounds.y1) {
            bits |= ChunkMeshFace.UP_BITS;
        }
        
        if (camera.posY < bounds.y2) {
            bits |= ChunkMeshFace.DOWN_BITS;
        }
        
        if (camera.posX > bounds.x1) {
            bits |= ChunkMeshFace.EAST_BITS;
        }
        
        if (camera.posX < bounds.x2) {
            bits |= ChunkMeshFace.WEST_BITS;
        }
        
        if (camera.posZ > bounds.z1) {
            bits |= ChunkMeshFace.SOUTH_BITS;
        }
        
        if (camera.posZ < bounds.z2) {
            bits |= ChunkMeshFace.NORTH_BITS;
        }
        
        return bits;
    }

    public int getTotalSectionCount() {
        return this.totalSectionCount;
    }

    public boolean isEmpty() {
        return this.totalSectionCount == 0;
    }
    
}
