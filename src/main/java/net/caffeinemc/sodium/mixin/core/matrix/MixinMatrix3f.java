package net.caffeinemc.sodium.mixin.core.matrix;

import net.caffeinemc.sodium.interop.vanilla.math.matrix.Matrix3fExtended;
import net.caffeinemc.sodium.util.packed.Normal3b;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.joml.Math;

@Mixin(Matrix3f.class)
public class MixinMatrix3f implements Matrix3fExtended {
    @Shadow
    protected float a00;

    @Shadow
    protected float a10;

    @Shadow
    protected float a20;

    @Shadow
    protected float a01;

    @Shadow
    protected float a11;

    @Shadow
    protected float a21;

    @Shadow
    protected float a02;

    @Shadow
    protected float a12;

    @Shadow
    protected float a22;

    @Override
    public float transformVecX(float x, float y, float z) {
        return Math.fma(this.a00, x, Math.fma(this.a01, y, this.a02 * z));
    }

    @Override
    public float transformVecY(float x, float y, float z) {
        return Math.fma(this.a10, x, Math.fma(this.a11, y, this.a12 * z));
    }

    @Override
    public float transformVecZ(float x, float y, float z) {
        return Math.fma(this.a20, x, Math.fma(this.a21, y, this.a22 * z));
    }

    @Override
    public void rotate(Quaternion quaternion) {
        boolean x = quaternion.getX() != 0.0F;
        boolean y = quaternion.getY() != 0.0F;
        boolean z = quaternion.getZ() != 0.0F;

        // Try to determine if this is a simple rotation on one axis component only
        if (x) {
            if (!y && !z) {
                this.rotateX(quaternion);
            } else {
                this.rotateXYZ(quaternion);
            }
        } else if (y) {
            if (!z) {
                this.rotateY(quaternion);
            } else {
                this.rotateXYZ(quaternion);
            }
        } else if (z) {
            this.rotateZ(quaternion);
        }
    }

    @Override
    public int computeNormal(Direction dir) {
        Vec3i faceNorm = dir.getVector();

        float x = faceNorm.getX();
        float y = faceNorm.getY();
        float z = faceNorm.getZ();

        float x2 = Math.fma(this.a00, x, Math.fma(this.a01, y, this.a02 * z));
        float y2 = Math.fma(this.a10, x, Math.fma(this.a11, y, this.a12 * z));
        float z2 = Math.fma(this.a20, x, Math.fma(this.a21, y, this.a22 * z));

        return Normal3b.pack(x2, y2, z2);
    }

    private void rotateX(Quaternion quaternion) {
        float x = quaternion.getX();
        float w = quaternion.getW();

        float xx = 2.0F * x * x;

        float ta11 = 1.0F - xx;
        float ta22 = 1.0F - xx;

        float xw = x * w;
        float ta21 = 2.0F * xw;
        float ta12 = 2.0F * -xw;

        float a01 = Math.fma(this.a01, ta11, this.a02 * ta21);
        float a02 = Math.fma(this.a01, ta12, this.a02 * ta22);
        float a11 = Math.fma(this.a11, ta11, this.a12 * ta21);
        float a12 = Math.fma(this.a11, ta12, this.a12 * ta22);
        float a21 = Math.fma(this.a21, ta11, this.a22 * ta21);
        float a22 = Math.fma(this.a21, ta12, this.a22 * ta22);

        this.a01 = a01;
        this.a02 = a02;
        this.a11 = a11;
        this.a12 = a12;
        this.a21 = a21;
        this.a22 = a22;
    }

    private void rotateY(Quaternion quaternion) {
        float y = quaternion.getY();
        float w = quaternion.getW();

        float yy = 2.0F * y * y;

        float ta00 = 1.0F - yy;
        float ta22 = 1.0F - yy;

        float yw = y * w;

        float ta20 = 2.0F * (-yw);
        float ta02 = 2.0F * (+yw);

        float a00 = Math.fma(this.a00, ta00, this.a02 * ta20);
        float a02 = Math.fma(this.a00, ta02, this.a02 * ta22);
        float a10 = Math.fma(this.a10, ta00, this.a12 * ta20);
        float a12 = Math.fma(this.a10, ta02, this.a12 * ta22);
        float a20 = Math.fma(this.a20, ta00, this.a22 * ta20);
        float a22 = Math.fma(this.a20, ta02, this.a22 * ta22);

        this.a00 = a00;
        this.a02 = a02;
        this.a10 = a10;
        this.a12 = a12;
        this.a20 = a20;
        this.a22 = a22;
    }

    private void rotateZ(Quaternion quaternion) {
        float z = quaternion.getZ();
        float w = quaternion.getW();

        float zz = 2.0F * z * z;

        float ta00 = 1.0F - zz;
        float ta11 = 1.0F - zz;

        float zw = z * w;

        float ta10 = 2.0F * (0.0F + zw);
        float ta01 = 2.0F * (0.0F - zw);

        float a00 = Math.fma(this.a00, ta00, this.a01 * ta10);
        float a01 = Math.fma(this.a00, ta01, this.a01 * ta11);
        float a10 = Math.fma(this.a10, ta00, this.a11 * ta10);
        float a11 = Math.fma(this.a10, ta01, this.a11 * ta11);
        float a20 = Math.fma(this.a20, ta00, this.a21 * ta10);
        float a21 = Math.fma(this.a20, ta01, this.a21 * ta11);

        this.a00 = a00;
        this.a01 = a01;
        this.a10 = a10;
        this.a11 = a11;
        this.a20 = a20;
        this.a21 = a21;
    }

    private void rotateXYZ(Quaternion quaternion) {
        float x = quaternion.getX();
        float y = quaternion.getY();
        float z = quaternion.getZ();
        float w = quaternion.getW();

        float xx = 2.0F * x * x;
        float yy = 2.0F * y * y;
        float zz = 2.0F * z * z;

        float ta00 = 1.0F - yy - zz;
        float ta11 = 1.0F - zz - xx;
        float ta22 = 1.0F - xx - yy;

        float xy = x * y;
        float yz = y * z;
        float zx = z * x;
        float xw = x * w;
        float yw = y * w;
        float zw = z * w;

        float ta10 = 2.0F * (xy + zw);
        float ta01 = 2.0F * (xy - zw);
        float ta20 = 2.0F * (zx - yw);
        float ta02 = 2.0F * (zx + yw);
        float ta21 = 2.0F * (yz + xw);
        float ta12 = 2.0F * (yz - xw);

        float a00 = Math.fma(this.a00, ta00, Math.fma(this.a01, ta10, this.a02 * ta20));
        float a01 = Math.fma(this.a00, ta01, Math.fma(this.a01, ta11, this.a02 * ta21));
        float a02 = Math.fma(this.a00, ta02, Math.fma(this.a01, ta12, this.a02 * ta22));
        float a10 = Math.fma(this.a10, ta00, Math.fma(this.a11, ta10, this.a12 * ta20));
        float a11 = Math.fma(this.a10, ta01, Math.fma(this.a11, ta11, this.a12 * ta21));
        float a12 = Math.fma(this.a10, ta02, Math.fma(this.a11, ta12, this.a12 * ta22));
        float a20 = Math.fma(this.a20, ta00, Math.fma(this.a21, ta10, this.a22 * ta20));
        float a21 = Math.fma(this.a20, ta01, Math.fma(this.a21, ta11, this.a22 * ta21));
        float a22 = Math.fma(this.a20, ta02, Math.fma(this.a21, ta12, this.a22 * ta22));

        this.a00 = a00;
        this.a01 = a01;
        this.a02 = a02;
        this.a10 = a10;
        this.a11 = a11;
        this.a12 = a12;
        this.a20 = a20;
        this.a21 = a21;
        this.a22 = a22;
    }


    @Override
    public float getA00() {
        return this.a00;
    }

    @Override
    public void setA00(float value) {
        this.a00 = value;
    }

    @Override
    public float getA10() {
        return this.a10;
    }

    @Override
    public void setA10(float value) {
        this.a10 = value;
    }

    @Override
    public float getA20() {
        return this.a20;
    }

    @Override
    public void setA20(float value) {
        this.a20 = value;
    }

    @Override
    public float getA01() {
        return this.a01;
    }

    @Override
    public void setA01(float value) {
        this.a01 = value;
    }

    @Override
    public float getA11() {
        return this.a11;
    }

    @Override
    public void setA11(float value) {
        this.a11 = value;
    }

    @Override
    public float getA21() {
        return this.a21;
    }

    @Override
    public void setA21(float value) {
        this.a21 = value;
    }

    @Override
    public float getA02() {
        return this.a02;
    }

    @Override
    public void setA02(float value) {
        this.a02 = value;
    }

    @Override
    public float getA12() {
        return this.a12;
    }

    @Override
    public void setA12(float value) {
        this.a12 = value;
    }

    @Override
    public float getA22() {
        return this.a22;
    }

    @Override
    public void setA22(float value) {
        this.a22 = value;
    }
}
