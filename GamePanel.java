import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class GamePanel extends JPanel {
    public static final int WIDTH = 1800;
    public static final int HEIGHT = 1000;
    private List<Ship> ships = new ArrayList<>();
    private List<Cannonball> cannonballs = new ArrayList<>();
    private List<Swell> swells = new ArrayList<>();
    private List<Island> islands = new ArrayList<>();
    private List<Ship.Shipwreck> wrecks = new ArrayList<>();
    private List<Ship.Rowboat> rowboats = new ArrayList<>();
    private List<HealthDrop> healthDrops = new ArrayList<>();
    private Timer timer;

    private final int shipsPerFaction = 5; // Number of ships to spawn per faction

    // Add pirates list
    private List<Pirate> pirates = new ArrayList<>();

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setDoubleBuffered(true);

        // Create swells
        for (int i = 0; i < 100; i++) {
            swells.add(new Swell(WIDTH, HEIGHT));
        }

        // Create islands without overlap
        int islandCount = 5;
        int maxTries = 100;
        Faction[] factions = Faction.values();
        for (int i = 0; i < islandCount; i++) {
            int tries = 0;
            Island newIsland;
            boolean overlaps;
            do {
                newIsland = new Island(WIDTH, HEIGHT, factions[i % factions.length]); // Pass faction
                overlaps = false;
                for (Island other : islands) {
                    int dx = newIsland.x - other.x;
                    int dy = newIsland.y - other.y;
                    int distSq = dx * dx + dy * dy;
                    int minDist = newIsland.radius + other.radius + 32; // 32px buffer
                    if (distSq < minDist * minDist) {
                        overlaps = true;
                        break;
                    }
                }
                tries++;
            } while (overlaps && tries < maxTries);
            islands.add(newIsland);

            for (int p = 0; p < 3; p++) { // 3 pirates per island
                pirates.add(new Pirate(newIsland.x, newIsland.y, newIsland.radius, newIsland.faction));
            }
        }

        // Create ships
        for (Island island : islands) {
            Point port = island.getPortLocation();
            // Clamp spawn to be at least 16px from the border
            int safeX = Math.max(16, Math.min(WIDTH - 16, port.x));
            int safeY = Math.max(16, Math.min(HEIGHT - 16, port.y));
            for (int j = 0; j < shipsPerFaction; j++) {
                ships.add(new Ship(safeX, safeY, islands, island.faction, true)); // true = exact spawn
            }
        }
    }

    public void startGame() {
        timer = new Timer(16, e -> updateGame());
        timer.start();
    }

    private void updateGame() {
        List<Ship> shipsToRemove = new ArrayList<>();
        List<Ship> shipsToAdd = new ArrayList<>(); // <-- Add this line

        for (Ship ship : ships) {
            ship.update(ships, cannonballs, islands, healthDrops);
            if (ship.health <= 0) {
                healthDrops.add(new HealthDrop(ship.x, ship.y));
                // Find the faction's island
                Island base = null;
                for (Island island : islands) {
                    if (island.faction == ship.faction) {
                        base = island;
                        break;
                    }
                }
                if (base != null) {
                    Point port = base.getPortLocation();
                    int safeX = Math.max(16, Math.min(WIDTH - 16, port.x));
                    int safeY = Math.max(16, Math.min(HEIGHT - 16, port.y));
                    shipsToAdd.add(new Ship(safeX, safeY, islands, ship.faction, true));
                }
                shipsToRemove.add(ship);
            }
        }
        ships.removeAll(shipsToRemove);
        ships.addAll(shipsToAdd); // <-- Add new ships after removal

        for (Cannonball cb : cannonballs) {
            cb.move();
        }

        for (Swell s : swells) {
            s.update(WIDTH, HEIGHT);
        }

        // Update wrecks
        for (Iterator<Ship.Shipwreck> it = wrecks.iterator(); it.hasNext(); ) {
            Ship.Shipwreck wreck = it.next();
            wreck.update();
            if (wreck.life <= 0) it.remove();
        }

        // Update and remove expired health drops
        List<HealthDrop> dropsToRemove = new ArrayList<>();
        for (HealthDrop drop : healthDrops) {
            drop.update();
            if (drop.isExpired()) dropsToRemove.add(drop);
        }

        // Ship collects health drop
        Iterator<HealthDrop> dropIt = healthDrops.iterator();
        while (dropIt.hasNext()) {
            HealthDrop drop = dropIt.next();
            boolean collected = false;
            for (Ship ship : ships) {
                int dx = ship.x - drop.x;
                int dy = ship.y - drop.y;
                int distSq = dx * dx + dy * dy;
                int minDist = ship.size/2 + drop.size/2;
                if (distSq < minDist * minDist) {
                    ship.health = 10; // Fully heal the ship (set to max health)
                    collected = true;
                    break;
                }
            }
            if (collected) dropIt.remove();
        }

        // Remove expired drops
        healthDrops.removeAll(dropsToRemove);

        cannonballs.removeIf(Cannonball::hasHitTarget);

        for (int i = 0; i < islands.size(); i++) {
            Island island = islands.get(i);
            for (Pirate pirate : pirates) {
                if (pirate.faction == island.faction) {
                    pirate.update(island);
                }
            }
        }

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Draw ocean background with swells
        drawOcean(g);

        // Draw islands
        for (Island island : islands) island.draw(g);

        // Draw ships, cannonballs, etc.
        for (Ship ship : ships) ship.draw(g);
        for (Cannonball cb : cannonballs) cb.draw(g);
        for (Ship.Shipwreck wreck : wrecks) wreck.draw(g);
        for (Ship.Rowboat boat : rowboats) boat.draw(g);
        // Draw health drops
        for (HealthDrop drop : healthDrops) drop.draw(g);
        for (Pirate pirate : pirates) pirate.draw(g);
    }

    private void drawOcean(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        // Base ocean blue
        g2.setColor(new Color(0, 70, 180));
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        // Swells (fading pixel wave lines)
        for (Swell s : swells) {
            float alpha = s.getAlpha();
            if (alpha <= 0) continue;

            g2.setColor(new Color(120, 180, 230, (int)(alpha * 255)));
            g2.fillRect(s.x, s.y, s.width, s.height);
        }
    }
}

