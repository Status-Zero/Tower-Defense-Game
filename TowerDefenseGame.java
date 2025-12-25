import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

public class TowerDefenseGame extends JPanel implements ActionListener, MouseListener {
    
    // Game components
    private ArrayList<Enemy> enemies;
    private ArrayList<Tower> towers;
    private ArrayList<Projectile> projectiles;
    private int money = 500;
    private int lives = 20;
    private int wave = 1;
    private boolean gameOver = false;
    
    // Game state
    private Timer gameTimer;
    private int selectedTower = -1;
    private Point mousePos;
    
    // Tower costs
    private static final int BASIC_TOWER_COST = 100;
    private static final int RAPID_TOWER_COST = 150;
    private static final int SNIPER_TOWER_COST = 200;
    
    // Extended enemy path with more turns and waypoints
    private static final Point[] PATH = {
        new Point(50, 300),
        new Point(150, 300),
        new Point(150, 100),
        new Point(300, 100),
        new Point(300, 400),
        new Point(450, 400),
        new Point(450, 150),
        new Point(600, 150),
        new Point(600, 500),
        new Point(750, 500),
        new Point(750, 200),
        new Point(900, 200),
        new Point(900, 450),
        new Point(1000, 450),
        new Point(1000, 50)
    };
    
    public TowerDefenseGame() {
        enemies = new ArrayList<>();
        towers = new ArrayList<>();
        projectiles = new ArrayList<>();
        
        addMouseListener(this);
        
        gameTimer = new Timer(16, this); // ~60 FPS
        gameTimer.start();
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameOver) {
            // Update enemies
            for (Enemy enemy : enemies) {
                enemy.update();
            }
            
            // Check if enemies reached end
            for (int i = enemies.size() - 1; i >= 0; i--) {
                if (enemies.get(i).hasReachedEnd()) {
                    lives--;
                    enemies.remove(i);
                    if (lives <= 0) {
                        gameOver = true;
                    }
                }
            }
            
            // Update towers and handle targeting
            for (Tower tower : towers) {
                Enemy target = findClosestEnemy(tower);
                if (target != null) {
                    tower.setTarget(target);
                    if (tower.canFire()) {
                        // Calculate predicted position of enemy
                        Point predictedPos = calculatePredictedPosition(target, tower);
                        Projectile projectile = new Projectile(
                            tower.getX(), tower.getY(),
                            predictedPos.x, predictedPos.y,
                            target, tower.getDamage()
                        );
                        projectiles.add(projectile);
                        tower.resetFireTimer();
                    }
                } else {
                    tower.setTarget(null);
                }
            }
            
            // Update projectiles
            for (int i = projectiles.size() - 1; i >= 0; i--) {
                Projectile proj = projectiles.get(i);
                proj.update();
                
                if (proj.hasHitTarget()) {
                    projectiles.remove(i);
                } else if (proj.isOffScreen()) {
                    projectiles.remove(i);
                }
            }
        }
        
        repaint();
    }
    
    private Enemy findClosestEnemy(Tower tower) {
        Enemy closest = null;
        double minDistance = tower.getRange();
        
        for (Enemy enemy : enemies) {
            double distance = tower.distanceTo(enemy);
            if (distance < minDistance) {
                minDistance = distance;
                closest = enemy;
            }
        }
        
        return closest;
    }
    
    private Point calculatePredictedPosition(Enemy enemy, Tower tower) {
        // Calculate where the enemy will be when the projectile reaches it
        double distance = tower.distanceTo(enemy);
        double projectileSpeed = 300; // pixels per second
        double timeToHit = distance / projectileSpeed;
        
        // Get enemy velocity
        Vector2D velocity = enemy.getVelocity();
        
        // Calculate predicted position
        Point currentPos = enemy.getPosition();
        double predictedX = currentPos.x + (velocity.x * timeToHit);
        double predictedY = currentPos.y + (velocity.y * timeToHit);
        
        return new Point((int)predictedX, (int)predictedY);
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        
        // Draw path
        drawPath(g2d);
        
        // Draw enemies
        for (Enemy enemy : enemies) {
            enemy.draw(g2d);
        }
        
        // Draw towers
        for (Tower tower : towers) {
            tower.draw(g2d);
        }
        
        // Draw projectiles
        for (Projectile projectile : projectiles) {
            projectile.draw(g2d);
        }
        
        // Draw UI
        drawUI(g2d);
    }
    
    private void drawPath(Graphics2D g) {
        g.setColor(new Color(200, 200, 200));
        g.setStroke(new BasicStroke(20));
        
        for (int i = 0; i < PATH.length - 1; i++) {
            g.drawLine(PATH[i].x, PATH[i].y, PATH[i + 1].x, PATH[i + 1].y);
        }
    }
    
    private void drawUI(Graphics2D g) {
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        
        g.drawString("Money: $" + money, 10, 20);
        g.drawString("Lives: " + lives, 10, 40);
        g.drawString("Wave: " + wave, 10, 60);
        
        g.drawString("1: Basic Tower ($" + BASIC_TOWER_COST + ")", 10, 100);
        g.drawString("2: Rapid Tower ($" + RAPID_TOWER_COST + ")", 10, 120);
        g.drawString("3: Sniper Tower ($" + SNIPER_TOWER_COST + ")", 10, 140);
        
        if (gameOver) {
            g.setColor(new Color(255, 0, 0, 128));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 48));
            g.drawString("GAME OVER", getWidth() / 2 - 150, getHeight() / 2);
        }
    }
    
    @Override
    public void mouseClicked(MouseEvent e) {
        if (selectedTower != -1 && !gameOver) {
            int x = e.getX();
            int y = e.getY();
            
            Tower tower = null;
            switch (selectedTower) {
                case 1:
                    if (money >= BASIC_TOWER_COST) {
                        tower = new BasicTower(x, y);
                        money -= BASIC_TOWER_COST;
                    }
                    break;
                case 2:
                    if (money >= RAPID_TOWER_COST) {
                        tower = new RapidTower(x, y);
                        money -= RAPID_TOWER_COST;
                    }
                    break;
                case 3:
                    if (money >= SNIPER_TOWER_COST) {
                        tower = new SniperTower(x, y);
                        money -= SNIPER_TOWER_COST;
                    }
                    break;
            }
            
            if (tower != null) {
                towers.add(tower);
            }
            
            selectedTower = -1;
        }
    }
    
    @Override
    public void mousePressed(MouseEvent e) {}
    
    @Override
    public void mouseReleased(MouseEvent e) {}
    
    @Override
    public void mouseEntered(MouseEvent e) {}
    
    @Override
    public void mouseExited(MouseEvent e) {}
    
    public void startWave() {
        for (int i = 0; i < 5 + wave * 2; i++) {
            enemies.add(new Enemy(PATH));
        }
        wave++;
    }
    
    public static void main(String[] args) {
        JFrame frame = new JFrame("Tower Defense Game");
        TowerDefenseGame game = new TowerDefenseGame();
        frame.add(game);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 700);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        
        // Test: start first wave after 2 seconds
        Timer startTimer = new Timer(2000, e -> game.startWave());
        startTimer.setRepeats(false);
        startTimer.start();
    }
}

// Enemy class
class Enemy {
    private Point[] path;
    private int pathIndex = 0;
    private double x, y;
    private double speed = 1.0; // pixels per frame
    private int health = 1;
    private Vector2D velocity;
    
    public Enemy(Point[] path) {
        this.path = path;
        this.x = path[0].x;
        this.y = path[0].y;
        this.velocity = new Vector2D(0, 0);
    }
    
    public void update() {
        if (pathIndex < path.length - 1) {
            Point current = path[pathIndex];
            Point next = path[pathIndex + 1];
            
            double dx = next.x - x;
            double dy = next.y - y;
            double distance = Math.sqrt(dx * dx + dy * dy);
            
            if (distance < speed) {
                pathIndex++;
                if (pathIndex < path.length - 1) {
                    next = path[pathIndex + 1];
                    dx = next.x - x;
                    dy = next.y - y;
                    distance = Math.sqrt(dx * dx + dy * dy);
                }
            }
            
            // Update velocity for predictive targeting
            velocity.x = (dx / distance) * speed;
            velocity.y = (dy / distance) * speed;
            
            x += velocity.x;
            y += velocity.y;
        }
    }
    
    public void takeDamage(int damage) {
        health -= damage;
    }
    
    public boolean isDead() {
        return health <= 0;
    }
    
    public boolean hasReachedEnd() {
        return pathIndex >= path.length - 1;
    }
    
    public Point getPosition() {
        return new Point((int)x, (int)y);
    }
    
    public Vector2D getVelocity() {
        return velocity;
    }
    
    public void draw(Graphics2D g) {
        g.setColor(Color.RED);
        g.fillOval((int)x - 5, (int)y - 5, 10, 10);
    }
}

// Tower base class
abstract class Tower {
    protected int x, y;
    protected int range = 100;
    protected double fireRate;
    protected int damage;
    protected long lastFireTime = 0;
    protected Enemy target;
    
    public Tower(int x, int y, double fireRate, int damage) {
        this.x = x;
        this.y = y;
        this.fireRate = fireRate;
        this.damage = damage;
    }
    
    public boolean canFire() {
        long currentTime = System.currentTimeMillis();
        long fireInterval = (long)(1000.0 / fireRate);
        return (currentTime - lastFireTime) >= fireInterval && target != null && !target.isDead();
    }
    
    public void resetFireTimer() {
        lastFireTime = System.currentTimeMillis();
    }
    
    public void setTarget(Enemy target) {
        this.target = target;
    }
    
    public int getX() {
        return x;
    }
    
    public int getY() {
        return y;
    }
    
    public int getRange() {
        return range;
    }
    
    public int getDamage() {
        return damage;
    }
    
    public double distanceTo(Enemy enemy) {
        Point enemyPos = enemy.getPosition();
        double dx = x - enemyPos.x;
        double dy = y - enemyPos.y;
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    public abstract void draw(Graphics2D g);
}

// Tower implementations
class BasicTower extends Tower {
    public BasicTower(int x, int y) {
        super(x, y, 2.0, 3); // fires every 0.5 seconds, deals 3 damage
    }
    
    @Override
    public void draw(Graphics2D g) {
        g.setColor(Color.BLUE);
        g.fillRect(x - 10, y - 10, 20, 20);
        g.setColor(new Color(0, 0, 150));
        g.drawOval(x - range, y - range, range * 2, range * 2);
    }
}

class RapidTower extends Tower {
    public RapidTower(int x, int y) {
        super(x, y, 10.0, 5); // fires every 0.1 seconds, deals 5 damage
    }
    
    @Override
    public void draw(Graphics2D g) {
        g.setColor(Color.GREEN);
        g.fillRect(x - 10, y - 10, 20, 20);
        g.setColor(new Color(0, 150, 0));
        g.drawOval(x - range, y - range, range * 2, range * 2);
    }
}

class SniperTower extends Tower {
    public SniperTower(int x, int y) {
        super(x, y, 0.5, 10); // fires every 2 seconds, deals 10 damage
        this.range = 150;
    }
    
    @Override
    public void draw(Graphics2D g) {
        g.setColor(Color.YELLOW);
        g.fillRect(x - 10, y - 10, 20, 20);
        g.setColor(new Color(150, 150, 0));
        g.drawOval(x - range, y - range, range * 2, range * 2);
    }
}

// Projectile class with predictive targeting
class Projectile {
    private double x, y;
    private double targetX, targetY;
    private Enemy target;
    private int damage;
    private double speed = 300.0; // pixels per second
    private boolean hit = false;
    
    public Projectile(int x, int y, int targetX, int targetY, Enemy target, int damage) {
        this.x = x;
        this.y = y;
        this.targetX = targetX;
        this.targetY = targetY;
        this.target = target;
        this.damage = damage;
    }
    
    public void update() {
        if (!hit && target != null && !target.isDead()) {
            // Move towards predicted position
            double dx = targetX - x;
            double dy = targetY - y;
            double distance = Math.sqrt(dx * dx + dy * dy);
            
            if (distance > 5) {
                double moveDistance = (speed / 60.0); // assuming 60 FPS
                x += (dx / distance) * moveDistance;
                y += (dy / distance) * moveDistance;
                
                // Check if projectile reached target
                if (distance < moveDistance) {
                    target.takeDamage(damage);
                    hit = true;
                }
            } else {
                target.takeDamage(damage);
                hit = true;
            }
        }
    }
    
    public boolean hasHitTarget() {
        return hit;
    }
    
    public boolean isOffScreen() {
        return x < 0 || x > 1200 || y < 0 || y > 700;
    }
    
    public void draw(Graphics2D g) {
        g.setColor(Color.YELLOW);
        g.fillOval((int)x - 3, (int)y - 3, 6, 6);
    }
}

// Vector2D helper class
class Vector2D {
    public double x, y;
    
    public Vector2D(double x, double y) {
        this.x = x;
        this.y = y;
    }
}
