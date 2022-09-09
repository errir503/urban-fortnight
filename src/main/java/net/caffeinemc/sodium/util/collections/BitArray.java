package net.caffeinemc.sodium.util.collections;

import java.util.Arrays;
import net.caffeinemc.gfx.util.misc.MathUtil;

public class BitArray {
    private static final int ADDRESS_BITS_PER_WORD = 6;
    private static final int BITS_PER_WORD = 1 << ADDRESS_BITS_PER_WORD;
    private static final int BIT_INDEX_MASK = BITS_PER_WORD - 1;
    private static final long WORD_MASK = 0xFFFFFFFFFFFFFFFFL;

    private final long[] words;
    private final int count;

    public BitArray(int count) {
        this.words = new long[(MathUtil.align(count, BITS_PER_WORD) >> ADDRESS_BITS_PER_WORD)];
        this.count = count;
    }

    public boolean get(int index) {
        return (this.words[wordIndex(index)] & 1L << bitIndex(index)) != 0;
    }

    public void set(int index) {
        this.words[wordIndex(index)] |= 1L << bitIndex(index);
    }
    
    public void unset(int index) {
        this.words[wordIndex(index)] &= ~(1L << bitIndex(index));
    }
    
    public void put(int index, boolean value) {
        int wordIndex = wordIndex(index);
        int bitIndex = bitIndex(index);
        long intValue = value ? 1 : 0;
        this.words[wordIndex] = (this.words[wordIndex] & ~(1L << bitIndex)) | (intValue << bitIndex);
    }

    /**
     * Sets the bits from startIdx (inclusive) to endIdx (exclusive) to 1
     */
    public void set(int startIdx, int endIdx) {
        int startWordIndex = wordIndex(startIdx);
        int endWordIndex = wordIndex(endIdx - 1);
    
        long firstWordMask = WORD_MASK << startIdx;
        long lastWordMask = WORD_MASK >>> -endIdx;
        if (startWordIndex == endWordIndex) {
            this.words[startWordIndex] |= (firstWordMask & lastWordMask);
        } else {
            this.words[startWordIndex] |= firstWordMask;
            
            for (int i = startWordIndex + 1; i < endWordIndex; i++) {
                this.words[i] = 0xFFFFFFFFFFFFFFFFL;
            }
            
            this.words[endWordIndex] |= lastWordMask;
        }
    }
    
    /**
     * Sets the bits from startIdx (inclusive) to endIdx (exclusive) to 0
     */
    public void unset(int startIdx, int endIdx) {
        int startWordIndex = wordIndex(startIdx);
        int endWordIndex = wordIndex(endIdx - 1);
        
        long firstWordMask = ~(WORD_MASK << startIdx);
        long lastWordMask = ~(WORD_MASK >>> -endIdx);
        if (startWordIndex == endWordIndex) {
            this.words[startWordIndex] &= (firstWordMask & lastWordMask);
        } else {
            this.words[startWordIndex] &= firstWordMask;
            
            for (int i = startWordIndex + 1; i < endWordIndex; i++) {
                this.words[i] = 0x0000000000000000L;
            }
            
            this.words[endWordIndex] &= lastWordMask;
        }
    }
    
    public void copy(BitArray src, int startIdx, int endIdx) {
        int startWordIndex = wordIndex(startIdx);
        int endWordIndex = wordIndex(endIdx - 1);

        long firstWordMask = WORD_MASK << startIdx;
        long lastWordMask = WORD_MASK >>> -endIdx;
        if (startWordIndex == endWordIndex) {
            long combinedMask = firstWordMask & lastWordMask;
            long invCombinedMask = ~combinedMask;
            this.words[startWordIndex] = (this.words[startWordIndex] & invCombinedMask) | (src.words[startWordIndex] & combinedMask);
        } else {
            long invFirstWordMask = ~firstWordMask;
            long invLastWordMask = ~lastWordMask;

            this.words[startWordIndex] = (this.words[startWordIndex] & invFirstWordMask) | (src.words[startWordIndex] & firstWordMask);

            int length = endWordIndex - (startWordIndex + 1);
            if (length > 0) {
                System.arraycopy(
                        src.words,
                        startWordIndex + 1,
                        this.words,
                        startWordIndex + 1,
                        length
                );
            }

            this.words[endWordIndex] = (this.words[endWordIndex] & invLastWordMask) | (src.words[endWordIndex] & lastWordMask);
        }
    }
    
    public void copy(BitArray src, int index) {
        int wordIndex = wordIndex(index);
        long invBitMask = 1L << bitIndex(index);
        long bitMask = ~invBitMask;
        this.words[wordIndex] = (this.words[wordIndex] & bitMask) | (src.words[wordIndex] & invBitMask);
    }

    private static int wordIndex(int index) {
        return index >> ADDRESS_BITS_PER_WORD;
    }

    private static int bitIndex(int index) {
        return index & BIT_INDEX_MASK;
    }

    public void fill(boolean value) {
        Arrays.fill(this.words, value ? 0xFFFFFFFFFFFFFFFFL : 0x0000000000000000L);
    }

    public int count() {
        int sum = 0;

        for (long word : this.words) {
            sum += Long.bitCount(word);
        }

        return sum;
    }

    public int capacity() {
        return this.count;
    }

    public boolean getAndUnset(int index) {
        var wordIndex = wordIndex(index);
        var bit = 1L << bitIndex(index);

        var word = this.words[wordIndex];
        this.words[wordIndex] = word & ~bit;

        return (word & bit) != 0;
    }
    
    public int nextSetBit(int fromIndex) {
        int u = wordIndex(fromIndex);
        
        long word = this.words[u] & (WORD_MASK << fromIndex);
        
        while (true) {
            if (word != 0) {
                return (u * BITS_PER_WORD) + Long.numberOfTrailingZeros(word);
            }
            
            if (++u == this.words.length) {
                return -1;
            }
            
            word = this.words[u];
        }
    }
}
