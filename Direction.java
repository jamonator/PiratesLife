import java.util.Random;

public enum Direction {
    NORTH(0, -1),
    EAST(1, 0),
    SOUTH(0, 1),
    WEST(-1, 0);

    public final int dx, dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    private static final Direction[] VALUES = values();
    private static final Random rand = new Random();

    public static Direction random() {
        return VALUES[rand.nextInt(VALUES.length)];
    }
}
