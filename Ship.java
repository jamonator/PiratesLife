import java.awt.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.awt.Point;
import java.awt.geom.AffineTransform;

public class Ship {
    int x, y;
    int size = 16;
    int health = 10;
    int maxHealth = 10; // or whatever your max health is
    int cooldown = 0;
    Direction dir;
    private int tick = 0;
    private static final Random rand = new Random();

    // Cannon flash timing
    private int cannonFlashTick = 0;
    private int firingCannon = -1; // Index of the cannon that is firing

    // Wake trail system
    private static class WakeSegment {
        int x, y;

        WakeSegment(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    private LinkedList<WakeSegment> wakeTrail = new LinkedList<>();
    private static final int WAKE_MAX = 20;

    private boolean attackMode = false;

    public Ship(int x, int y, List<Island> islands) {
        // Ensure ship does not spawn on an island or too close to border
        boolean valid;
        do {
            valid = true;
            this.x = x;
            this.y = y;
            // Check border
            if (x - size / 2 < 16 || x + size / 2 > GamePanel.WIDTH - 16 ||
                y - size / 2 < 16 || y + size / 2 > GamePanel.HEIGHT - 16) {
                valid = false;
            }
            // Check islands
            for (Island island : islands) {
                int dx = x - island.x;
                int dy = y - island.y;
                int minDist = size / 2 + island.radius + 24;
                if (dx * dx + dy * dy < minDist * minDist) {
                    valid = false;
                    break;
                }
            }
            if (!valid) {
                x = 32 + rand.nextInt(GamePanel.WIDTH - 64);
                y = 32 + rand.nextInt(GamePanel.HEIGHT - 64);
            }
        } while (!valid);
        this.dir = Direction.random();
    }

    public void update(List<Ship> ships, List<Cannonball> cannonballs, List<Island> islands, List<HealthDrop> healthDrops) {
        tick++;

        // Health management: move towards nearest health drop if health is under 80%
        if (this.health < 0.8 * this.maxHealth) {
            HealthDrop nearest = findNearestHealthDrop(healthDrops, 400); // 400px range, adjust as needed
            if (nearest != null) {
                // Move toward the health drop
                double angle = Math.atan2(nearest.y - this.y, nearest.x - this.x);
                this.x += (int)(Math.cos(angle) * 2); // speed is 2 for health seeking
                this.y += (int)(Math.sin(angle) * 2);
                return; // Skip normal AI for this frame
            }
        }

        // Find nearest enemy ship
        Ship target = null;
        double minDist = Double.MAX_VALUE;
        for (Ship other : ships) {
            if (other == this) continue;
            double dist = Math.hypot(other.x - x, other.y - y);
            if (dist < minDist) {
                minDist = dist;
                target = other;
            }
        }

        // Enter attack mode if a ship is in range, else wander randomly
        if (target != null && minDist < 120) {
            attackMode = true;
            int dx = target.x - x;
            int dy = target.y - y;

            // If too far from target, move closer
            if (minDist > 80) {
                // Move toward the target
                if (Math.abs(dx) > Math.abs(dy)) {
                    dir = dx > 0 ? Direction.EAST : Direction.WEST;
                } else {
                    dir = dy > 0 ? Direction.SOUTH : Direction.NORTH;
                }
            } else if (minDist < 48) {
                // If too close, move away
                if (Math.abs(dx) > Math.abs(dy)) {
                    dir = dx > 0 ? Direction.WEST : Direction.EAST;
                } else {
                    dir = dy > 0 ? Direction.NORTH : Direction.SOUTH;
                }
            } else {
                // In range: move randomly, but change direction sometimes
                if (dir == null || rand.nextInt(20) == 0) {
                    dir = Direction.random();
                }
            }
        } else {
            // Wander randomly if not in attack mode
            if (attackMode || dir == null || rand.nextInt(60) == 0) {
                dir = Direction.random();
            }
            attackMode = false;
        }

        move(islands);

        if (cooldown > 0) cooldown--;

        // Only shoot if in attack mode and in range
        if (attackMode && target != null && minDist < 100 && cooldown == 0) {
            cannonballs.add(new Cannonball(x, y, target));
            cooldown = 30;
            cannonFlashTick = 5;
            firingCannon = rand.nextInt(4);
        }

        if (cannonFlashTick > 0) cannonFlashTick--;
        else firingCannon = -1; // Reset after flash ends
    }

    public void move(List<Island> islands) {
        int nextX = x + dir.dx;
        int nextY = y + dir.dy;

        // Check collision with islands
        boolean collides = false;
        for (Island island : islands) {
            int dx = nextX - island.x;
            int dy = nextY - island.y;
            int minDist = size / 2 + island.radius;
            if (dx * dx + dy * dy < minDist * minDist) {
                collides = true;
                break;
            }
        }

        // Check world borders (assuming GamePanel.WIDTH/HEIGHT)
        if (nextX - size / 2 < 0 || nextX + size / 2 > GamePanel.WIDTH ||
            nextY - size / 2 < 0 || nextY + size / 2 > GamePanel.HEIGHT) {
            collides = true;
        }

        if (!collides) {
            x = nextX;
            y = nextY;

            // Add a wake segment at the new position
            wakeTrail.addFirst(new WakeSegment(x, y));
            if (wakeTrail.size() > WAKE_MAX) {
                wakeTrail.removeLast();
            }
        } else {
            // Pick a new random direction if blocked by an island or border
            dir = Direction.random();
        }
    }

    public void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();

        int px = x - size / 2;
        int py = y - size / 2;

        // Draw wake trail (optional, keep as is or shrink)
        for (int i = 0; i < wakeTrail.size(); i++) {
            WakeSegment seg = wakeTrail.get(i);
            float alpha = 1.0f - (float)i / WAKE_MAX;
            int width = 3;
            int height = 1;
            Color swellColor = new Color(120, 180, 230, (int)(alpha * 100));
            g2.setColor(swellColor);
            g2.fillRect(seg.x - width / 2, seg.y - height / 2, width, height);
        }

        // Pixel-art ship body (brown hull)
        g2.setColor(new Color(139, 69, 19));
        g2.fillRect(px + 3, py + 7, 10, 4);

        // Bow (front, lighter brown)
        g2.setColor(new Color(181, 101, 29));
        g2.fillRect(px + 2, py + 8, 2, 2);

        // Stern (back, darker brown)
        g2.setColor(new Color(100, 50, 10));
        g2.fillRect(px + 12, py + 8, 2, 2);

        // Deck (lighter stripe)
        g2.setColor(new Color(205, 133, 63));
        g2.fillRect(px + 5, py + 9, 6, 1);

        // Mast (gray)
        g2.setColor(new Color(120, 120, 120));
        g2.fillRect(px + 7, py + 4, 2, 5);

        // Sail (white)
        g2.setColor(new Color(240, 240, 240));
        g2.fillRect(px + 6, py + 2, 4, 4);

        // Flag (black)
        g2.setColor(Color.BLACK);
        g2.fillRect(px + 8, py + 1, 3, 1);

        // Cannons (dark gray dots)
        g2.setColor(new Color(60, 60, 60));
        g2.fillRect(px + 4, py + 11, 2, 2);
        g2.fillRect(px + 10, py + 11, 2, 2);

        // Health bar (tiny, above ship)
        g2.setColor(Color.RED);
        g2.fillRect(x - 8, y - size / 2 - 4, 16, 2);
        g2.setColor(Color.GREEN);
        g2.fillRect(x - 8, y - size / 2 - 4, 16 * health / maxHealth, 2);

        g2.dispose();
    }

    // --- New methods for shipwreck and rowboat --

    public void destroy(List<Shipwreck> wrecks, List<Rowboat> rowboats, List<Island> islands) {
        // Spawn a shipwreck at the ship's position
        wrecks.add(new Shipwreck(x, y));
        // Pick a random island for the rowboat to go to
        Island targetIsland = islands.get(rand.nextInt(islands.size()));
        rowboats.add(new Rowboat(x, y, targetIsland, islands));
        // Remove or mark this ship as destroyed
        // (e.g. set a flag or remove from the ships list in GamePanel)
    }

    // --- Shipwreck class with animation ---
    public static class Shipwreck {
        int x, y;
        int life = 600; // frames to stay
        int bobTick = 0;

        public Shipwreck(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public void update() {
            life--;
            bobTick++;
        }

        public void draw(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            int bob = (int)(Math.sin(bobTick * 0.08) * 2);

            // Draw broken hull
            g2.setColor(new Color(90, 60, 40, 200));
            g2.fillRoundRect(x - 14, y - 6 + bob, 28, 12, 8, 8);

            // Draw mast stump
            g2.setColor(new Color(70, 50, 30, 180));
            g2.fillRect(x - 2, y - 18 + bob, 4, 12);

            // Draw some floating planks
            g2.setColor(new Color(140, 110, 60, 120));
            for (int i = 0; i < 3; i++) {
                int px = x - 10 + i * 10;
                int py = y + 6 + (int)(Math.sin(bobTick * 0.12 + i) * 2);
                g2.fillRect(px, py + bob, 8, 2);
            }
        }
    }

    // Minimal Port class definition to resolve the type error
    public static class Port {
        int x, y;
        public Port(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    // --- Rowboat class with animation ---
    public static class Rowboat {
        int x, y;
        int speed = 2;
        Island targetIsland;
        Point targetPort;
        boolean arrived = false;
        Ship respawnShip = null;
        int tick = 0;
        double angle = 0;
        Queue<Point> path = new LinkedList<>();
        boolean pathFound = false;

        public Rowboat(int x, int y, Island targetIsland, List<Island> islands) {
            this.x = x;
            this.y = y;
            this.targetIsland = targetIsland;
            this.targetPort = targetIsland.getPortLocation();
            // Calculate angle for animation
            int dx = targetPort.x - x;
            int dy = targetPort.y - y;
            angle = Math.atan2(dy, dx);
            // Find path around islands
            this.path = findPath(x, y, targetPort.x, targetPort.y, islands);
            this.pathFound = !path.isEmpty();
        }

        public void update() {
            tick++;
            if (arrived) return;

            if (pathFound && !path.isEmpty()) {
                Point next = path.peek();
                int dx = next.x - x;
                int dy = next.y - y;
                double dist = Math.hypot(dx, dy);
                if (dist < speed) {
                    x = next.x;
                    y = next.y;
                    path.poll();
                } else {
                    x += (int)(speed * dx / dist);
                    y += (int)(speed * dy / dist);
                }
                // Update angle for animation
                if (dx != 0 || dy != 0) angle = Math.atan2(dy, dx);
                if (path.isEmpty()) {
                    arrived = true;
                    x = targetPort.x;
                    y = targetPort.y;
                }
            } else {
                // Fallback: direct movement if no path found
                int dx = targetPort.x - x;
                int dy = targetPort.y - y;
                double dist = Math.hypot(dx, dy);
                if (dist < speed) {
                    x = targetPort.x;
                    y = targetPort.y;
                    arrived = true;
                } else {
                    x += (int)(speed * dx / dist);
                    y += (int)(speed * dy / dist);
                }
                if (dx != 0 || dy != 0) angle = Math.atan2(dy, dx);
            }
        }

        public void draw(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.translate(x, y);
            g2.rotate(angle);

            // Bobbing animation
            int bob = (int)(Math.sin(tick * 0.18) * 2);

            // Draw the path the rowboat is following (for debugging)
            g2.setTransform(new AffineTransform()); // Reset transform to draw path in world coords
            g2.setColor(Color.CYAN);
            Point last = new Point(x, y);
            for (Point p : path) {
                g2.drawLine(last.x, last.y, p.x, p.y);
                last = p;
            }

            // Restore transform for drawing the boat
            g2.translate(x, y);
            g2.rotate(angle);

            // Draw boat body (rectangle)
            g2.setColor(new Color(160, 120, 60));
            g2.fillRoundRect(-10, -4 + bob, 20, 8, 8, 8);

            // Draw boat rim
            g2.setColor(new Color(110, 80, 40));
            g2.drawRoundRect(-10, -4 + bob, 20, 8, 8, 8);

            g2.dispose();
        }

        // Add a more robust grid-based BFS pathfinding method:
        private Queue<Point> findPath(int startX, int startY, int endX, int endY, List<Island> islands) {
            int grid = 4; // finer grid for better accuracy
            int w = GamePanel.WIDTH / grid;
            int h = GamePanel.HEIGHT / grid;
            boolean[][] visited = new boolean[w][h];
            Point[][] prev = new Point[w][h];
            Queue<Point> queue = new LinkedList<>();
            int sx = startX / grid, sy = startY / grid;
            int ex = endX / grid, ey = endY / grid;
            queue.add(new Point(sx, sy));
            visited[sx][sy] = true;

            int[] dxs = {1, -1, 0, 0};
            int[] dys = {0, 0, 1, -1};

            while (!queue.isEmpty()) {
                Point p = queue.poll();
                if (p.x == ex && p.y == ey) break;
                for (int d = 0; d < 4; d++) {
                    int nx = p.x + dxs[d];
                    int ny = p.y + dys[d];
                    if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                    if (visited[nx][ny]) continue;
                    int px = nx * grid + grid / 2;
                    int py = ny * grid + grid / 2;
                    boolean blocked = false;
                    for (Island island : islands) {
                        int idx = px - island.x;
                        int idy = py - island.y;
                        int buffer = 18; // increased buffer for rowboat size and safety
                        int minDist = island.radius + buffer;
                        if (idx * idx + idy * idy < minDist * minDist) {
                            blocked = true;
                            break;
                        }
                    }
                    if (!blocked) {
                        visited[nx][ny] = true;
                        prev[nx][ny] = p;
                        queue.add(new Point(nx, ny));
                    }
                }
            }

            // Reconstruct path
            LinkedList<Point> path = new LinkedList<>();
            Point p = new Point(ex, ey);
            if (!visited[ex][ey]) return path; // no path found
            while (p != null && !(p.x == sx && p.y == sy)) {
                path.addFirst(new Point(p.x * grid + grid / 2, p.y * grid + grid / 2));
                p = prev[p.x][p.y];
            }
            return path;
        }
    }

    // --- New method to find nearest health drop ---
    public HealthDrop findNearestHealthDrop(List<HealthDrop> healthDrops, int range) {
        HealthDrop nearest = null;
        int minDistSq = range * range;
        for (HealthDrop drop : healthDrops) {
            int dx = drop.x - this.x;
            int dy = drop.y - this.y;
            int distSq = dx * dx + dy * dy;
            if (distSq < minDistSq) {
                minDistSq = distSq;
                nearest = drop;
            }
        }
        return nearest;
    }
}
