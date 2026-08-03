package wtf.demise.utils.math;

public class Vec3d {
    // 3d vector coordinate fields
    public double x;
    public double y;
    public double z;

    public Vec3d() {
        this(0.0, 0.0, 0.0);
    }

    public Vec3d(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void set(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void add(Vec3d v) {
        this.x += v.x;
        this.y += v.y;
        this.z += v.z;
    }

    public void mul(double scale) {
        this.x *= scale;
        this.y *= scale;
        this.z *= scale;
    }
}
