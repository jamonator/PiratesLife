import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.awt.Point;

public class Island {
    int x, y, radius;
    private int[] outlineX, outlineY;
    private static final Random rand = new Random();

    // Features
    private List<Tree> trees = new ArrayList<>();
    private List<House> houses = new ArrayList<>();
    private Port port;

    // Randomizer constructor (pixel style, random spot)
    public Island(int mapWidth, int mapHeight) {
        // Pixel-style radius (multiple of 8)
        this.radius = 40 + rand.nextInt(5) * 8;
        // Random position, avoid edges
        this.x = radius + rand.nextInt((mapWidth - 2 * radius) / 8) * 8;
        this.y = radius + rand.nextInt((mapHeight - 2 * radius) / 8) * 8;
        generateOutline();
        randomizeFeatures();
    }

    // Manual constructor for fixed islands
    public Island(int x, int y, int radius) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        generateOutline();
        randomizeFeatures();
    }

    // Generate a soft, square, pixel-art outline for the island (Stardew-like, more square)
    private void generateOutline() {
        int points = 16;
        outlineX = new int[points];
        outlineY = new int[points];
        double step = Math.PI * 2 / points;
        for (int i = 0; i < points; i++) {
            double angle = i * step;
            // Square bias: blend between circle and square
            double squareBias = 0.55 + 0.45 * Math.max(Math.abs(Math.cos(angle)), Math.abs(Math.sin(angle)));
            double r = radius * squareBias * (0.95 + 0.08 * rand.nextDouble());
            // Snap to grid for pixel look
            int px = x + (int)(Math.cos(angle) * r);
            int py = y + (int)(Math.sin(angle) * r);
            outlineX[i] = (px / 4) * 4;
            outlineY[i] = (py / 4) * 4;
        }
    }

    // Randomly add trees, houses, and always a port
    private void randomizeFeatures() {
        // Place the port (dock) on a random edge, facing outward from the island center
        int edgePoint = rand.nextInt(outlineX.length);
        int px = outlineX[edgePoint];
        int py = outlineY[edgePoint];

        // Calculate angle from island center to dock (so dock faces out)
        double angle = Math.atan2(py - y, px - x);

        port = new Port(px, py, angle);

        // Keep track of occupied spots (as rectangles)
        List<Rectangle> occupied = new ArrayList<>();
        // Reserve port area
        occupied.add(new Rectangle(px - 20, py - 20, 40, 40));

        // Random houses (try to avoid overlap)
        int houseCount = rand.nextInt(3); // 0, 1, or 2 houses
        for (int i = 0; i < houseCount; i++) {
            boolean placed = false;
            for (int attempt = 0; attempt < 20 && !placed; attempt++) {
                double hAngle = rand.nextDouble() * 2 * Math.PI;
                int hx = x + (int)(Math.cos(hAngle) * radius * 0.4);
                int hy = y + (int)(Math.sin(hAngle) * radius * 0.4);
                hx = (hx / 4) * 4; hy = (hy / 4) * 4;
                Rectangle rect = new Rectangle(hx, hy, 16, 16);
                boolean overlaps = false;
                for (Rectangle occ : occupied) {
                    if (rect.intersects(occ)) {
                        overlaps = true;
                        break;
                    }
                }
                if (!overlaps) {
                    houses.add(new House(hx, hy));
                    occupied.add(rect);
                    placed = true;
                }
            }
        }

        // Random trees (try to avoid overlap)
        int treeCount = 2 + rand.nextInt(4);
        for (int i = 0; i < treeCount; i++) {
            boolean placed = false;
            for (int attempt = 0; attempt < 20 && !placed; attempt++) {
                double tAngle = rand.nextDouble() * 2 * Math.PI;
                int tx = x + (int)(Math.cos(tAngle) * radius * rand.nextDouble() * 0.7);
                int ty = y + (int)(Math.sin(tAngle) * radius * rand.nextDouble() * 0.7);
                tx = (tx / 4) * 4; ty = (ty / 4) * 4;
                int tw = 12 + rand.nextInt(2) * 4, th = 16 + rand.nextInt(2) * 4;
                Rectangle rect = new Rectangle(tx, ty, tw, th);
                boolean overlaps = false;
                for (Rectangle occ : occupied) {
                    if (rect.intersects(occ)) {
                        overlaps = true;
                        break;
                    }
                }
                if (!overlaps) {
                    trees.add(new Tree(tx, ty));
                    occupied.add(rect);
                    placed = true;
                }
            }
        }
    }

    public void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        // Snap island center to 16x16 grid
        int gx = (x / 16) * 16;
        int gy = (y / 16) * 16;
        int gr = (radius / 16) * 16;

        // 1. Water outline (snapped)
        g2.setColor(new Color(80, 140, 200));
        g2.fillRect(gx - gr - 16, gy - gr - 16, (gr + 16) * 2, (gr + 16) * 2);

        // 2. Main sand body (blocky, snapped)
        g2.setColor(new Color(222, 202, 142));
        for (int dx = -gr; dx < gr; dx += 16) {
            for (int dy = -gr; dy < gr; dy += 16) {
                int dist = (int)Math.sqrt(dx*dx + dy*dy);
                if (dist < gr - 8) {
                    g2.fillRect(gx + dx, gy + dy, 16, 16);
                }
            }
        }

        // 3. Grass border (blocky, snapped)
        g2.setColor(new Color(106, 190, 48));
        for (int dx = -gr; dx < gr; dx += 16) {
            for (int dy = -gr; dy < gr; dy += 16) {
                int dist = (int)Math.sqrt(dx*dx + dy*dy);
                if (dist >= gr - 32 && dist < gr - 8) {
                    g2.fillRect(gx + dx, gy + dy, 16, 16);
                }
            }
        }

        // 4. Pixel-art rocks (snapped)
        g2.setColor(new Color(120, 120, 120));
        g2.fillRect(gx - 16, gy + 16, 8, 8);
        g2.fillRect(gx + 16, gy - 16, 8, 8);

        // 5. Pixel-art palm (snapped)
        g2.setColor(new Color(139, 69, 19));
        g2.fillRect(gx + 16, gy - 32, 8, 16); // trunk
        g2.setColor(new Color(106, 190, 48));
        g2.fillRect(gx + 8, gy - 40, 16, 8); // leaves

        // 6. Draw port (snapped)
        port.draw(g2);

        // 7. Draw houses (snapped)
        for (House house : houses) house.draw(g2);

        // 8. Draw trees (snapped)
        for (Tree tree : trees) tree.draw(g2);
    }

    public Point getPortLocation() {
        return new Point(port.x, port.y);
    }

    // --- Feature classes ---

    private static class Tree {
        int x, y;
        Tree(int x, int y) {
            this.x = (x / 16) * 16;
            this.y = (y / 16) * 16;
        }
        void draw(Graphics2D g2) {
            // Trunk (2x6)
            g2.setColor(new Color(120, 80, 40));
            g2.fillRect(x + 7, y + 10, 2, 6);
            // Leaves (12x8)
            g2.setColor(new Color(70, 160, 70));
            g2.fillRect(x + 2, y + 2, 12, 8);
            g2.setColor(new Color(100, 200, 100));
            g2.fillRect(x + 4, y + 4, 8, 4);
            g2.setColor(new Color(180, 255, 180));
            g2.fillRect(x + 7, y + 5, 2, 2);
        }
    }

    private static class House {
        int x, y;
        Color wall, roof;
        House(int x, int y) {
            this.x = (x / 16) * 16;
            this.y = (y / 16) * 16;
            wall = new Color(210, 180, 140);
            roof = new Color(180, 60, 60);
        }
        void draw(Graphics2D g2) {
            // Walls (12x8)
            g2.setColor(wall);
            g2.fillRect(x + 2, y + 6, 12, 8);
            // Roof (12x4)
            g2.setColor(roof);
            g2.fillRect(x + 2, y + 2, 12, 4);
            // Door (3x4)
            g2.setColor(new Color(90, 60, 40));
            g2.fillRect(x + 7, y + 10, 3, 4);
            // Window (3x3)
            g2.setColor(new Color(180, 220, 255));
            g2.fillRect(x + 4, y + 8, 3, 3);
        }
    }

    private static class Port {
        int x, y;
        double angle;
        Port(int x, int y, double angle) {
            // Snap to 16x16 grid
            this.x = (x / 16) * 16;
            this.y = (y / 16) * 16;
            this.angle = angle;
        }
        void draw(Graphics2D g2) {
            int dockW = 32; // twice the ship width
            int dockH = 10; // taller for a bigger dock
            int postW = 4, postH = 10;

            AffineTransform old = g2.getTransform();
            g2.translate(x, y);
            g2.rotate(angle);

            // Dock body (blocky, Stardew style)
            g2.setColor(new Color(140, 110, 60));
            g2.fillRect(-dockW / 2, 0, dockW, dockH);

            // Dock posts (blocky, grid-aligned)
            g2.setColor(new Color(110, 80, 40));
            for (int i = -dockW / 2; i < dockW / 2; i += 8) {
                g2.fillRect(i, dockH - 2, postW, postH);
            }

            // Plank lines (lighter color, grid-aligned)
            g2.setColor(new Color(180, 140, 80));
            for (int i = -dockW / 2; i < dockW / 2; i += 8) {
                g2.drawLine(i + 2, 1, i + 2, dockH - 2);
            }

            g2.setTransform(old);
        }
    }
}
