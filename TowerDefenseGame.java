import java.util.*;

/**
 * Tower Defense Game
 * Main game logic class handling towers, enemies, and game mechanics
 */
public class TowerDefenseGame {
    private List<Tower> towers;
    private List<Enemy> enemies;
    private int playerHealth;
    private int playerGold;
    private static final int STARTING_HEALTH = 100;
    private static final int STARTING_GOLD = 500;

    public TowerDefenseGame() {
        this.towers = new ArrayList<>();
        this.enemies = new ArrayList<>();
        this.playerHealth = STARTING_HEALTH;
        this.playerGold = STARTING_GOLD;
    }

    /**
     * Update game state each frame
     */
    public void update(float deltaTime) {
        updateTowers(deltaTime);
        updateEnemies(deltaTime);
        removeDeadEnemies();
    }

    /**
     * Update all towers - handle targeting only visible enemies
     */
    private void updateTowers(float deltaTime) {
        for (Tower tower : towers) {
            tower.update(deltaTime);
            
            // Find nearest visible enemy to target
            Enemy target = findNearestVisibleEnemy(tower);
            if (target != null && tower.canShoot()) {
                tower.shoot(target);
            }
        }
    }

    /**
     * Find the nearest visible enemy within tower range
     * Only targets enemies that have completed their spawn delay
     */
    private Enemy findNearestVisibleEnemy(Tower tower) {
        Enemy nearest = null;
        float nearestDistance = tower.getRange();

        for (Enemy enemy : enemies) {
            // Only target visible enemies
            if (!enemy.isVisible()) {
                continue;
            }

            float distance = tower.getPosition().distance(enemy.getPosition());
            
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = enemy;
            }
        }

        return nearest;
    }

    /**
     * Update all enemies - handle movement and animation
     */
    private void updateEnemies(float deltaTime) {
        for (Enemy enemy : enemies) {
            enemy.update(deltaTime);
        }
    }

    /**
     * Remove dead enemies and award gold
     */
    private void removeDeadEnemies() {
        Iterator<Enemy> iterator = enemies.iterator();
        while (iterator.hasNext()) {
            Enemy enemy = iterator.next();
            if (enemy.isDead()) {
                playerGold += enemy.getGoldReward();
                iterator.remove();
            }
        }
    }

    /**
     * Spawn a new enemy at the start position
     */
    public void spawnEnemy(Enemy enemy) {
        enemies.add(enemy);
    }

    /**
     * Place a tower at the specified position
     */
    public void placeTower(Tower tower) {
        if (playerGold >= tower.getCost()) {
            towers.add(tower);
            playerGold -= tower.getCost();
        }
    }

    /**
     * Get list of all towers
     */
    public List<Tower> getTowers() {
        return new ArrayList<>(towers);
    }

    /**
     * Get list of all enemies
     */
    public List<Enemy> getEnemies() {
        return new ArrayList<>(enemies);
    }

    /**
     * Get current player health
     */
    public int getPlayerHealth() {
        return playerHealth;
    }

    /**
     * Get current player gold
     */
    public int getPlayerGold() {
        return playerGold;
    }

    /**
     * Set player health
     */
    public void setPlayerHealth(int health) {
        this.playerHealth = Math.max(0, health);
    }

    /**
     * Tower class - handles tower behavior and targeting
     */
    public static class Tower {
        private Vector2 position;
        private int cost;
        private float range;
        private float fireRate;
        private float shootCooldown;

        public Tower(Vector2 position, int cost, float range, float fireRate) {
            this.position = position;
            this.cost = cost;
            this.range = range;
            this.fireRate = fireRate;
            this.shootCooldown = 0;
        }

        /**
         * Update tower cooldown
         */
        public void update(float deltaTime) {
            if (shootCooldown > 0) {
                shootCooldown -= deltaTime;
            }
        }

        /**
         * Check if tower can shoot
         */
        public boolean canShoot() {
            return shootCooldown <= 0;
        }

        /**
         * Shoot at target enemy
         */
        public void shoot(Enemy target) {
            if (target != null && canShoot()) {
                target.takeDamage(10);
                shootCooldown = 1.0f / fireRate;
            }
        }

        public Vector2 getPosition() {
            return position;
        }

        public float getRange() {
            return range;
        }

        public int getCost() {
            return cost;
        }
    }

    /**
     * Enemy class - handles enemy behavior and health
     */
    public static class Enemy {
        private Vector2 position;
        private Vector2 velocity;
        private int health;
        private int maxHealth;
        private int goldReward;
        private float spawnDelay;
        private float elapsedTime;

        public Enemy(Vector2 position, int health, int goldReward, float spawnDelay) {
            this.position = position;
            this.velocity = new Vector2(0, 0);
            this.health = health;
            this.maxHealth = health;
            this.goldReward = goldReward;
            this.spawnDelay = spawnDelay;
            this.elapsedTime = 0;
        }

        /**
         * Update enemy state
         */
        public void update(float deltaTime) {
            // Update spawn delay timer
            if (elapsedTime < spawnDelay) {
                elapsedTime += deltaTime;
                return; // Don't update position during spawn delay
            }

            // Update position after spawn delay
            position.x += velocity.x * deltaTime;
            position.y += velocity.y * deltaTime;
        }

        /**
         * Check if enemy is visible (has completed spawn delay)
         */
        public boolean isVisible() {
            return elapsedTime >= spawnDelay;
        }

        /**
         * Apply damage to enemy
         */
        public void takeDamage(int damage) {
            health -= damage;
        }

        /**
         * Check if enemy is dead
         */
        public boolean isDead() {
            return health <= 0;
        }

        public Vector2 getPosition() {
            return position;
        }

        public void setVelocity(Vector2 velocity) {
            this.velocity = velocity;
        }

        public int getGoldReward() {
            return goldReward;
        }

        public int getHealth() {
            return health;
        }

        public int getMaxHealth() {
            return maxHealth;
        }

        public float getSpawnDelay() {
            return spawnDelay;
        }

        public float getElapsedTime() {
            return elapsedTime;
        }
    }

    /**
     * Vector2 utility class for positions and velocities
     */
    public static class Vector2 {
        public float x;
        public float y;

        public Vector2(float x, float y) {
            this.x = x;
            this.y = y;
        }

        /**
         * Calculate distance to another vector
         */
        public float distance(Vector2 other) {
            float dx = this.x - other.x;
            float dy = this.y - other.y;
            return (float) Math.sqrt(dx * dx + dy * dy);
        }

        @Override
        public String toString() {
            return String.format("(%.1f, %.1f)", x, y);
        }
    }
}
