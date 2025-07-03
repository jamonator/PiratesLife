import java.awt.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.awt.Point;
import java.awt.geom.AffineTransform;

public class Ship {
    int x, y;
    int size = 16; // was 16, now slightly bigger
    int health = 10;
    int maxHealth = 10;
    int cooldown = 0;
    Direction dir;
    private int tick = 0;
    private static final Random rand = new Random();
    public final Faction faction; // Add this line

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

    public Ship(int x, int y, List<Island> islands, Faction faction) { // Add faction param
        this.faction = faction; // Set faction
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

    // For compatibility, you may want to keep the old constructor for rowboats/respawn:
    public Ship(int x, int y, List<Island> islands) {
        this(x, y, islands, Faction.RED); // Default to RED or random if needed
    }

    public Ship(int x, int y, List<Island> islands, Faction faction, boolean exactSpawn) {
        this.faction = faction;
        this.x = x;
        this.y = y;
        this.dir = Direction.random();
        // No randomization if exactSpawn is true
        if (!exactSpawn) {
            boolean valid;
            do {
                valid = true;
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
        }
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
            if (other.faction == this.faction) continue; // Only attack enemy factions
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

        // Faction outline
        Color factionColor = switch (faction) {
            case RED -> Color.RED;
            case BLUE -> Color.BLUE;
            case GREEN -> Color.GREEN;
            case YELLOW -> Color.YELLOW;
            case PURPLE -> new Color(128, 0, 128);
        };

        // Draw faction color square above the ship
        g2.setColor(factionColor);
        g2.fillRect(x - 4, y - size / 2 - 12, 8, 8);
        g2.setColor(Color.BLACK);
        g2.drawRect(x - 4, y - size / 2 - 12, 8, 8);

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

        // Flag (faction color)
        g2.setColor(factionColor);
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
        // Find the island with the same faction as this ship
        Island targetIsland = null;
        for (Island island : islands) {
            if (island.faction == this.faction) {
                targetIsland = island;
                break;
            }
        }
        if (targetIsland != null) {
            rowboats.add(new Rowboat(x, y, targetIsland, islands, this.faction));
        }
        // Remove or mark this ship as destroyed (handled in GamePanel)
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
        Faction faction;
        List<Point> waypoints = new LinkedList<>();

        public Rowboat(int x, int y, Island targetIsland, List<Island> islands, Faction faction) {
            this.x = x;
            this.y = y;
            this.targetIsland = targetIsland;
            this.faction = faction;
            this.targetPort = targetIsland.getPortLocation();
            // Start with a direct path
            waypoints.add(new Point(targetPort.x, targetPort.y));
            // Optionally, you could add more advanced pathfinding here
        }

        public void update(List<Ship> ships, List<Island> islands) {
            if (arrived || waypoints.isEmpty()) return;

            Point next = waypoints.get(0);
            int dx = next.x - x;
            int dy = next.y - y;
            double dist = Math.hypot(dx, dy);

            // Try to move in small steps to avoid skipping through islands
            int steps = (int)Math.ceil(dist / speed);
            boolean blocked = false;
            int nx = x, ny = y;

            for (int i = 1; i <= steps; i++) {
                int testX = x + (int)Math.round(dx * i / steps);
                int testY = y + (int)Math.round(dy * i / steps);

                for (Island island : islands) {
                    if (island == targetIsland) continue;
                    int minDist = 10 + island.radius;
                    int idx = testX - island.x;
                    int idy = testY - island.y;
                    if (idx * idx + idy * idy < minDist * minDist) {
                        blocked = true;
                        // Generate a detour waypoint to the left or right of the island
                        double angle = Math.atan2(dy, dx);
                        double detourAngle = angle + (Math.random() > 0.5 ? Math.PI / 2 : -Math.PI / 2); // random left/right
                        int detourDist = island.radius + 28;
                        int wx = island.x + (int)(Math.cos(detourAngle) * detourDist);
                        int wy = island.y + (int)(Math.sin(detourAngle) * detourDist);
                        waypoints.add(0, new Point(wx, wy));
                        break;
                    }
                }
                if (blocked) break;
                nx = testX;
                ny = testY;
            }

            if (!blocked) {
                if (dist < speed) {
                    x = next.x;
                    y = next.y;
                    waypoints.remove(0);
                    if (waypoints.isEmpty()) {
                        arrived = true;
                        // Respawn a new ship at the port
                        if (respawnShip == null) {
                            ships.add(new Ship(x, y, islands, faction));
                            respawnShip = ships.get(ships.size() - 1);
                        }
                    }
                    return;
                }
                x = nx;
                y = ny;
            } else if (arrived && respawnShip == null) {
                // Always respawn at the port location of the target island
                Point port = targetIsland.getPortLocation();
                ships.add(new Ship(port.x, port.y, islands, faction, true)); // true = exact spawn
                respawnShip = ships.get(ships.size() - 1);
            }
        }

        public void draw(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            // Draw waypoints
            g2.setColor(Color.YELLOW);
            for (Point wp : waypoints) {
                g2.fillOval(wp.x - 3, wp.y - 3, 6, 6);
            }
            // Simple brown rowboat
            g2.setColor(new Color(139, 69, 19));
            g2.fillRect(x - 6, y - 3, 12, 6);
            // Faction color flag
            Color factionColor = switch (faction) {
                case RED -> Color.RED;
                case BLUE -> Color.BLUE;
                case GREEN -> Color.GREEN;
                case YELLOW -> Color.YELLOW;
                case PURPLE -> new Color(128, 0, 128);
            };
            g2.setColor(factionColor);
            g2.fillRect(x + 2, y - 6, 4, 2);
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
