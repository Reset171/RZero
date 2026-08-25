package ru.reset.rzero.client.cache;



public final class SpatialGrid<T> {

    public final int centerX;
    public final int centerZ;
    public final int radius;

    private final int width;
    private final Object[] data;

    public SpatialGrid(int centerX, int centerZ, int radius) {
        if (radius < 0) throw new IllegalArgumentException("radius must be >= 0, got " + radius);
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.radius = radius;
        this.width = 2 * radius + 1;
        this.data = new Object[width * width];
    }

    public T get(int x, int z) {
        int lx = x - centerX + radius;
        int lz = z - centerZ + radius;
        if ((lx | lz) < 0 || lx >= width || lz >= width) return null;
        return (T) data[lx + lz * width];
    }

    public void set(int x, int z, T value) {
        int lx = x - centerX + radius;
        int lz = z - centerZ + radius;
        if ((lx | lz) < 0 || lx >= width || lz >= width) return;
        data[lx + lz * width] = value;
    }

    public int width() { return width; }
}
