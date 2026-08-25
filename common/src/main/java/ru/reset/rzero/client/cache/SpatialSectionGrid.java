package ru.reset.rzero.client.cache;



public final class SpatialSectionGrid<T> {

    public final int centerX;
    public final int centerZ;
    public final int radius;
    public final int minY;
    public final int maxY;

    private final int width;
    private final int height;
    private final int planeStride;
    private final Object[] data;

    public SpatialSectionGrid(int centerX, int centerZ, int radius, int minY, int maxY) {
        if (radius < 0) throw new IllegalArgumentException("radius must be >= 0, got " + radius);
        if (maxY <= minY) throw new IllegalArgumentException("maxY must be > minY, got [" + minY + "," + maxY + ")");
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.radius = radius;
        this.minY = minY;
        this.maxY = maxY;
        this.width = 2 * radius + 1;
        this.height = maxY - minY;
        this.planeStride = width * width;
        this.data = new Object[planeStride * height];
    }

    public T get(int x, int y, int z) {
        int lx = x - centerX + radius;
        int lz = z - centerZ + radius;
        int ly = y - minY;
        if ((lx | lz | ly) < 0 || lx >= width || lz >= width || ly >= height) return null;
        return (T) data[lx + lz * width + ly * planeStride];
    }

    public void set(int x, int y, int z, T value) {
        int lx = x - centerX + radius;
        int lz = z - centerZ + radius;
        int ly = y - minY;
        if ((lx | lz | ly) < 0 || lx >= width || lz >= width || ly >= height) return;
        data[lx + lz * width + ly * planeStride] = value;
    }

    public void clearColumn(int x, int z) {
        int lx = x - centerX + radius;
        int lz = z - centerZ + radius;
        if ((lx | lz) < 0 || lx >= width || lz >= width) return;
        int base = lx + lz * width;
        for (int ly = 0; ly < height; ly++) {
            data[base + ly * planeStride] = null;
        }
    }

    public int width() { return width; }
    public int height() { return height; }
}
