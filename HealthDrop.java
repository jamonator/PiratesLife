import java.awt.*;

public class HealthDrop {
    int x, y;
    int size = 16;
    int life = 300; // frames before disappearing

    public HealthDrop(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void update() {
        life--;
    }

    public boolean isExpired() {
        return life <= 0;
    }

    public void draw(Graphics g) {
        g.setColor(new Color(60, 220, 60));
        g.fillOval(x - size/2, y - size/2, size, size);
        g.setColor(Color.WHITE);
        g.drawString("+", x - 4, y + 5);
    }
}