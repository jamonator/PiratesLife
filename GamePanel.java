import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GamePanel extends JPanel {
    public static final int WIDTH = 1000;
    public static final int HEIGHT = 800;
    private List<Ship> ships = new ArrayList<>();
    private List<Cannonball> cannonballs = new ArrayList<>();
    private List<Swell> swells = new ArrayList<>();
    private List<Island> islands = new ArrayList<>();
    private List<Ship.Shipwreck> wrecks = new ArrayList<>();
    private List<Ship.Rowboat> rowboats = new ArrayList<>();
    private Timer timer;

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setDoubleBuffered(true);

        // Create ships
        for (int i = 0; i < 5; i++) {
            ships.add(new Ship((int)(Math.random() * WIDTH), (int)(Math.random() * HEIGHT), islands));
        }

        // Create swells
        for (int i = 0; i < 100; i++) {
            swells.add(new Swell(WIDTH, HEIGHT));
        }

        // Create islands without overlap
        int islandCount = 3;
        int maxTries = 100;
        for (int i = 0; i < islandCount; i++) {
            int tries = 0;
            Island newIsland;
            boolean overlaps;
            do {
                newIsland = new Island(WIDTH, HEIGHT);
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
        }
    }

    public void startGame() {
        timer = new Timer(16, e -> updateGame());
        timer.start();
    }

    private void updateGame() {

        for (Iterator<Ship> it = ships.iterator(); it.hasNext(); ) {
            Ship ship = it.next();
            ship.update(ships, cannonballs, islands);
            if (ship.health <= 0) {
                ship.destroy(wrecks, rowboats, islands);
                it.remove(); // Remove the destroyed ship
            }
        }

        for (Cannonball cb : cannonballs) {
            cb.move();
        }

        for (Swell s : swells) {
            s.update(WIDTH, HEIGHT);
        }

        // Update wrecks and rowboats
        for (Iterator<Ship.Shipwreck> it = wrecks.iterator(); it.hasNext(); ) {
            Ship.Shipwreck wreck = it.next();
            wreck.update();
            if (wreck.life <= 0) it.remove();
        }
        for (Iterator<Ship.Rowboat> it = rowboats.iterator(); it.hasNext(); ) {
            Ship.Rowboat boat = it.next();
            boat.update();
            if (boat.arrived && boat.respawnShip == null) {
                // Respawn a new ship at the port
                Ship newShip = new Ship(boat.targetPort.x, boat.targetPort.y, islands);
                ships.add(newShip);
                boat.respawnShip = newShip;
                it.remove(); // Remove rowboat after respawn
            }
        }

        cannonballs.removeIf(Cannonball::hasHitTarget);

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

