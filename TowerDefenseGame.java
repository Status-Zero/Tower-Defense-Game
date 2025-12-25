import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.*;

/**
 * Tower Defense Game - Complete JavaFX Implementation
 * Features:
 * - Tower placement mode (T key activation)
 * - Predictive tower aiming
 * - Placement preview with mouse following
 * - Enemy pathfinding and movement
 * - Tower shooting and damage mechanics
 */
public class TowerDefenseGame extends Application {
    private Canvas canvas;
    private GraphicsContext gc;
    private Scene scene;
    
    // Game state
    private boolean placementMode = false;
    private Point2D mousePos = new Point2D(0, 0);
    private Tower previewTower = null;
    private double gameSpeed = 1.0;
    
    // Game objects
    private List<Tower> towers = new ArrayList<>();
    private List<Enemy> enemies = new ArrayList<>();
    private List<Projectile> projectiles = new ArrayList<>();
    private Path path;
    
    // Game statistics
    private int lives = 20;
    private int gold = 500;
    private int kills = 0;
    private double waveTimer = 0;
    private int currentWave = 1;
    private boolean waveInProgress = false;
    
    // Constants
    private static final int CANVAS_WIDTH = 1200;
    private static final int CANVAS_HEIGHT = 800;
    private static final double TOWER_COST = 150;
    private static final double TOWER_RANGE = 150;
    private static final double TOWER_FIRE_RATE = 0.5; // shots per second
    
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Tower Defense Game");
        
        // Create canvas
        canvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
        gc = canvas.getGraphicsContext2D();
        
        // Setup scene
        BorderPane root = new BorderPane();
        root.setCenter(canvas);
        scene = new Scene(root, CANVAS_WIDTH, CANVAS_HEIGHT);
        
        // Setup event handlers
        scene.setOnKeyPressed(this::handleKeyPress);
        scene.setOnKeyReleased(this::handleKeyRelease);
        canvas.setOnMouseMoved(this::handleMouseMoved);
        canvas.setOnMouseClicked(this::handleMouseClicked);
        
        // Initialize game
        initializeGame();
        
        // Start game loop
        startGameLoop();
        
        primaryStage.setScene(scene);
        primaryStage.show();
        
        canvas.requestFocus();
    }
    
    private void initializeGame() {
        // Create the path for enemies
        path = new Path();
        path.addPoint(0, 400);
        path.addPoint(150, 400);
        path.addPoint(150, 200);
        path.addPoint(350, 200);
        path.addPoint(350, 600);
        path.addPoint(600, 600);
        path.addPoint(600, 300);
        path.addPoint(900, 300);
        path.addPoint(900, 500);
        path.addPoint(1200, 500);
        
        // Add initial towers for demonstration
        towers.add(new Tower(300, 300, TOWER_RANGE, TOWER_FIRE_RATE));
        towers.add(new Tower(700, 250, TOWER_RANGE, TOWER_FIRE_RATE));
    }
    
    private void startGameLoop() {
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                update();
                render();
            }
        };
        timer.start();
    }
    
    private void update() {
        // Spawn enemies
        waveTimer += 0.016 * gameSpeed; // approximately 60 FPS
        if (waveTimer > 2.0 && !waveInProgress) {
            spawnWave();
            waveInProgress = true;
        }
        
        // Update enemies
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy enemy = enemies.get(i);
            enemy.update(0.016 * gameSpeed);
            
            if (enemy.isAlive() && enemy.hasReachedEnd()) {
                enemies.remove(i);
                lives--;
            } else if (!enemy.isAlive()) {
                enemies.remove(i);
                gold += enemy.getReward();
                kills++;
            }
        }
        
        // Update towers
        for (Tower tower : towers) {
            tower.update(0.016 * gameSpeed);
            
            // Find targets
            Enemy target = null;
            double closestDistance = tower.range;
            
            for (Enemy enemy : enemies) {
                if (enemy.isAlive()) {
                    double distance = tower.getDistance(enemy);
                    if (distance < closestDistance) {
                        closestDistance = distance;
                        target = enemy;
                    }
                }
            }
            
            // Shoot with predictive aiming
            if (target != null && tower.canShoot()) {
                Point2D aimPoint = calculatePredictiveAim(tower, target);
                tower.shoot(aimPoint);
            }
        }
        
        // Update projectiles
        for (int i = projectiles.size() - 1; i >= 0; i--) {
            Projectile proj = projectiles.get(i);
            proj.update(0.016 * gameSpeed);
            
            // Check collision with enemies
            for (Enemy enemy : enemies) {
                if (enemy.isAlive() && proj.collidesWith(enemy)) {
                    enemy.takeDamage(proj.damage);
                    projectiles.remove(i);
                    break;
                }
            }
            
            // Remove if off-screen
            if (proj.x < 0 || proj.x > CANVAS_WIDTH || proj.y < 0 || proj.y > CANVAS_HEIGHT) {
                projectiles.remove(i);
            }
        }
        
        // Check game over conditions
        if (lives <= 0) {
            // Game over - could restart or show menu
        }
    }
    
    /**
     * Calculates predictive aim point for tower to intercept moving enemy
     */
    private Point2D calculatePredictiveAim(Tower tower, Enemy enemy) {
        double dx = enemy.x - tower.x;
        double dy = enemy.y - tower.y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        if (distance == 0) {
            return new Point2D(enemy.x, enemy.y);
        }
        
        // Get enemy velocity
        Point2D enemyVelocity = enemy.getVelocity();
        double vx = enemyVelocity.getX();
        double vy = enemyVelocity.getY();
        
        // Projectile speed
        double projectileSpeed = 400; // pixels per second
        
        // Solve for time of impact using quadratic equation
        // We need to find t such that: |enemy_pos + enemy_vel * t - tower_pos| = projectile_speed * t
        double a = vx * vx + vy * vy - projectileSpeed * projectileSpeed;
        double b = 2 * (dx * vx + dy * vy);
        double c = dx * dx + dy * dy;
        
        double t = 0;
        double discriminant = b * b - 4 * a * c;
        
        if (discriminant >= 0 && Math.abs(a) > 0.001) {
            double t1 = (-b + Math.sqrt(discriminant)) / (2 * a);
            double t2 = (-b - Math.sqrt(discriminant)) / (2 * a);
            
            t = (t1 > 0) ? t1 : t2;
            if (t < 0) t = 0;
        } else {
            // Fallback: aim at current position
            t = distance / projectileSpeed;
        }
        
        // Calculate intercept point
        double interceptX = enemy.x + vx * t;
        double interceptY = enemy.y + vy * t;
        
        return new Point2D(interceptX, interceptY);
    }
    
    private void spawnWave() {
        int enemyCount = Math.min(5 + currentWave, 15);
        for (int i = 0; i < enemyCount; i++) {
            enemies.add(new Enemy(path, 50 + i * 100));
        }
        currentWave++;
        waveInProgress = false;
        waveTimer = 0;
    }
    
    private void render() {
        // Clear canvas
        gc.setFill(Color.web("#1a1a1a"));
        gc.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
        
        // Draw path
        drawPath();
        
        // Draw enemies
        for (Enemy enemy : enemies) {
            enemy.render(gc);
        }
        
        // Draw towers
        for (Tower tower : towers) {
            tower.render(gc);
            
            // Draw range indicator when selected
            if (tower == previewTower) {
                gc.setStroke(Color.web("#ffff00", 0.3));
                gc.setLineWidth(2);
                gc.strokeOval(tower.x - tower.range, tower.y - tower.range,
                        tower.range * 2, tower.range * 2);
            }
        }
        
        // Draw projectiles
        for (Projectile proj : projectiles) {
            proj.render(gc);
        }
        
        // Draw placement preview
        if (placementMode && previewTower != null) {
            drawPlacementPreview();
        }
        
        // Draw UI
        drawUI();
    }
    
    private void drawPath() {
        gc.setStroke(Color.web("#FFD700"));
        gc.setLineWidth(40);
        List<Point2D> points = path.getPoints();
        for (int i = 0; i < points.size() - 1; i++) {
            Point2D p1 = points.get(i);
            Point2D p2 = points.get(i + 1);
            gc.strokeLine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
        }
    }
    
    private void drawPlacementPreview() {
        previewTower.x = mousePos.getX();
        previewTower.y = mousePos.getY();
        
        // Check if placement is valid
        boolean validPlacement = isValidTowerPlacement(previewTower.x, previewTower.y);
        Color previewColor = validPlacement ? Color.web("#00ff00", 0.3) : Color.web("#ff0000", 0.3);
        
        // Draw tower
        gc.setFill(previewColor);
        gc.fillOval(previewTower.x - 15, previewTower.y - 15, 30, 30);
        
        // Draw range indicator
        gc.setStroke(Color.web("#00ff00", 0.2));
        gc.setLineWidth(1);
        gc.strokeOval(previewTower.x - previewTower.range, previewTower.y - previewTower.range,
                previewTower.range * 2, previewTower.range * 2);
        
        // Draw predictive aim lines to nearby enemies
        gc.setStroke(Color.web("#ffff00", 0.5));
        gc.setLineWidth(1);
        for (Enemy enemy : enemies) {
            if (enemy.isAlive()) {
                double distance = previewTower.getDistance(enemy);
                if (distance < previewTower.range) {
                    Point2D aimPoint = calculatePredictiveAim(previewTower, enemy);
                    gc.strokeLine(previewTower.x, previewTower.y, aimPoint.getX(), aimPoint.getY());
                }
            }
        }
    }
    
    private boolean isValidTowerPlacement(double x, double y) {
        // Check if too close to path
        List<Point2D> pathPoints = path.getPoints();
        for (int i = 0; i < pathPoints.size() - 1; i++) {
            Point2D p1 = pathPoints.get(i);
            Point2D p2 = pathPoints.get(i + 1);
            if (distanceToLineSegment(x, y, p1.getX(), p1.getY(), p2.getX(), p2.getY()) < 60) {
                return false;
            }
        }
        
        // Check if overlapping with existing towers
        for (Tower tower : towers) {
            if (Math.hypot(x - tower.x, y - tower.y) < 50) {
                return false;
            }
        }
        
        return true;
    }
    
    private double distanceToLineSegment(double px, double py, double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double t = Math.max(0, Math.min(1, ((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy)));
        double closestX = x1 + t * dx;
        double closestY = y1 + t * dy;
        return Math.hypot(px - closestX, py - closestY);
    }
    
    private void drawUI() {
        gc.setFill(Color.WHITE);
        gc.setFont(new Font("Arial", 16));
        
        // Draw stats
        gc.fillText("Lives: " + lives, 20, 30);
        gc.fillText("Gold: " + gold, 20, 60);
        gc.fillText("Kills: " + kills, 20, 90);
        gc.fillText("Wave: " + currentWave, 20, 120);
        
        // Draw mode indicator
        if (placementMode) {
            gc.setFill(Color.web("#ffff00"));
            gc.fillText("PLACEMENT MODE (T to toggle, Click to place)", CANVAS_WIDTH - 400, 30);
            
            double cost = TOWER_COST;
            if (gold >= cost) {
                gc.setFill(Color.web("#00ff00"));
                gc.fillText("Cost: " + (int)cost + " Gold", CANVAS_WIDTH - 400, 60);
            } else {
                gc.setFill(Color.web("#ff0000"));
                gc.fillText("Cost: " + (int)cost + " Gold (INSUFFICIENT)", CANVAS_WIDTH - 400, 60);
            }
        }
        
        gc.setFill(Color.WHITE);
        gc.setFont(new Font("Arial", 12));
        gc.fillText("Press T to toggle tower placement mode", 20, CANVAS_HEIGHT - 20);
    }
    
    private void handleKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.T) {
            placementMode = !placementMode;
            if (placementMode) {
                previewTower = new Tower(mousePos.getX(), mousePos.getY(), TOWER_RANGE, TOWER_FIRE_RATE);
            }
        }
        
        // Game speed controls for testing
        if (event.getCode() == KeyCode.UP) {
            gameSpeed = Math.min(3.0, gameSpeed + 0.1);
        }
        if (event.getCode() == KeyCode.DOWN) {
            gameSpeed = Math.max(0.1, gameSpeed - 0.1);
        }
    }
    
    private void handleKeyRelease(KeyEvent event) {
        // Handle key releases if needed
    }
    
    private void handleMouseMoved(MouseEvent event) {
        mousePos = new Point2D(event.getX(), event.getY());
        if (placementMode && previewTower != null) {
            previewTower.x = mousePos.getX();
            previewTower.y = mousePos.getY();
        }
    }
    
    private void handleMouseClicked(MouseEvent event) {
        if (placementMode && previewTower != null) {
            if (isValidTowerPlacement(previewTower.x, previewTower.y) && gold >= TOWER_COST) {
                Tower newTower = new Tower(previewTower.x, previewTower.y, TOWER_RANGE, TOWER_FIRE_RATE);
                towers.add(newTower);
                gold -= (int)TOWER_COST;
            }
        }
    }
    
    // ============ Inner Classes ============
    
    /**
     * Tower class - handles tower rendering, targeting, and shooting
     */
    private class Tower {
        double x, y;
        double range;
        double fireRate;
        double cooldown;
        
        Tower(double x, double y, double range, double fireRate) {
            this.x = x;
            this.y = y;
            this.range = range;
            this.fireRate = fireRate;
            this.cooldown = 0;
        }
        
        void update(double deltaTime) {
            cooldown = Math.max(0, cooldown - deltaTime);
        }
        
        boolean canShoot() {
            return cooldown <= 0;
        }
        
        void shoot(Point2D target) {
            double dx = target.getX() - x;
            double dy = target.getY() - y;
            double distance = Math.sqrt(dx * dx + dy * dy);
            
            if (distance > 0) {
                double vx = (dx / distance) * 400;
                double vy = (dy / distance) * 400;
                projectiles.add(new Projectile(x, y, vx, vy, 20));
            }
            
            cooldown = 1.0 / fireRate;
        }
        
        double getDistance(Enemy enemy) {
            return Math.hypot(x - enemy.x, y - enemy.y);
        }
        
        void render(GraphicsContext gc) {
            gc.setFill(Color.web("#FF6B6B"));
            gc.fillOval(x - 15, y - 15, 30, 30);
            
            // Draw tower base
            gc.setStroke(Color.web("#FF4444"));
            gc.setLineWidth(2);
            gc.strokeOval(x - 15, y - 15, 30, 30);
        }
    }
    
    /**
     * Enemy class - follows path and takes damage
     */
    private class Enemy {
        double x, y;
        double progress; // progress along path (0-1)
        double speed;
        double health;
        double maxHealth;
        Path path;
        
        Enemy(Path path, double startDelay) {
            this.path = path;
            this.progress = -startDelay / 200.0; // Start before path begins
            this.speed = 100; // pixels per second
            this.health = 30;
            this.maxHealth = 30;
            
            // Position on path
            updatePosition();
        }
        
        void update(double deltaTime) {
            progress += (speed * deltaTime) / path.getTotalLength();
            updatePosition();
        }
        
        private void updatePosition() {
            Point2D pos = path.getPointAt(progress);
            this.x = pos.getX();
            this.y = pos.getY();
        }
        
        void takeDamage(double damage) {
            health -= damage;
        }
        
        boolean isAlive() {
            return health > 0;
        }
        
        boolean hasReachedEnd() {
            return progress > 1.0;
        }
        
        int getReward() {
            return 10;
        }
        
        Point2D getVelocity() {
            double lookAhead = 0.01;
            Point2D current = path.getPointAt(Math.max(0, progress));
            Point2D future = path.getPointAt(Math.min(1, progress + lookAhead));
            
            double vx = (future.getX() - current.getX()) * speed;
            double vy = (future.getY() - current.getY()) * speed;
            
            return new Point2D(vx, vy);
        }
        
        void render(GraphicsContext gc) {
            // Draw enemy
            gc.setFill(Color.web("#00FF00"));
            gc.fillOval(x - 8, y - 8, 16, 16);
            
            // Draw health bar
            gc.setFill(Color.web("#FF0000"));
            gc.fillRect(x - 12, y - 18, 24, 3);
            gc.setFill(Color.web("#00FF00"));
            gc.fillRect(x - 12, y - 18, 24 * (health / maxHealth), 3);
        }
    }
    
    /**
     * Projectile class - handles projectile movement and collision
     */
    private class Projectile {
        double x, y;
        double vx, vy;
        double damage;
        double radius;
        
        Projectile(double x, double y, double vx, double vy, double damage) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.damage = damage;
            this.radius = 4;
        }
        
        void update(double deltaTime) {
            x += vx * deltaTime;
            y += vy * deltaTime;
        }
        
        boolean collidesWith(Enemy enemy) {
            return Math.hypot(x - enemy.x, y - enemy.y) < radius + 8;
        }
        
        void render(GraphicsContext gc) {
            gc.setFill(Color.web("#FFFF00"));
            gc.fillOval(x - radius, y - radius, radius * 2, radius * 2);
        }
    }
    
    /**
     * Path class - defines the path that enemies follow
     */
    private class Path {
        List<Point2D> points = new ArrayList<>();
        List<Double> segmentLengths = new ArrayList<>();
        double totalLength = 0;
        
        void addPoint(double x, double y) {
            Point2D newPoint = new Point2D(x, y);
            
            if (!points.isEmpty()) {
                Point2D lastPoint = points.get(points.size() - 1);
                double distance = Math.hypot(newPoint.getX() - lastPoint.getX(),
                        newPoint.getY() - lastPoint.getY());
                segmentLengths.add(distance);
                totalLength += distance;
            }
            
            points.add(newPoint);
        }
        
        List<Point2D> getPoints() {
            return new ArrayList<>(points);
        }
        
        double getTotalLength() {
            return totalLength;
        }
        
        Point2D getPointAt(double progress) {
            progress = Math.max(0, Math.min(1, progress));
            
            if (points.size() < 2) {
                return points.isEmpty() ? new Point2D(0, 0) : points.get(0);
            }
            
            double targetDistance = progress * totalLength;
            double currentDistance = 0;
            
            for (int i = 0; i < segmentLengths.size(); i++) {
                double segmentLength = segmentLengths.get(i);
                if (currentDistance + segmentLength >= targetDistance) {
                    double ratio = (targetDistance - currentDistance) / segmentLength;
                    Point2D p1 = points.get(i);
                    Point2D p2 = points.get(i + 1);
                    return new Point2D(
                            p1.getX() + (p2.getX() - p1.getX()) * ratio,
                            p1.getY() + (p2.getY() - p1.getY()) * ratio
                    );
                }
                currentDistance += segmentLength;
            }
            
            return points.get(points.size() - 1);
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
