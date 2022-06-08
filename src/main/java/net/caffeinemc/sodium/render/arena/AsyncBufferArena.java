package net.caffeinemc.sodium.render.arena;

import net.caffeinemc.gfx.api.buffer.Buffer;
import net.caffeinemc.gfx.api.device.RenderDevice;

import java.util.*;

// TODO: handle alignment
// TODO: handle element vs pointers
public class AsyncBufferArena implements BufferArena {
    static final boolean CHECK_ASSERTIONS = false;

    private final int resizeIncrement;

    private final RenderDevice device;
    private Buffer arenaBuffer;

    private BufferSegment head;

    private int capacity;
    private int used;

    private final int stride;

    public AsyncBufferArena(RenderDevice device, int capacity, int stride) {
        this.device = device;
        this.resizeIncrement = capacity / 16;
        this.capacity = capacity;

        this.head = new BufferSegment(this, 0, capacity);
        this.head.setFree(true);

        this.arenaBuffer = device.createBuffer((long) capacity * stride);
        this.stride = stride;
    }

    private void resize(int newCapacity) {
        if (this.used > newCapacity) {
            throw new UnsupportedOperationException("New capacity must be larger than used size");
        }

        this.checkAssertions();

        int base = newCapacity - this.used;

        List<BufferSegment> usedSegments = this.getUsedSegments();
        List<PendingBufferCopyCommand> pendingCopies = this.buildTransferList(usedSegments, base);

        this.transferSegments(pendingCopies, newCapacity);

        this.head = new BufferSegment(this, 0, base);
        this.head.setFree(true);

        if (usedSegments.isEmpty()) {
            this.head.setNext(null);
        } else {
            this.head.setNext(usedSegments.get(0));
            this.head.getNext()
                    .setPrev(this.head);
        }

        this.checkAssertions();
    }

    private List<PendingBufferCopyCommand> buildTransferList(List<BufferSegment> usedSegments, int base) {
        List<PendingBufferCopyCommand> pendingCopies = new ArrayList<>();
        PendingBufferCopyCommand currentCopyCommand = null;

        int writeOffset = base;

        for (int i = 0; i < usedSegments.size(); i++) {
            BufferSegment s = usedSegments.get(i);

            if (currentCopyCommand == null || currentCopyCommand.readOffset + currentCopyCommand.length != s.getOffset()) {
                if (currentCopyCommand != null) {
                    pendingCopies.add(currentCopyCommand);
                }

                currentCopyCommand = new PendingBufferCopyCommand(s.getOffset(), writeOffset, s.getLength());
            } else {
                currentCopyCommand.length += s.getLength();
            }

            s.setOffset(writeOffset);

            if (i + 1 < usedSegments.size()) {
                s.setNext(usedSegments.get(i + 1));
            } else {
                s.setNext(null);
            }

            if (i - 1 < 0) {
                s.setPrev(null);
            } else {
                s.setPrev(usedSegments.get(i - 1));
            }

            writeOffset += s.getLength();
        }

        if (currentCopyCommand != null) {
            pendingCopies.add(currentCopyCommand);
        }

        return pendingCopies;
    }

    private void transferSegments(Collection<PendingBufferCopyCommand> list, int capacity) {
        var srcBufferObj = this.arenaBuffer;
        var dstBufferObj = this.device.createBuffer(this.toBytes(capacity));

        for (PendingBufferCopyCommand cmd : list) {
            this.device.copyBuffer(srcBufferObj, this.toBytes(cmd.readOffset), dstBufferObj, this.toBytes(cmd.writeOffset), this.toBytes(cmd.length));
        }

        this.device.deleteBuffer(srcBufferObj);

        this.arenaBuffer = dstBufferObj;
        this.capacity = capacity;
    }

    private ArrayList<BufferSegment> getUsedSegments() {
        ArrayList<BufferSegment> used = new ArrayList<>();
        BufferSegment seg = this.head;

        while (seg != null) {
            BufferSegment next = seg.getNext();

            if (!seg.isFree()) {
                used.add(seg);
            }

            seg = next;
        }

        return used;
    }

    @Override
    public int getDeviceUsedMemory() {
        return this.toBytes(this.used);
    }

    @Override
    public int getDeviceAllocatedMemory() {
        return this.toBytes(this.capacity);
    }

    private BufferSegment alloc(int size) {
        BufferSegment a = this.findFree(size);

        if (a == null) {
            return null;
        }

        BufferSegment result;

        if (a.getLength() == size) {
            a.setFree(false);

            result = a;
        } else {
            BufferSegment b = new BufferSegment(this, a.getEnd() - size, size);
            b.setNext(a.getNext());
            b.setPrev(a);

            if (b.getNext() != null) {
                b.getNext()
                        .setPrev(b);
            }

            a.setLength(a.getLength() - size);
            a.setNext(b);

            result = b;
        }

        this.used += result.getLength();
        this.checkAssertions();

        return result;
    }

    private BufferSegment findFree(int size) {
        BufferSegment entry = this.head;
        BufferSegment best = null;

        while (entry != null) {
            if (entry.isFree()) {
                if (entry.getLength() == size) {
                    return entry;
                } else if (entry.getLength() >= size) {
                    if (best == null || best.getLength() > entry.getLength()) {
                        best = entry;
                    }
                }
            }

            entry = entry.getNext();
        }

        return best;
    }

    @Override
    public void free(BufferSegment entry) {
        if (entry.isFree()) {
            throw new IllegalStateException("Already freed");
        }

        entry.setFree(true);

        this.used -= entry.getLength();

        BufferSegment next = entry.getNext();

        if (next != null && next.isFree()) {
            entry.mergeInto(next);
        }

        BufferSegment prev = entry.getPrev();

        if (prev != null && prev.isFree()) {
            prev.mergeInto(entry);
        }

        this.checkAssertions();
    }

    @Override
    public void delete() {
        this.device.deleteBuffer(this.arenaBuffer);
    }

    @Override
    public boolean isEmpty() {
        return this.used <= 0;
    }

    @Override
    public Buffer getBufferObject() {
        return this.arenaBuffer;
    }

    @Override
    public void upload(List<PendingUpload> uploads) {
        var batch = new UploadBatch(this.device, uploads);

        // Try to upload all of the data into free segments first
        batch.queue.removeIf(entry -> this.tryUpload(batch, entry));

        // If we weren't able to upload some buffers, they will have been left behind in the queue
        if (!batch.queue.isEmpty()) {
            // Calculate the amount of memory needed for the remaining uploads
            int remainingElements = batch.queue
                    .stream()
                    .mapToInt(upload -> this.toElements(upload.length()))
                    .sum();

            // Ask the arena to grow to accommodate the remaining uploads
            // This will force a re-allocation and compaction, which will leave us a continuous free segment
            // for the remaining uploads
            this.ensureCapacity(remainingElements);

            // Try again to upload any buffers that failed last time
            batch.queue.removeIf(entry -> this.tryUpload(batch, entry));

            // If we still had failures, something has gone wrong
            if (!batch.queue.isEmpty()) {
                throw new RuntimeException("Failed to upload all buffers");
            }
        }

        this.device.deleteBuffer(batch.buffer);
    }

    private boolean tryUpload(UploadBatch batch, UploadBatch.Entry entry) {
        Buffer src = batch.buffer;
        BufferSegment segment = this.alloc(this.toElements(entry.length()));

        if (segment == null) {
            return false;
        }

        // Copy the data into our staging buffer, then copy it into the arena's buffer
        this.device.copyBuffer(src, entry.offset(), this.arenaBuffer, this.toBytes(segment.getOffset()), entry.length());

        entry.holder().set(segment);

        return true;
    }

    public void ensureCapacity(int elementCount) {
        // Re-sizing the arena results in a compaction, so any free space in the arena will be
        // made into one contiguous segment, joined with the new segment of free space we're asking for
        // We calculate the number of free elements in our arena and then subtract that from the total requested
        int elementsNeeded = elementCount - (this.capacity - this.used);

        // Try to allocate some extra buffer space unless this is an unusually large allocation
        this.resize(Math.max(this.capacity + this.resizeIncrement, this.capacity + elementsNeeded));
    }

    private void checkAssertions() {
        if (CHECK_ASSERTIONS) {
            this.checkAssertions0();
        }
    }

    private void checkAssertions0() {
        BufferSegment seg = this.head;
        int used = 0;

        while (seg != null) {
            if (seg.getOffset() < 0) {
                throw new IllegalStateException("segment.start < 0: out of bounds");
            } else if (seg.getEnd() > this.capacity) {
                throw new IllegalStateException("segment.end > arena.capacity: out of bounds");
            }

            if (!seg.isFree()) {
                used += seg.getLength();
            }

            BufferSegment next = seg.getNext();

            if (next != null) {
                if (next.getOffset() < seg.getEnd()) {
                    throw new IllegalStateException("segment.next.start < segment.end: overlapping segments (corrupted)");
                } else if (next.getOffset() > seg.getEnd()) {
                    throw new IllegalStateException("segment.next.start > segment.end: not truly connected (sparsity error)");
                }

                if (next.isFree() && next.getNext() != null) {
                    if (next.getNext().isFree()) {
                        throw new IllegalStateException("segment.free && segment.next.free: not merged consecutive segments");
                    }
                }
            }

            BufferSegment prev = seg.getPrev();

            if (prev != null) {
                if (prev.getEnd() > seg.getOffset()) {
                    throw new IllegalStateException("segment.prev.end > segment.start: overlapping segments (corrupted)");
                } else if (prev.getEnd() < seg.getOffset()) {
                    throw new IllegalStateException("segment.prev.end < segment.start: not truly connected (sparsity error)");
                }

                if (prev.isFree() && prev.getPrev() != null) {
                    if (prev.getPrev().isFree()) {
                        throw new IllegalStateException("segment.free && segment.prev.free: not merged consecutive segments");
                    }
                }
            }

            seg = next;
        }

        if (this.used < 0) {
            throw new IllegalStateException("arena.used < 0: failure to track");
        } else if (this.used > this.capacity) {
            throw new IllegalStateException("arena.used > arena.capacity: failure to track");
        }

        if (this.used != used) {
            throw new IllegalStateException("arena.used is invalid");
        }
    }


    private int toBytes(int index) {
        return index * this.stride;
    }

    private int toElements(int bytes) {
        return bytes / this.stride;
    }
}
