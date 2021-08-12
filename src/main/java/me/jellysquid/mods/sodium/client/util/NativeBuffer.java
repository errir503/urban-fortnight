package me.jellysquid.mods.sodium.client.util;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMaps;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.system.MemoryUtil;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.stream.Collectors;

public class NativeBuffer {
    private static final Logger LOGGER = LogManager.getLogger(NativeBuffer.class);

    private static final boolean ENABLE_STACK_TRACE_LOGGING = false;

    private static final ReferenceQueue<NativeBuffer> RECLAIM_QUEUE = new ReferenceQueue<>();
    private static final Reference2ReferenceMap<Reference<NativeBuffer>, BufferReference> ACTIVE_BUFFERS =
            Reference2ReferenceMaps.synchronize(new Reference2ReferenceOpenHashMap<>());

    private static long ALLOCATED = 0L;

    private final BufferReference ref;

    public NativeBuffer(int capacity) {
        this.ref = allocate(capacity);

        ACTIVE_BUFFERS.put(new PhantomReference<>(this, RECLAIM_QUEUE), this.ref);
    }

    public ByteBuffer getDirectBuffer() {
        this.ref.checkFreed();

        return MemoryUtil.memByteBuffer(this.ref.address, this.ref.length);
    }

    public void free() {
        deallocate(this.ref);
    }

    public int size() {
        return this.ref.length;
    }

    public static void reclaim(boolean forceGc) {
        if (forceGc) {
            System.gc();
        }

        Reference<? extends NativeBuffer> ref;

        while ((ref = RECLAIM_QUEUE.poll()) != null) {
            BufferReference buf = ACTIVE_BUFFERS.remove(ref);

            if (buf.freed) {
                continue;
            }

            deallocate(buf);

            if (buf.allocationSite != null) {
                LOGGER.warn("Reclaimed {} bytes at address {} that were leaked from allocation site:\n{}",
                        buf.length, buf.address,
                        Arrays.stream(buf.allocationSite)
                                .map(StackTraceElement::toString)
                                .collect(Collectors.joining("\n")));
            } else {
                LOGGER.warn("Reclaimed {} bytes at address {} that were leaked from an unknown location (logging is disabled)",
                        buf.length, buf.address);
            }
        }
    }

    public static long getTotalAllocated() {
        return ALLOCATED;
    }

    private static StackTraceElement[] getStackTrace() {
        return ENABLE_STACK_TRACE_LOGGING ? Thread.currentThread()
                .getStackTrace() : null;
    }

    private static BufferReference allocate(int capacity) {
        long address = 0;

        for (int i = 0; i < 3; i++) {
            address = MemoryUtil.nmemAlloc(capacity);

            if (address != MemoryUtil.NULL) {
                break;
            }

            // If memory allocation fails, force a garbage collection
            reclaim(true);
        }

        if (address == MemoryUtil.NULL) {
            throw new OutOfMemoryError("Couldn't allocate %s bytes".formatted(capacity));
        }

        StackTraceElement[] stackTrace = ENABLE_STACK_TRACE_LOGGING ? Thread.currentThread().getStackTrace() : null;

        BufferReference ref = new BufferReference(address, capacity, stackTrace);
        ALLOCATED += ref.length;

        return ref;
    }

    private static void deallocate(BufferReference ref) {
        ref.checkFreed();
        ref.freed = true;

        MemoryUtil.nmemFree(ref.address);

        ALLOCATED -= ref.length;
    }

    private static class BufferReference {
        public final long address;
        public final int length;

        public final StackTraceElement[] allocationSite;

        public boolean freed;

        private BufferReference(long address, int length, StackTraceElement[] allocationSite) {
            this.address = address;
            this.length = length;
            this.allocationSite = allocationSite;
        }

        private void checkFreed() {
            if (this.freed) {
                throw new IllegalStateException("Buffer has been deleted");
            }
        }
    }
}
