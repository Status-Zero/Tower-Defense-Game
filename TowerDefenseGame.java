import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.*;

/**
 * Complete JavaFX Tower Defense Game Implementation
 * Features: Towers, Enemies, Waves, Economy System, UI
 */
public class TowerDefenseGame extends Application {
    private static final int WIDTH = 1200;
    private static final int HEIGHT = 800;
    private static final int TILE_SIZE = 40;

    private GameState gameState;
    private Canvas gameCanvas;
    private AnimationTimer gameLoop;
    private Text statusText;

    @Override
    public void start(Stage primaryStage) {
        gameState = new GameState();
        
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #2a2a2a;");

        // Game Canvas
        gameCanvas = new Canvas(WIDTH - 250, HEIGHT);
        gameCanvas.setOnMouseClicked(this::handleCanvasClick);
        root.setCenter(gameCanvas);

        // Right Panel - UI
        VBox rightPanel = createRightPanel();
        root.setRight(rightPanel);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        primaryStage.setTitle("Tower Defense Game");
        primaryStage.setScene(scene);
        primaryStage.show();

        startGameLoop();
    }

    private VBox createRightPanel() {
        VBox panel = new VBox(10);
        panel.setPrefWidth(250);
        panel.setStyle("-fx-background-color: #1a1a1a; -fx-padding: 10;");

        // Status Display
        statusText = new Text();
        statusText.setFont(Font.font(14));
        statusText.setFill(Color.WHITE);

        // Towers Section
        VBox towersBox = new VBox(5);
        towersBox.setStyle("-fx-border-color: #444; -fx-border-width: 1; -fx-padding: 10;");
        Text towersTitle = new Text("Towers (Click to Select)");
        towersTitle.setFont(Font.font(14));
        towersTitle.setFill(Color.LIGHTBLUE);

        // Tower buttons
        Text basicTower = new Text("Basic: 100G");
        basicTower.setFont(Font.font(12));
        basicTower.setFill(Color.WHITE);
        basicTower.setOnMouseClicked(e -> gameState.setSelectedTower(TowerType.BASIC));

        Text rapidTower = new Text("Rapid: 150G");
        rapidTower.setFont(Font.font(12));
        rapidTower.setFill(Color.WHITE);
        rapidTower.setOnMouseClicked(e -> gameState.setSelectedTower(TowerType.RAPID));

        Text sniperTower = new Text("Sniper: 200G");
        sniperTower.setFont(Font.font(12));
        sniperTower.setFill(Color.WHITE);
        sniperTower.setOnMouseClicked(e -> gameState.setSelectedTower(TowerType.SNIPER));

        towersBox.getChildren().addAll(towersTitle, basicTower, rapidTower, sniperTower);

        panel.getChildren().addAll(statusText, towersBox);
        return panel;
    }

    private void handleCanvasClick(MouseEvent event) {
        int gridX = (int) (event.getX() / TILE_SIZE);
        int gridY = (int) (event.getY() / TILE_SIZE);

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
        GraphicsContext gc = gameCanvas.getGraphicsContext2D();
        gc.setFill(Color.web("#1a3a1a"));
        gc.fillRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());

        // Draw grid
        drawGrid(gc);

        // Draw towers
        for (Tower tower : gameState.getTowers()) {
            tower.render(gc, TILE_SIZE);
        }

        // Draw enemies
        for (Enemy enemy : gameState.getEnemies()) {
            enemy.render(gc);
        }

        // Draw projectiles
        for (Projectile projectile : gameState.getProjectiles()) {
            projectile.render(gc);
        }

        // Update status text
        updateStatusUI();
    }

    private void drawGrid(GraphicsContext gc) {
        gc.setStroke(Color.web("#333333"));
        gc.setLineWidth(0.5);

        int cols = (int) (gameCanvas.getWidth() / TILE_SIZE);
        int rows = (int) (gameCanvas.getHeight() / TILE_SIZE);

        for (int i = 0; i <= cols; i++) {
            gc.strokeLine(i * TILE_SIZE, 0, i * TILE_SIZE, gameCanvas.getHeight());
        }

        for (int i = 0; i <= rows; i++) {
            gc.strokeLine(0, i * TILE_SIZE, gameCanvas.getWidth(), i * TILE_SIZE);
        }
    }

    private void updateStatusUI() {
        statusText.setText(
            "Gold: " + gameState.getGold() + "\n" +
            "Health: " + gameState.getHealth() + "\n" +
            "Wave: " + gameState.getCurrentWave() + "\n" +
            "Enemies: " + gameState.getEnemies().size() + "\n" +
            "Towers: " + gameState.getTowers().size() + "\n" +
            "Selected: " + (gameState.getSelectedTower() != null ? gameState.getSelectedTower() : "None")
        );
    }

    public static void main(String[] args) {
        launch(args);
    }
}

// Game State Manager
class GameState {
    private int gold = 500;
    private int health = 100;
    private int currentWave = 1;
    private int enemiesSpawned = 0;
    private int maxEnemiesPerWave = 10;
    private long waveStartTime = 0;
    private long waveDelay = 3000; // 3 seconds between enemies

    private List<Tower> towers = new ArrayList<>();
    private List<Enemy> enemies = new ArrayList<>();
    private List<Projectile> projectiles = new ArrayList<>();
    private Set<String> occupiedTiles = new HashSet<>();

    private TowerType selectedTower = null;
    private Random random = new Random();

    public GameState() {
        waveStartTime = System.currentTimeMillis();
    }

    public void update() {
        // Spawn enemies
        if (enemies.size() < maxEnemiesPerWave && System.currentTimeMillis() - waveStartTime > waveDelay * enemiesSpawned) {
            if (enemiesSpawned < maxEnemiesPerWave) {
                spawnEnemy();
                enemiesSpawned++;
            }
        }

        // Update towers
        for (Tower tower : towers) {
            tower.update(enemies, projectiles);
        }

        // Update enemies
        List<Enemy> deadEnemies = new ArrayList<>();
        for (Enemy enemy : enemies) {
            enemy.update();
            if (enemy.isDead()) {
                deadEnemies.add(enemy);
                gold += enemy.getReward();
            }
            if (enemy.getProgress() >= 1.0) {
                deadEnemies.add(enemy);
                health -= enemy.getDamage();
            }
        }
        enemies.removeAll(deadEnemies);

        // Update projectiles
        List<Projectile> deadProjectiles = new ArrayList<>();
        for (Projectile projectile : projectiles) {
            projectile.update(enemies);
            if (projectile.isDead()) {
                deadProjectiles.add(projectile);
            }
        }
        projectiles.removeAll(deadProjectiles);

        // Wave management
        if (enemies.isEmpty() && enemiesSpawned >= maxEnemiesPerWave) {
            startNewWave();
        }

        // Game over check
        if (health <= 0) {
            health = 0;
        }
    }

    private void spawnEnemy() {
        enemies.add(new Enemy(0, 0, currentWave));
    }

    private void startNewWave() {
        currentWave++;
        enemiesSpawned = 0;
        maxEnemiesPerWave = Math.min(15, 10 + currentWave * 2);
        waveStartTime = System.currentTimeMillis();
    }

    public void placeTower(int gridX, int gridY) {
        if (selectedTower == null) return;

        String tileKey = gridX + "," + gridY;
        if (occupiedTiles.contains(tileKey)) return;

        int cost = selectedTower.getCost();
        if (gold >= cost) {
            gold -= cost;
            towers.add(new Tower(gridX * 40, gridY * 40, selectedTower));
            occupiedTiles.add(tileKey);
        }
    }

    public void setSelectedTower(TowerType type) {
        selectedTower = type;
    }

    // Getters
    public int getGold() { return gold; }
    public int getHealth() { return health; }
    public int getCurrentWave() { return currentWave; }
    public List<Tower> getTowers() { return towers; }
    public List<Enemy> getEnemies() { return enemies; }
    public List<Projectile> getProjectiles() { return projectiles; }
    public TowerType getSelectedTower() { return selectedTower; }
}

// Tower Type Enum
enum TowerType {
    BASIC(100, 40, 1.0, 5),
    RAPID(150, 30, 1.5, 3),
    SNIPER(200, 80, 0.5, 8);

    private final int cost;
    private final int range;
    private final double fireRate;
    private final int damage;

    TowerType(int cost, int range, double fireRate, int damage) {
        this.cost = cost;
        this.range = range;
        this.fireRate = fireRate;
        this.damage = damage;
    }

    public int getCost() { return cost; }
    public int getRange() { return range; }
    public double getFireRate() { return fireRate; }
    public int getDamage() { return damage; }
}

// Tower Class
class Tower {
    private double x, y;
    private TowerType type;
    private long lastShotTime = 0;
    private static final Color[] TOWER_COLORS = {Color.DARKBLUE, Color.DARKRED, Color.DARKGREEN};

    public Tower(double x, double y, TowerType type) {
        this.x = x + 20;
        this.y = y + 20;
        this.type = type;
    }

    public void update(List<Enemy> enemies, List<Projectile> projectiles) {
        long currentTime = System.currentTimeMillis();
        long fireInterval = (long) (1000 / type.getFireRate());

        if (currentTime - lastShotTime > fireInterval) {
            Enemy target = findTarget(enemies);
            if (target != null) {
                projectiles.add(new Projectile(x, y, target, type.getDamage()));
                lastShotTime = currentTime;
            }
        }
    }

    private Enemy findTarget(List<Enemy> enemies) {
        Enemy closest = null;
        double closestDistance = type.getRange();

        for (Enemy enemy : enemies) {
            double distance = distance(x, y, enemy.getX(), enemy.getY());
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = enemy;
            }
        }

        return closest;
    }

    private double distance(double x1, double y1, double x2, double y2) {
        return Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
    }

    public void render(GraphicsContext gc, int tileSize) {
        gc.setFill(TOWER_COLORS[type.ordinal()]);
        gc.fillOval(x - 15, y - 15, 30, 30);
        gc.setStroke(Color.YELLOW);
        gc.setLineWidth(2);
        gc.strokeOval(x - type.getRange(), y - type.getRange(), type.getRange() * 2, type.getRange() * 2);
    }
}

// Enemy Class
class Enemy {
    private double x, y;
    private double progress = 0;
    private int health;
    private int maxHealth;
    private static final int SPEED = 1;
    private static final Point2D[] PATH = {
        new Point2D(0, 100),
        new Point2D(200, 100),
        new Point2D(200, 300),
        new Point2D(500, 300),
        new Point2D(500, 500),
        new Point2D(800, 500),
        new Point2D(800, 100),
        new Point2D(950, 100)
    };

    public Enemy(double x, double y, int wave) {
        this.x = x;
        this.y = y;
        this.maxHealth = 20 + wave * 5;
        this.health = maxHealth;
    }

    public void update() {
        progress += SPEED / 500.0; // Normalized progress
        if (progress < 1.0) {
            updatePosition();
        }
    }

    private void updatePosition() {
        int pathIndex = (int) (progress * (PATH.length - 1));
        Point2D current = PATH[Math.min(pathIndex, PATH.length - 1)];
        Point2D next = PATH[Math.min(pathIndex + 1, PATH.length - 1)];

        double segmentProgress = (progress * (PATH.length - 1)) - pathIndex;
        x = current.getX() + (next.getX() - current.getX()) * segmentProgress;
        y = current.getY() + (next.getY() - current.getY()) * segmentProgress;
    }

    public void takeDamage(int damage) {
        health -= damage;
    }

    public void render(GraphicsContext gc) {
        gc.setFill(Color.RED);
        gc.fillOval(x - 10, y - 10, 20, 20);

        // Health bar
        gc.setFill(Color.GREEN);
        double healthPercentage = (double) health / maxHealth;
        gc.fillRect(x - 10, y - 15, 20 * healthPercentage, 3);
        gc.setStroke(Color.DARKGREEN);
        gc.strokeRect(x - 10, y - 15, 20, 3);
    }

    public boolean isDead() { return health <= 0; }
    public double getProgress() { return progress; }
    public double getX() { return x; }
    public double getY() { return y; }
    public int getDamage() { return 10; }
    public int getReward() { return 25; }
}

// Projectile Class
class Projectile {
    private double x, y;
    private Enemy target;
    private int damage;
    private double speed = 5;
    private boolean dead = false;

    public Projectile(double x, double y, Enemy target, int damage) {
        this.x = x;
        this.y = y;
        this.target = target;
        this.damage = damage;
    }

    public void update(List<Enemy> enemies) {
        if (target == null || target.isDead() || target.getProgress() >= 1.0) {
            dead = true;
            return;
        }

        double targetX = target.getX();
        double targetY = target.getY();
        double distance = Math.sqrt((targetX - x) * (targetX - x) + (targetY - y) * (targetY - y));

        if (distance < 15) {
            target.takeDamage(damage);
            dead = true;
        } else if (distance > 0) {
            x += (targetX - x) / distance * speed;
            y += (targetY - y) / distance * speed;
        }
    }

    public void render(GraphicsContext gc) {
        gc.setFill(Color.YELLOW);
        gc.fillOval(x - 5, y - 5, 10, 10);
    }

    public boolean isDead() { return dead; }
}
