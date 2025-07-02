class Swell {
    int x, y;
    int width = 6, height = 2;
    int life = 0;
    int maxLife;
    double phaseOffset;

    public Swell(int panelWidth, int panelHeight) {
        reset(panelWidth, panelHeight);
    }

    public void reset(int w, int h) {
        x = (int)(Math.random() * w);
        y = (int)(Math.random() * h);
        maxLife = 100 + (int)(Math.random() * 100); // lifespan in frames
        life = 0;
        phaseOffset = Math.random() * Math.PI * 2;
    }

    public float getAlpha() {
        float t = (float) life / maxLife;
        return (float) (Math.sin(t * Math.PI) * 0.8); // smooth fade in/out
    }

    public void update(int w, int h) {
        life++;
        if (life > maxLife) reset(w, h);
    }
}
