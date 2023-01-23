package me.jellysquid.mods.sodium.client.render.vertex.formats;

import me.jellysquid.mods.sodium.client.render.vertex.VertexFormatRegistry;
import me.jellysquid.mods.sodium.client.render.vertex.VertexFormatDescription;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

public final class ColorVertex {
    public static final VertexFormatDescription FORMAT = VertexFormatRegistry.get(VertexFormats.POSITION_COLOR);

    public static final int STRIDE = 16;

    private static final int OFFSET_POSITION = 0;
    private static final int OFFSET_COLOR = 12;

    public static void write(long ptr, Matrix4f matrix, float x, float y, float z, int color) {
        float x2 = (matrix.m00() * x) + (matrix.m10() * y) + (matrix.m20() * z) + matrix.m30();
        float y2 = (matrix.m01() * x) + (matrix.m11() * y) + (matrix.m21() * z) + matrix.m31();
        float z2 = (matrix.m02() * x) + (matrix.m12() * y) + (matrix.m22() * z) + matrix.m32();

        write(ptr, x2, y2, z2, color);
    }

    public static void write(long ptr, float x, float y, float z, int color) {
        MemoryUtil.memPutFloat(ptr + OFFSET_POSITION + 0, x);
        MemoryUtil.memPutFloat(ptr + OFFSET_POSITION + 4, y);
        MemoryUtil.memPutFloat(ptr + OFFSET_POSITION + 8, z);

        MemoryUtil.memPutInt(ptr + OFFSET_COLOR + 0, color);
    }

}
