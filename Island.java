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
                    trees.add(new Tree(tx, ty, tw, th));
                    occupied.add(rect);
                    placed = true;
                }
            }
        }
    }

    public void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        // Draw sand base (irregular, pixel style)
        g2.setColor(new Color(224, 202, 142));
        Polygon sand = new Polygon(outlineX, outlineY, outlineX.length);
        g2.fillPolygon(sand);

        // Draw grass patch (smaller, also pixel style, more blobby)
        int points = outlineX.length;
        int[] grassX = new int[points];
        int[] grassY = new int[points];
        for (int i = 0; i < points; i++) {
            double dx = outlineX[i] - x;
            double dy = outlineY[i] - y;
            grassX[i] = x + (int)(dx * 0.7);
            grassY[i] = y + (int)(dy * 0.7);
            grassX[i] = (grassX[i] / 4) * 4;
            grassY[i] = (grassY[i] / 4) * 4;
        }
        g2.setColor(new Color(120, 180, 90));
        g2.fillPolygon(grassX, grassY, points);

        // Draw port (always present)
        port.draw(g2);

        // Draw houses
        for (House house : houses) house.draw(g2);

        // Draw trees
        for (Tree tree : trees) tree.draw(g2);
    }

    public Point getPortLocation() {
        return new Point(port.x, port.y);
    }

    // --- Feature classes ---

    private static class Tree {
        int x, y, w, h;
        Tree(int x, int y, int w, int h) {
            this.x = x; this.y = y; this.w = w; this.h = h;
        }
        void draw(Graphics2D g2) {
            // Trunk (pixel style)
            g2.setColor(new Color(120, 80, 40));
            g2.fillRect(x + w / 2 - 2, y + h - 6, 4, 8);
            // Leaves (blocky, Stardew-like)
            g2.setColor(new Color(70, 160, 70));
            g2.fillRect(x, y, w, h);
            g2.setColor(new Color(100, 200, 100));
            g2.fillRect(x + 2, y + 2, w - 4, h - 4);
        }
    }

    private static class House {
        int x, y;
        Color wall, roof;
        House(int x, int y) {
            this.x = x; this.y = y;
            wall = new Color(200 + rand.nextInt(30), 170 + rand.nextInt(30), 120 + rand.nextInt(30));
            roof = rand.nextBoolean() ? new Color(180, 60, 60) : new Color(60, 120, 180);
        }
        void draw(Graphics2D g2) {
            g2.setColor(wall);
            g2.fillRect(x, y, 12, 12);
            g2.setColor(roof);
            g2.fillRect(x, y - 4, 12, 6);
            g2.setColor(new Color(90, 60, 40));
            g2.fillRect(x + 4, y + 6, 4, 6);
        }
    }

    private static class Port {
        int x, y;
        double angle;
        Port(int x, int y, double angle) {
            this.x = x; this.y = y;
            this.angle = angle;
        }
        void draw(Graphics2D g2) {
            int dockW = 28, dockH = 10;

            // Save the original transform
            AffineTransform old = g2.getTransform();

            // Draw dock rotated to face outward (away from island center)
            g2.translate(x, y);
            g2.rotate(angle); // angle already points from center to dock (outward)
            g2.setColor(new Color(140, 110, 60));
            g2.fillRect(-dockW / 2, 0, dockW, dockH);
            g2.setColor(new Color(110, 80, 40));
            for (int i = 0; i < dockW; i += 7) {
                g2.fillRect(-dockW / 2 + i, dockH - 2, 3, 6); // posts
            }
            g2.setColor(new Color(180, 140, 80));
            for (int i = 0; i < dockW; i += 7) {
                g2.drawLine(-dockW / 2 + i, 0, -dockW / 2 + i, dockH);
            }

            // Restore the original transform
            g2.setTransform(old);
        }
    }
}