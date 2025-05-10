public class QuidditchSimulation {
    private static final int FIELD_WIDTH = 200;
    private static final int FIELD_HEIGHT = 150;
    private static final int FIELD_DEPTH = 50;
    
    private static final int GOAL_WIDTH = 20;
    private static final int GOAL_HEIGHT = 30;
    
    private static final int MAX_TURN_COUNT = 1000;
    private static final double BALL_SPEED = 5.0;
    private static final double BLUDGER_SPEED = 7.0;
    
    private enum BallType {
        QUAFFLE,
        BLUDGER,
        GOLDEN_SNITCH
    }
    
    private enum PlayerRole {
        CHASER,
        BEATER,
        KEEPER,
        SEEKER
    }
    
    private class Ball {
        private BallType type;
        private double x, y, z;
        private double vx, vy, vz; // velocity components
        private boolean inPlay;
        
        public Ball(BallType type) {
            this.type = type;
            this.inPlay = true;
            resetPosition();
            resetVelocity();
        }
        
        public void resetPosition() {
            if (type == BallType.GOLDEN_SNITCH) {
                x = FIELD_WIDTH / 2;
                y = FIELD_HEIGHT / 2;
                z = FIELD_DEPTH / 2;
            } else {
                x = Math.random() * FIELD_WIDTH;
                y = Math.random() * FIELD_HEIGHT;
                z = Math.random() * FIELD_DEPTH;
            }
        }
        
        public void resetVelocity() {
            vx = Math.random() * BALL_SPEED * 2 - BALL_SPEED;
            vy = Math.random() * BALL_SPEED * 2 - BALL_SPEED;
            vz = Math.random() * BALL_SPEED * 2 - BALL_SPEED;
        }
        
        public void updatePosition() {
            if (!inPlay) return;
            
            // Update position based on velocity
            x += vx;
            y += vy;
            z += vz;
            
            // Handle boundary collisions
            if (x < 0 || x > FIELD_WIDTH) vx *= -0.8;
            if (y < 0 || y > FIELD_HEIGHT) vy *= -0.8;
            if (z < 0 || z > FIELD_DEPTH) vz *= -0.8;
            
            // Apply friction
            vx *= 0.99;
            vy *= 0.99;
            vz *= 0.99;
            
            // Keep ball within bounds
            x = Math.max(0, Math.min(FIELD_WIDTH, x));
            y = Math.max(0, Math.min(FIELD_HEIGHT, y));
            z = Math.max(0, Math.min(FIELD_DEPTH, z));
        }
    }
    
    private interface PlayerStrategy {
        void execute(Player player, Ball ball);
    }
    
    private class ChaserStrategy implements PlayerStrategy {
        @Override
        public void execute(Player player, Ball ball) {
            if (player.getTeam() == 'A') {
                // Move towards opponent's goal
                player.moveTowardsGoal(FIELD_WIDTH/2, FIELD_HEIGHT/2, FIELD_DEPTH/2, 2);
            } else {
                // Move towards own goal
                player.moveTowardsGoal(-FIELD_WIDTH/2, FIELD_HEIGHT/2, FIELD_DEPTH/2, 0);
            }
            
            // Try to catch the Quaffle
            if (ball.type == BallType.QUAFFLE) {
                double dx = ball.x - player.x;
                double dy = ball.y - player.y;
                double dz = ball.z - player.z;
                
                double distance = Math.sqrt(dx*dx + dy*dy + dz*dz);
                if (distance < 5) {
                    // Successfully caught the Quaffle
                    System.out.println("Player " + player.playerIndex + " caught the Quaffle!");
                }
            }
        }
    }
    
    private class BeaterStrategy implements PlayerStrategy {
        @Override
        public void execute(Player player, Ball ball) {
            if (ball.type == BallType.BLUDGER) {
                // Try to hit the bludger at opponents
                for (Player opponent : player.getTeam() == 'A' ? teamB : teamA) {
                    if (!opponent.isKnockedOut) {
                        double dx = opponent.x - ball.x;
                        double dy = opponent.y - ball.y;
                        double dz = opponent.z - ball.z;
                        
                        double distance = Math.sqrt(dx*dx + dy*dy + dz*dz);
                        if (distance < 15) {
                            // Hit the bludger towards the opponent
                            ball.vx = (dx / distance) * BLUDGER_SPEED;
                            ball.vy = (dy / distance) * BLUDGER_SPEED;
                            ball.vz = (dz / distance) * BLUDGER_SPEED;
                            System.out.println("Player " + player.playerIndex + " hit a bludger!");
                            break;
                        }
                    }
                }
            }
        }
    }
    
    private class KeeperStrategy implements PlayerStrategy {
        @Override
        public void execute(Player player, Ball ball) {
            // Defend the goal
            if (player.getTeam() == 'A') {
                player.moveTowardsGoal(-FIELD_WIDTH/2, FIELD_HEIGHT/2, FIELD_DEPTH/2, 0);
            } else {
                player.moveTowardsGoal(FIELD_WIDTH/2, FIELD_HEIGHT/2, FIELD_DEPTH/2, 2);
            }
            
            // Try to block the Quaffle
            if (ball.type == BallType.QUAFFLE) {
                double dx = ball.x - player.x;
                double dy = ball.y - player.y;
                double dz = ball.z - player.z;
                
                double distance = Math.sqrt(dx*dx + dy*dy + dz*dz);
                if (distance < 10) {
                    // Successfully blocked the Quaffle
                    System.out.println("Keeper " + player.playerIndex + " blocked a shot!");
                }
            }
        }
    }
    
    private class SeekerStrategy implements PlayerStrategy {
        @Override
        public void execute(Player player, Ball ball) {
            if (ball.type == BallType.GOLDEN_SNITCH) {
                // Move towards Golden Snitch
                double dx = ball.x - player.x;
                double dy = ball.y - player.y;
                double dz = ball.z - player.z;
                
                double distance = Math.sqrt(dx*dx + dy*dy + dz*dz);
                
                if (distance < 5) {
                    // Catch the Golden Snitch
                    ball.inPlay = false;
                    System.out.println("Seeker " + player.playerIndex + " caught the Golden Snitch!");
                } else if (distance < 30) {
                    // Move closer to the Snitch
                    player.move(dx/10, dy/10, dz/10, FIELD_WIDTH/2, FIELD_HEIGHT/2, FIELD_DEPTH/2);
                }
            }
        }
    }
    
    private class Player extends Vertex {
        private PlayerRole role;
        private boolean isKnockedOut;
        private PlayerStrategy strategy;
        private int stamina = 100; // Each player starts with 100 stamina
        
        public Player(double x, double y, double z, char team, PlayerRole role) {
            super(x, y, z, team);
            this.role = role;
            this.isKnockedOut = false;
            setStrategy();
        }
        
        private void setStrategy() {
            switch (role) {
                case CHASER:
                    strategy = new ChaserStrategy();
                    break;
                case BEATER:
                    strategy = new BeaterStrategy();
                    break;
                case KEEPER:
                    strategy = new KeeperStrategy();
                    break;
                case SEEKER:
                    strategy = new SeekerStrategy();
                    break;
            }
        }
        
        public char getTeam() {
            return team;
        }
        
        public void knockOut() {
            isKnockedOut = true;
            stamina = 0;
        }
        
        public void returnToPlay() {
            isKnockedOut = false;
            stamina = 100;
            // Return to team's goal area
            if (getTeam() == 'A') {
                move(-FIELD_WIDTH/4, 0, 0, FIELD_WIDTH/2, FIELD_HEIGHT/2, FIELD_DEPTH/2);
            } else {
                move(FIELD_WIDTH/4, 0, 0, FIELD_WIDTH/2, FIELD_HEIGHT/2, FIELD_DEPTH/2);
            }
        }
        
        public void executeTurn(Ball ball) {
            if (!isKnockedOut && stamina > 0) {
                strategy.execute(this, ball);
                stamina -= 1; // Decrease stamina for each action
            }
        }
    }
    
    private Player[] teamA = new Player[7];
    private Player[] teamB = new Player[7];
    private Ball quaffle;
    private Ball[] bludgers = new Ball[2];
    private Ball goldenSnitch;
    private int teamAScore = 0;
    private int teamBScore = 0;
    private int turnCount = 0;
    private boolean gameStarted = false;
    private boolean gameEnded = false;
    
    private class GameState {
        private int currentTurn;
        private int teamAScore;
        private int teamBScore;
        private boolean gameEnded;
        private boolean goldenSnitchCaught;
        private int teamAStaminaTotal;
        private int teamBStaminaTotal;
        private int knockedOutPlayersA;
        private int knockedOutPlayersB;
        
        public GameState() {
            reset();
        }
        
        public void update() {
            currentTurn = turnCount;
            this.teamAScore = QuidditchSimulation.this.teamAScore;
            this.teamBScore = QuidditchSimulation.this.teamBScore;
            gameStarted = QuidditchSimulation.this.gameStarted;
            gameEnded = QuidditchSimulation.this.gameEnded;
            goldenSnitchCaught = !goldenSnitch.inPlay;
            
            // Calculate team stamina totals
            teamAStaminaTotal = 0;
            teamBStaminaTotal = 0;
            knockedOutPlayersA = 0;
            knockedOutPlayersB = 0;
            
            for (Player player : teamA) {
                teamAStaminaTotal += player.stamina;
                if (player.isKnockedOut) knockedOutPlayersA++;
            }
            
            for (Player player : teamB) {
                teamBStaminaTotal += player.stamina;
                if (player.isKnockedOut) knockedOutPlayersB++;
            }
        }
        
        public void reset() {
            currentTurn = 0;
            teamAScore = 0;
            teamBScore = 0;
            gameStarted = false;
            gameEnded = false;
            goldenSnitchCaught = false;
            teamAStaminaTotal = 700; // 7 players * 100 stamina
            teamBStaminaTotal = 700;
            knockedOutPlayersA = 0;
            knockedOutPlayersB = 0;
        }
    }
    
    private GameState gameState = new GameState();
    
    public QuidditchSimulation() {
        initializePlayers();
        initializeBalls();
    }
    
    private void initializePlayers() {
        // Team A (left side)
        teamA[0] = new Player(-FIELD_WIDTH/4, 0, 0, 'A', PlayerRole.KEEPER);
        teamA[1] = new Player(-FIELD_WIDTH/4, FIELD_HEIGHT/4, FIELD_DEPTH/4, 'A', PlayerRole.CHASER);
        teamA[2] = new Player(-FIELD_WIDTH/4, -FIELD_HEIGHT/4, FIELD_DEPTH/4, 'A', PlayerRole.CHASER);
        teamA[3] = new Player(-FIELD_WIDTH/4, 0, -FIELD_DEPTH/4, 'A', PlayerRole.CHASER);
        teamA[4] = new Player(-FIELD_WIDTH/4, FIELD_HEIGHT/4, -FIELD_DEPTH/4, 'A', PlayerRole.BEATER);
        teamA[5] = new Player(-FIELD_WIDTH/4, -FIELD_HEIGHT/4, -FIELD_DEPTH/4, 'A', PlayerRole.BEATER);
        teamA[6] = new Player(-FIELD_WIDTH/4, 0, FIELD_DEPTH/2, 'A', PlayerRole.SEEKER);
        
        // Team B (right side)
        teamB[0] = new Player(FIELD_WIDTH/4, 0, 0, 'B', PlayerRole.KEEPER);
        teamB[1] = new Player(FIELD_WIDTH/4, FIELD_HEIGHT/4, FIELD_DEPTH/4, 'B', PlayerRole.CHASER);
        teamB[2] = new Player(FIELD_WIDTH/4, -FIELD_HEIGHT/4, FIELD_DEPTH/4, 'B', PlayerRole.CHASER);
        teamB[3] = new Player(FIELD_WIDTH/4, 0, -FIELD_DEPTH/4, 'B', PlayerRole.CHASER);
        teamB[4] = new Player(FIELD_WIDTH/4, FIELD_HEIGHT/4, -FIELD_DEPTH/4, 'B', PlayerRole.BEATER);
        teamB[5] = new Player(FIELD_WIDTH/4, -FIELD_HEIGHT/4, -FIELD_DEPTH/4, 'B', PlayerRole.BEATER);
        teamB[6] = new Player(FIELD_WIDTH/4, 0, FIELD_DEPTH/2, 'B', PlayerRole.SEEKER);
    }
    
    private void initializeBalls() {
        quaffle = new Ball(BallType.QUAFFLE);
        bludgers[0] = new Ball(BallType.BLUDGER);
        bludgers[1] = new Ball(BallType.BLUDGER);
        goldenSnitch = new Ball(BallType.GOLDEN_SNITCH);
    }
    
    public void startGame() {
        if (gameStarted) {
            System.out.println("Game already started!");
            return;
        }

        gameStarted = true;
        System.out.println("\n=== Quidditch Match Starting ===");
        System.out.println("Field dimensions: " + FIELD_WIDTH + "x" + FIELD_HEIGHT + "x" + FIELD_DEPTH);
        System.out.println("Each team has 7 players:");
        System.out.println("- 3 Chasers (score goals)");
        System.out.println("- 2 Beaters (control bludgers)");
        System.out.println("- 1 Keeper (defend goals)");
        System.out.println("- 1 Seeker (catch the Golden Snitch)");
        System.out.println("Game will run for a maximum of " + MAX_TURN_COUNT + " turns");
        System.out.println("Scoring:");
        System.out.println("- 10 points for each goal scored");
        System.out.println("- 150 points for catching the Golden Snitch");
        System.out.println("===============================\n");

        while (!isGameOver()) {
            simulateTurn();
            turnCount++;
            if (turnCount >= MAX_TURN_COUNT) {
                System.out.println("Maximum turn count reached - game ends in a draw!");
                break;
            }
        }

        printFinalScore();
    }
    
    private void simulateTurn() {
        // Update ball positions
        quaffle.updatePosition();
        bludgers[0].updatePosition();
        bludgers[1].updatePosition();
        goldenSnitch.updatePosition();
        
        // Team A turn
        System.out.println("\n=== Turn " + turnCount + " - Team A's turn ===");
        for (Player player : teamA) {
            if (!player.isKnockedOut) {
                player.executeTurn(quaffle);
            }
        }
        
        // Team B turn
        System.out.println("\n=== Turn " + turnCount + " - Team B's turn ===");
        for (Player player : teamB) {
            if (!player.isKnockedOut) {
                player.executeTurn(quaffle);
            }
        }
        
        // Handle ball interactions
        handleBludger(bludgers[0]);
        handleBludger(bludgers[1]);
        handleQuaffle();
        handleGoldenSnitch();
        
        // Check for scoring opportunities
        checkForScoring();
        handleBludger(bludgers[0]);
        handleBludger(bludgers[1]);
        handleQuaffle();
        handleGoldenSnitch();
        
        // Print game state
        gameState.update();
        printGameState();
    }
    
    private void handleBludger(Ball bludger) {
        // Beaters from both teams can hit the bludger
        for (Player player : teamA) {
            if (player.role == PlayerRole.BEATER && !player.isKnockedOut) {
                // Simple logic: if within range, hit the bludger
                double distance = Math.sqrt(
                    Math.pow(player.x - bludger.x, 2) +
                    Math.pow(player.y - bludger.y, 2) +
                    Math.pow(player.z - bludger.z, 2)
                );

                if (distance < 10) {
                    // Hit the bludger towards a random opponent
                    Player target = getRandomOpponent(player);
                    if (target != null) {
                        bludger.x = target.x;
                        bludger.y = target.y;
                        bludger.z = target.z;
                        target.knockOut();
                        System.out.println("Player " + player.playerIndex + " hit a bludger at " + target.playerIndex + "!");
                    }
                }
            }
        }

        for (Player player : teamB) {
            if (player.role == PlayerRole.BEATER && !player.isKnockedOut) {
                double distance = Math.sqrt(
                    Math.pow(player.x - bludger.x, 2) +
                    Math.pow(player.y - bludger.y, 2) +
                    Math.pow(player.z - bludger.z, 2)
                );

                if (distance < 10) {
                    Player target = getRandomOpponent(player);
                    if (target != null) {
                        bludger.x = target.x;
                        bludger.y = target.y;
                        bludger.z = target.z;
                        target.knockOut();
                        System.out.println("Player " + player.playerIndex + " hit a bludger at " + target.playerIndex + "!");
                    }
                }
            }
        }
    }
    
    private Player getRandomOpponent(Player player) {
        Player[] opponents = player.team == 'A' ? teamB : teamA;
        if (Math.random() < 0.3) { // 30% chance to hit
            return opponents[(int)(Math.random() * 7)];
        }
        return null;
    }
    
    private void handleQuaffle() {
        // Chasers try to score with the Quaffle
        for (Player player : teamA) {
            if (player.role == PlayerRole.CHASER && !player.isKnockedOut) {
                // Move towards opponent's goal
                player.moveTowardsGoal(FIELD_WIDTH/2, FIELD_HEIGHT/2, FIELD_DEPTH/2, 2);
                
                // Check if scored
                if (player.x >= FIELD_WIDTH/2 - GOAL_WIDTH/2 &&
                    player.x <= FIELD_WIDTH/2 + GOAL_WIDTH/2 &&
                    player.y >= -GOAL_HEIGHT/2 &&
                    player.y <= GOAL_HEIGHT/2 &&
                    player.z <= GOAL_HEIGHT/2) {
                    teamAScore += 10;
                    System.out.println("Team A scores 10 points!");
                    // Reset positions
                    for (Player p : teamA) {
                        p.returnToPlay();
                    }
                    // Reset ball position
                    quaffle.resetPosition();
                    quaffle.resetVelocity();
                }
            }
        }

        for (Player player : teamB) {
            if (player.role == PlayerRole.CHASER && !player.isKnockedOut) {
                // Move towards opponent's goal
                player.moveTowardsGoal(-FIELD_WIDTH/2, FIELD_HEIGHT/2, FIELD_DEPTH/2, 0);
                
                // Check if scored
                if (player.x <= -FIELD_WIDTH/2 + GOAL_WIDTH/2 &&
                    player.x >= -FIELD_WIDTH/2 - GOAL_WIDTH/2 &&
                    player.y >= -GOAL_HEIGHT/2 &&
                    player.y <= GOAL_HEIGHT/2 &&
                    player.z <= GOAL_HEIGHT/2) {
                    teamBScore += 15;
                    System.out.println("Team B scores 15 points!");
                    // Reset positions
                    for (Player p : teamB) {
                        p.returnToPlay();
                    }
                    // Reset ball position
                    quaffle.resetPosition();
                    quaffle.resetVelocity();
                }
            }
        }
    }
    
    private void handleGoldenSnitch() {
        // Seekers try to catch the Golden Snitch
        for (Player player : teamA) {
            if (player.role == PlayerRole.SEEKER && !player.isKnockedOut) {
                double dx = goldenSnitch.x - player.x;
                double dy = goldenSnitch.y - player.y;
                double dz = goldenSnitch.z - player.z;

                double distance = Math.sqrt(dx*dx + dy*dy + dz*dz);

                if (distance < 5) { // Catch radius
                    goldenSnitch.inPlay = false;
                    teamAScore += 150;
                    System.out.println("Team A catches the Golden Snitch! +150 points");
                    // End the game when Golden Snitch is caught
                    gameEnded = true;
                }
            }
        }

        for (Player player : teamB) {
            if (player.role == PlayerRole.SEEKER && !player.isKnockedOut) {
                double dx = goldenSnitch.x - player.x;
                double dy = goldenSnitch.y - player.y;
                double dz = goldenSnitch.z - player.z;

                double distance = Math.sqrt(dx*dx + dy*dy + dz*dz);

                if (distance < 5) { // Catch radius
                    goldenSnitch.inPlay = false;
                    teamBScore += 150;
                    System.out.println("Team B catches the Golden Snitch! +150 points");
                    // End the game when Golden Snitch is caught
                    gameEnded = true;
                }
            }
        }
    }
    
    private void printGameState() {
        System.out.println("\n=== Game State Summary ===");
        System.out.println("Turn: " + gameState.currentTurn);
        System.out.println("Score: Team A " + gameState.teamAScore + " - Team B " + gameState.teamBScore);

        System.out.println("\nTeam A Status:");
        System.out.println("Total Team Stamina: " + gameState.teamAStaminaTotal);
        System.out.println("Knocked Out Players: " + gameState.knockedOutPlayersA);
        System.out.println("Players In Play: " + (7 - gameState.knockedOutPlayersA));

        System.out.println("\nTeam B Status:");
        System.out.println("Total Team Stamina: " + gameState.teamBStaminaTotal);
        System.out.println("Knocked Out Players: " + gameState.knockedOutPlayersB);
        System.out.println("Players In Play: " + (7 - gameState.knockedOutPlayersB));

        System.out.println("\nBall Status:");
        System.out.println("Quaffle: " + (quaffle.inPlay ? "In Play" : "Out of Play"));
        System.out.println("Bludger 1: " + (bludgers[0].inPlay ? "In Play" : "Out of Play"));
        System.out.println("Bludger 2: " + (bludgers[1].inPlay ? "In Play" : "Out of Play"));
        System.out.println("Golden Snitch: " + (goldenSnitch.inPlay ? "In Play" : "Caught"));

        if (gameState.gameEnded) {
            System.out.println("\n=== Game Ended ===");
            if (gameState.goldenSnitchCaught) {
                System.out.println("Golden Snitch was caught!");
            } else {
                System.out.println("Maximum turn count reached - game ends in a draw!");
            }
        }
    }
    
    private boolean isGameOver() {
        return !goldenSnitch.inPlay || turnCount >= MAX_TURN_COUNT;
    }
    
    private void checkForScoring() {
        // Check for Quaffle scoring
        for (Player player : teamA) {
            if (player.role == PlayerRole.CHASER && !player.isKnockedOut) {
                if (player.x >= FIELD_WIDTH/2 - GOAL_WIDTH/2 &&
                    player.x <= FIELD_WIDTH/2 + GOAL_WIDTH/2 &&
                    player.y >= -GOAL_HEIGHT/2 &&
                    player.y <= GOAL_HEIGHT/2 &&
                    player.z <= GOAL_HEIGHT/2) {
                    teamAScore += 10;
                    System.out.println("Team A scores 10 points!");
                    // Reset positions
                    for (Player p : teamA) {
                        p.returnToPlay();
                    }
                }
            }
        }

        for (Player player : teamB) {
            if (player.role == PlayerRole.CHASER && !player.isKnockedOut) {
                if (player.x <= -FIELD_WIDTH/2 + GOAL_WIDTH/2 &&
                    player.x >= -FIELD_WIDTH/2 - GOAL_WIDTH/2 &&
                    player.y >= -GOAL_HEIGHT/2 &&
                    player.y <= GOAL_HEIGHT/2 &&
                    player.z <= GOAL_HEIGHT/2) {
                    teamBScore += 10;
                    System.out.println("Team B scores 10 points!");
                    // Reset positions
                    for (Player p : teamB) {
                        p.returnToPlay();
                    }
                }
            }
        }

        // Check for Golden Snitch catch
        for (Player player : teamA) {
            if (player.role == PlayerRole.SEEKER && !player.isKnockedOut) {
                double dx = goldenSnitch.x - player.x;
                double dy = goldenSnitch.y - player.y;
                double dz = goldenSnitch.z - player.z;

                double distance = Math.sqrt(dx*dx + dy*dy + dz*dz);

                if (distance < 5) { // Catch radius
                    goldenSnitch.inPlay = false;
                    teamAScore += 150;
                    System.out.println("Team A catches the Golden Snitch! +150 points");
                }
            }
        }

        for (Player player : teamB) {
            if (player.role == PlayerRole.SEEKER && !player.isKnockedOut) {
                double dx = goldenSnitch.x - player.x;
                double dy = goldenSnitch.y - player.y;
                double dz = goldenSnitch.z - player.z;

                double distance = Math.sqrt(dx*dx + dy*dy + dz*dz);

                if (distance < 5) { // Catch radius
                    goldenSnitch.inPlay = false;
                    teamBScore += 150;
                    System.out.println("Team B catches the Golden Snitch! +150 points");
                }
            }
        }
    }
    
    private void printFinalScore() {
        System.out.println("\n=== Final Score ===");
        System.out.println("Team A: " + teamAScore);
        System.out.println("Team B: " + teamBScore);
        
        if (teamAScore > teamBScore) {
            System.out.println("Team A wins!");
        } else if (teamBScore > teamAScore) {
            System.out.println("Team B wins!");
        } else {
            System.out.println("It's a draw!");
        }
        
        System.out.println("\n=== Game Statistics ===");
        System.out.println("Total turns played: " + turnCount);
        System.out.println("Final Team A stamina total: " + gameState.teamAStaminaTotal);
        System.out.println("Final Team B stamina total: " + gameState.teamBStaminaTotal);
        System.out.println("Team A knocked out players: " + gameState.knockedOutPlayersA);
        System.out.println("Team B knocked out players: " + gameState.knockedOutPlayersB);
    }
    
    public static void main(String[] args) {
        QuidditchSimulation simulation = new QuidditchSimulation();
        simulation.startGame();
    }
}