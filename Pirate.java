import java.awt.*;
import java.util.Random;

public class Pirate {
    int x, y;
    int radius;
    Faction faction;
    Color color;
    int dx, dy;
    int step = 0;
    Random rand = new Random();

    public Pirate(int centerX, int centerY, int islandRadius, Faction faction) {
        this.faction = faction;
        this.radius = islandRadius - 10;
        this.x = centerX + rand.nextInt(radius * 2) - radius;
        this.y = centerY + rand.nextInt(radius * 2) - radius;
        this.color = switch (faction) {
            case RED -> Color.RED;
            case BLUE -> Color.BLUE;
            case GREEN -> Color.GREEN;
            case YELLOW -> Color.YELLOW;
            case PURPLE -> new Color(128, 0, 128);
        };
        randomDirection();
    }

    private void randomDirection() {
        double angle = rand.nextDouble() * Math.PI * 2;
        dx = (int) Math.round(Math.cos(angle));
        dy = (int) Math.round(Math.sin(angle));
        step = 20 + rand.nextInt(40);
    }

    public void update(Island island) {
        if (step-- <= 0) randomDirection();
        int nx = x + dx;
        int ny = y + dy;
        int dist = (int)Math.hypot(nx - island.x, ny - island.y);
        // Only move if still inside the island's radius (with a small margin)
        if (dist < island.radius - 6) { // -6 keeps them away from the edge
            x = nx;
            y = ny;
        } else {
            randomDirection();
        }
    }

    public void draw(Graphics g) {
        g.setColor(color);
        g.fillOval(x - 2, y - 3, 4, 4); // much smaller head
        g.setColor(new Color(60, 40, 20));
        g.fillRect(x - 1, y, 2, 4); // much smaller body
    }
}