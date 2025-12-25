import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.*;

/**
 * Tower Defense Game - A JavaFX-based tower defense game
 * Players place towers to defend against waves of enemies
 */
public class TowerDefenseGame extends Application {
    
    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 800;
    private static final int GRID_SIZE = 40;
    
    private Canvas canvas;
    private GraphicsContext gc;
    private GameState gameState;
    private AnimationTimer gameLoop;
    
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Tower Defense Game");
        
        canvas = new Canvas(WINDOW_WIDTH, WINDOW_HEIGHT);
        gc = canvas.getGraphicsContext2D();
        gameState = new GameState(WINDOW_WIDTH, WINDOW_HEIGHT);
        
        // Mouse click event handler for tower placement
        canvas.setOnMouseClicked(this::handleMouseClick);
        
        StackPane root = new StackPane(canvas);
        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        primaryStage.setScene(scene);
        primaryStage.show();
        
        startGameLoop();
    }
    
    private void handleMouseClick(MouseEvent event) {
        int gridX = (int) (event.getX() / GRID_SIZE);
        int gridY = (int) (event.getY() / GRID_SIZE);
        gameState.placeTower(gridX, gridY);
    }
    
    private void startGameLoop() {
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                update();
                render();
            }
        };
        gameLoop.start();
    }
    
    private void update() {
        gameState.update();
    }
    
    private void render() {
        // Clear canvas
        gc.setFill(Color.web("#1a1a2e"));
        gc.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
        
        // Draw grid
        drawGrid();
        
        // Draw towers
        gameState.getTowers().forEach(tower -> drawTower(tower));
        
        // Draw enemies
        gameState.getEnemies().forEach(enemy -> drawEnemy(enemy));
        
        // Draw projectiles
        gameState.getProjectiles().forEach(projectile -> drawProjectile(projectile));
        
        // Draw UI
        drawUI();
    }
    
    private void drawGrid() {
        gc.setStroke(Color.web("#0f3460"));
        gc.setLineWidth(0.5);
        
        for (int x = 0; x <= WINDOW_WIDTH; x += GRID_SIZE) {
            gc.strokeLine(x, 0, x, WINDOW_HEIGHT);
        }
        for (int y = 0; y <= WINDOW_HEIGHT; y += GRID_SIZE) {
            gc.strokeLine(0, y, WINDOW_WIDTH, y);
        }
    }
    
    private void drawTower(Tower tower) {
        gc.setFill(Color.web("#00d4ff"));
        gc.fillRect(tower.x - 10, tower.y - 10, 20, 20);
        
        // Draw range indicator when selected
        if (tower.isSelected) {
            gc.setStroke(Color.web("#00d4ff"));
            gc.setLineWidth(1);
            gc.setGlobalAlpha(0.3);
            gc.strokeOval(tower.x - tower.range, tower.y - tower.range, 
                         tower.range * 2, tower.range * 2);
            gc.setGlobalAlpha(1.0);
        }
    }
    
    private void drawEnemy(Enemy enemy) {
        gc.setFill(Color.web("#e94560"));
        gc.fillOval(enemy.x - 8, enemy.y - 8, 16, 16);
        
        // Draw health bar
        gc.setFill(Color.web("#2d2d44"));
        gc.fillRect(enemy.x - 10, enemy.y - 15, 20, 3);
        gc.setFill(Color.GREEN);
        gc.fillRect(enemy.x - 10, enemy.y - 15, (int)(20 * enemy.getHealthPercentage()), 3);
    }
    
    private void drawProjectile(Projectile projectile) {
        gc.setFill(Color.YELLOW;
        gc.fillOval(projectile.x - 4, projectile.y - 4, 8, 8);
    }
    
    private void drawUI() {
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font(16));
        
        int y = 30;
        gc.fillText("Gold: " + gameState.getGold(), 20, y);
        y += 30;
        gc.fillText("Lives: " + gameState.getLives(), 20, y);
        y += 30;
        gc.fillText("Wave: " + gameState.getCurrentWave(), 20, y);
        y += 30;
        gc.fillText("Enemies: " + gameState.getEnemies().size(), 20, y);
        
        if (gameState.isGameOver()) {
            gc.setFill(Color.web("#e94560"));
            gc.setFont(Font.font(32));
            gc.fillText("GAME OVER", WINDOW_WIDTH / 2 - 80, WINDOW_HEIGHT / 2);
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}

/**
 * Manages the overall game state
 */
class GameState {
    private int gold = 500;
    private int lives = 20;
    private int currentWave = 1;
    private List<Tower> towers = new ArrayList<>();
    private List<Enemy> enemies = new ArrayList<>();
    private List<Projectile> projectiles = new ArrayList<>();
    private int windowWidth;
    private int windowHeight;
    private int waveCounter = 0;
    private boolean gameOver = false;
    
    public GameState(int width, int height) {
        this.windowWidth = width;
        this.windowHeight = height;
        spawnWave();
    }
    
    public void update() {
        if (gameOver) return;
        
        // Update enemies
        enemies.forEach(Enemy::update);
        enemies.removeIf(enemy -> enemy.reachedEnd);
        
        // Update projectiles
        projectiles.forEach(Projectile::update);
        projectiles.removeIf(projectile -> projectile.hasHit || projectile.isOffScreen(windowWidth, windowHeight));
        
        // Update towers
        towers.forEach(tower -> tower.update(enemies, projectiles));
        
        // Check for collisions between projectiles and enemies
        projectiles.forEach(projectile -> {
            enemies.stream()
                .filter(enemy -> projectile.collidesWith(enemy))
                .findFirst()
                .ifPresent(enemy -> {
                    enemy.takeDamage(projectile.damage);
                    projectile.hasHit = true;
                    if (enemy.isDead()) {
                        gold += enemy.goldReward;
                    }
                });
        });
        
        // Remove dead enemies
        enemies.removeIf(Enemy::isDead);
        
        // Check if enemies reached the end
        enemies.stream()
            .filter(enemy -> enemy.reachedEnd)
            .forEach(enemy -> lives--);
        
        // Spawn new wave
        waveCounter++;
        if (enemies.isEmpty() && waveCounter > 60) {
            currentWave++;
            spawnWave();
            waveCounter = 0;
        }
        
        // Check game over condition
        if (lives <= 0) {
            gameOver = true;
        }
    }
    
    private void spawnWave() {
        int enemyCount = 5 + currentWave * 2;
        for (int i = 0; i < enemyCount; i++) {
            enemies.add(new Enemy(50 + i * 30, 0, currentWave));
        }
    }
    
    public void placeTower(int gridX, int gridY) {
        int x = gridX * 40 + 20;
        int y = gridY * 40 + 20;
        
        // Check if tower placement is valid and affordable
        if (gold >= 100 && isValidPlacement(x, y)) {
            towers.add(new Tower(x, y));
            gold -= 100;
        }
    }
    
    private boolean isValidPlacement(int x, int y) {
        // Check if tower doesn't overlap with existing towers
        return towers.stream()
            .noneMatch(tower -> Math.hypot(tower.x - x, tower.y - y) < 40);
    }
    
    // Getters
    public int getGold() { return gold; }
    public int getLives() { return lives; }
    public int getCurrentWave() { return currentWave; }
    public List<Tower> getTowers() { return towers; }
    public List<Enemy> getEnemies() { return enemies; }
    public List<Projectile> getProjectiles() { return projectiles; }
    public boolean isGameOver() { return gameOver; }
}

/**
 * Represents a tower that shoots enemies
 */
class Tower {
    public int x, y;
    public int range = 150;
    public int fireRate = 10;
    public int fireCounter = 0;
    public int damage = 10;
    public boolean isSelected = false;
    
    public Tower(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    public void update(List<Enemy> enemies, List<Projectile> projectiles) {
        fireCounter++;
        
        if (fireCounter >= fireRate) {
            // Find nearest enemy in range
            Enemy target = enemies.stream()
                .filter(enemy -> Math.hypot(enemy.x - this.x, enemy.y - this.y) <= range)
                .min(Comparator.comparingDouble(e -> Math.hypot(e.x - this.x, e.y - this.y)))
                .orElse(null);
            
            if (target != null) {
                // Create projectile towards target
                double angle = Math.atan2(target.y - this.y, target.x - this.x);
                projectiles.add(new Projectile(this.x, this.y, angle, damage));
                fireCounter = 0;
            }
        }
    }
}

/**
 * Represents an enemy unit
 */
class Enemy {
    public int x, y;
    public int health;
    public int maxHealth;
    public int speed = 2;
    public int goldReward = 10;
    public boolean reachedEnd = false;
    private int pathX = 0;
    
    public Enemy(int startX, int startY, int wave) {
        this.x = startX;
        this.y = startY;
        this.maxHealth = 20 + wave * 5;
        this.health = maxHealth;
        this.speed = 1 + wave / 2;
    }
    
    public void update() {
        y += speed;
        if (y > 800) {
            reachedEnd = true;
        }
    }
    
    public void takeDamage(int damage) {
        health -= damage;
    }
    
    public boolean isDead() {
        return health <= 0;
    }
    
    public double getHealthPercentage() {
        return (double) health / maxHealth;
    }
}

/**
 * Represents a projectile fired by a tower
 */
class Projectile {
    public double x, y;
    public double vx, vy;
    public int damage;
    public boolean hasHit = false;
    private static final int PROJECTILE_SPEED = 8;
    
    public Projectile(int startX, int startY, double angle, int damage) {
        this.x = startX;
        this.y = startY;
        this.damage = damage;
        this.vx = PROJECTILE_SPEED * Math.cos(angle);
        this.vy = PROJECTILE_SPEED * Math.sin(angle);
    }
    
    public void update() {
        x += vx;
        y += vy;
    }
    
    public boolean collidesWith(Enemy enemy) {
        double distance = Math.hypot(enemy.x - this.x, enemy.y - this.y);
        return distance < 12;
    }
    
    public boolean isOffScreen(int width, int height) {
        return x < 0 || x > width || y < 0 || y > height;
    }
}
