import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Cannonball {
    int x, y;
    int dx, dy;
    int rangeLeft;
    boolean hit = false;
    boolean exploded = false;
    Ship target;

    private static final int MAX_RANGE = 220; // pixels

    // Explosion animation state
    private int explosionTick = 0;
    private static final int EXPLOSION_DURATION = 12;
    private List<Debris> debrisList = new ArrayList<>();

    public Cannonball(int x, int y, Ship target) {
        this.x = x;
        this.y = y;
        this.target = target;
        double angle = Math.atan2(target.y - y, target.x - x);
        double speed = 8;
        dx = (int) (Math.cos(angle) * speed);
        dy = (int) (Math.sin(angle) * speed);
        rangeLeft = MAX_RANGE;
    }

    public void move() {
        if (exploded) {
            explosionTick++;
            for (Debris d : debrisList) d.move();
            return;
        }
        if (hit) return;
        x += dx;
        y += dy;
        rangeLeft -= Math.sqrt(dx * dx + dy * dy);

        // Check if hit target
        int distSq = (x - target.x) * (x - target.x) + (y - target.y) * (y - target.y);
        if (distSq < target.size * target.size / 4) {
            hit = true;
            target.health--;
            startExplosion();
        }

        // Splash if out of range
        if (rangeLeft <= 0 && !hit) {
            hit = true;
            // Optionally: spawn a splash effect here
        }
    }

    private void startExplosion() {
        exploded = true;
        explosionTick = 0;
        debrisList.clear();
        // Generate debris flying out in random directions, much slower
        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(i * 45 + (int)(Math.random() * 20 - 10));
            double speed = 0.7 + Math.random() * 0.7; // much slower debris
            debrisList.add(new Debris(x, y, Math.cos(angle) * speed, Math.sin(angle) * speed));
        }
    }

    public boolean hasHitTarget() {
        // Remove after explosion animation or splash
        return (hit && !exploded) || (exploded && explosionTick > EXPLOSION_DURATION) || (hit && rangeLeft <= 0);
    }

    public void draw(Graphics g) {
        if (exploded && explosionTick <= EXPLOSION_DURATION) {
            // Draw explosion (very small)
            int r = 2 + explosionTick / 2; // much smaller radius
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(new Color(255, 200, 60, 180));
            g2.fillOval(x - r / 2, y - r / 2, r, r);
            g2.setColor(new Color(255, 120, 0, 120));
            g2.fillOval(x - r / 4, y - r / 4, r / 2, r / 2);

            // Draw debris
            for (Debris d : debrisList) d.draw(g2);
        } else if (hit && rangeLeft <= 0) {
            // Draw splash
            g.setColor(new Color(120, 180, 230, 180));
            g.fillOval(x - 6, y - 3, 12, 6);
        } else if (!hit) {
            // Draw cannonball
            g.setColor(Color.DARK_GRAY);
            g.fillOval(x - 3, y - 3, 6, 6);
        }
    }

    // Debris class for explosion effect
    private static class Debris {
        double x, y, dx, dy;
        int life = 10 + (int)(Math.random() * 6);

        Debris(double x, double y, double dx, double dy) {
            this.x = x;
            this.y = y;
            this.dx = dx;
            this.dy = dy;
        }

        void move() {
            x += dx;
            y += dy;
            dy += 0.2; // gravity for arc
            life--;
        }

        void draw(Graphics2D g2) {
            if (life > 0) {
                g2.setColor(new Color(120 + (int)(Math.random() * 80), 80, 30));
                g2.fillRect((int)x, (int)y, 3, 3);
            }
        }
    }
}
