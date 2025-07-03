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

    private static final int MAX_RANGE = 150; // pixels

    // Explosion animation state
    private int explosionTick = 0;
    private static final int EXPLOSION_DURATION = 12;
    private List<Debris> debrisList = new ArrayList<>();
    private int splashTick = 0;
    private static final int SPLASH_DURATION = 15;

    public Cannonball(int x, int y, Ship target) {
        this.x = x;
        this.y = y;
        this.target = target;
        double angle = Math.atan2(target.y - y, target.x - x);
        double speed = 4;
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
        if (hit) {
            // If it's a splash, increment splashTick
            if (rangeLeft <= 0 && splashTick < SPLASH_DURATION) {
                splashTick++;
            }
            return;
        }
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
            splashTick = 0; // Start splash animation
            // Optionally: play splash sound here
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
        return (hit && !exploded && splashTick >= SPLASH_DURATION) || (exploded && explosionTick > EXPLOSION_DURATION);
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
        } else if (hit && rangeLeft <= 0 && splashTick < SPLASH_DURATION) {
            // Draw animated splash
            Graphics2D g2 = (Graphics2D) g;
            int splashRadius = 4 + splashTick * 2;
            int alpha = 180 - splashTick * 10;
            if (alpha < 0) alpha = 0;
            g2.setColor(new Color(120, 180, 230, alpha));
            g2.fillOval(x - splashRadius / 2, y - splashRadius / 4, splashRadius, splashRadius / 2);
        } else if (!hit) {
            // Draw cannonball
            g.setColor(new Color(60, 60, 60));
            g.fillRect(x - 2, y - 2, 4, 4);

            // Optional: add a white highlight for a pixel-art shine
            g.setColor(new Color(220, 220, 220));
            g.fillRect(x - 1, y - 1, 1, 1);
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
