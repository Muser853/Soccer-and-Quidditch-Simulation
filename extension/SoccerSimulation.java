import java.util.*;
import java.util.stream.Collectors;

public final class SoccerSimulation {
    public static final int penaltyAreaDistance = 25;
    public static final int boundX = 100;

    public static class SimulationResult {
        public SoccerStrategy winner;
        public int totalIterations;
        public Map<SoccerStrategy, Integer> strategyScores;
        public Map<Character, Integer> teamScores;
        public int successfulPasses;
        public int failedPasses;
        public double totalMovingDistance;

        public SimulationResult(SoccerStrategy winningStrategy, int totalIterations, 
                               Map<SoccerStrategy, Integer> strategyScores, 
                               Map<Character, Integer> teamScores,
                               int successfulPasses, int failedPasses, 
                               double totalMovingDistance) {
            this.winner = winningStrategy;
            this.totalIterations = totalIterations;
            this.strategyScores = strategyScores;
            this.teamScores = teamScores;
            this.successfulPasses = successfulPasses;
            this.failedPasses = failedPasses;
            this.totalMovingDistance = totalMovingDistance;
        }
    }
    public enum StartingScenario {
        KICK_OFF, // Standard kickoff from center
        CORNER_KICK,   // Corner kick
        GOAL_KICK      // Goal kick
    }
    public double bound;
    public double adjacentRadius;
    protected final List<List<Vertex>> teams;// Lists to store players from different teams
    protected final List<Vertex> players; // List of all players in the simulation
    protected Vertex ballController;
    protected int currentTeamWithBall;
    protected int successfulPasses;
    protected int failedPasses;
    protected int numGoals;
    protected double totalMovingDistance;
    protected boolean is3D;
    protected Map<Character, Integer> teamGoals; // Maps team ID to goal index

    public SoccerSimulation(double width, double height, double adjacentRadius, boolean is3D) {
        bound = height / 2;
        bound = is3D ? height / 2 : 0;
        this.adjacentRadius = adjacentRadius;
        teams = new ArrayList<>();
        players = new ArrayList<>();
        ballController = null;
        currentTeamWithBall = -1;
        successfulPasses = 0;
        failedPasses = 0;
        totalMovingDistance = 0;
        numGoals = 2; // Default number of goals for 2D
        this.is3D = is3D;
        teamGoals = new HashMap<>();
    }
    
    public SoccerSimulation(double width, double height) {
        this(width, height, 10.0, false);
    }
    
    public SoccerSimulation(double bound, double adjacentRadius, int numGoals) {
        this.bound = bound;
        this.adjacentRadius = adjacentRadius;
        teams = new ArrayList<>();
        players = new ArrayList<>();
        ballController = null;
        currentTeamWithBall = -1;
        successfulPasses = 0;
        failedPasses = 0;
        totalMovingDistance = 0;
        this.numGoals = numGoals;
        is3D = true;
        teamGoals = new HashMap<>();
    }
    public void resetStatistics() {
        this.successfulPasses = 0;
        this.failedPasses = 0;
        this.totalMovingDistance = 0;
    }
    
    public void initializeTeams(List<Integer> teamSizes, StartingScenario scenario) {
        teams.clear();
        
        for (int t = 0; t < teamSizes.size(); t++) {
            int teamSize = teamSizes.get(t);
            char teamId = (char)('A' + t); // Team A, B, C, etc.
            List<Vertex> team = new ArrayList<>();
            
            // Determine the goal index for this team
            int goalIndex = t % numGoals;
            teamGoals.put(teamId, goalIndex);
            
            // Calculate initial positions based on scenario and team goal
            switch (scenario) {
                case KICK_OFF:
                    if (is3D) {
                        // 3D Kick Off: Players in straight lines
                        if (numGoals == 2) {
                            // For 2-team 3D simulation
                            int numLines = (int) Math.ceil(Math.sqrt(teamSize));
                            double lineSpacing = bound / numLines;
                            
                            for (int i = 0; i < teamSize; i++) {
                                int line = i / numLines;
                                int withinLine = i % numLines;
                                double y = -bound/2 + line * lineSpacing;
                                double x = -50 + (withinLine * 200.0 / (numLines + 1));
                                double z = x;
                                
                                Vertex player = new Vertex(x, y, z, teamId);
                                player.playerIndex = i;
                                player.goalIndex = goalIndex;
                                team.add(player);
                            }
                        } else {
                            // For multi-team 3D simulation
                            // Determine the cuboid space based on goal index
                            double minX, maxX, minY, maxY, minZ, maxZ;
                            switch (goalIndex) {
                                case 4: // z = bound
                                    minX = -50; maxX = 50;
                                    minY = -bound/2; maxY = bound/2;
                                    minZ = bound/2; maxZ = bound;
                                    break;
                                case 5: // z = -bound
                                    minX = -50; maxX = 50;
                                    minY = -bound/2; maxY = bound/2;
                                    minZ = -bound; maxZ = -bound/2;
                                    break;
                                case 0: // y = -bound
                                    minX = -50; maxX = 50;
                                    minY = -bound; maxY = -bound/2;
                                    minZ = -bound/2; maxZ = bound/2;
                                    break;
                                case 1: // y = bound
                                    minX = -50; maxX = 50;
                                    minY = bound/2; maxY = bound;
                                    minZ = -bound/2; maxZ = bound/2;
                                    break;
                                case 2: // x = 100
                                    minX = 50; maxX = 100;
                                    minY = -bound/2; maxY = bound/2;
                                    minZ = -bound/2; maxZ = bound/2;
                                    break;
                                case 3: // x = -100
                                    minX = -100; maxX = -50;
                                    minY = -bound/2; maxY = bound/2;
                                    minZ = -bound/2; maxZ = bound/2;
                                    break;
                                default:
                                    throw new IllegalArgumentException("Invalid goal index: " + goalIndex);
                            }
                            
                            // Place players evenly in the cuboid
                            int numX = (int) Math.ceil(Math.sqrt(teamSize));
                            int numY = (int) Math.ceil(Math.sqrt(teamSize));
                            int numZ = (int) Math.ceil(Math.sqrt(teamSize));
                            
                            for (int i = 0; i < teamSize; i++) {
                                int xIndex = i % numX;
                                int yIndex = (i / numX) % numY;
                                int zIndex = i / (numX * numY);
                                
                                double x = minX + (maxX - minX) * ((xIndex + 1.0) / (numX + 1));
                                double y = minY + (maxY - minY) * ((yIndex + 1.0) / (numY + 1));
                                double z = minZ + (maxZ - minZ) * ((zIndex + 1.0) / (numZ + 1));
                                
                                Vertex player = new Vertex(x, y, z, teamId);
                                player.playerIndex = i;
                                player.goalIndex = goalIndex;
                                team.add(player);
                            }
                        }
                    } else {
                        // 2D Kick Off: Players in straight lines
                        int numLines = (int) Math.ceil(Math.sqrt(teamSize));
                        double lineSpacing = bound / numLines;
                        
                        for (int i = 0; i < teamSize; i++) {
                            int line = i / numLines;
                            int withinLine = i % numLines;
                            double y = -bound/2 + line * lineSpacing;
                            double x = -50 + (withinLine * 200.0 / (numLines + 1));
                            
                            Vertex player = new Vertex(x, y, 0, teamId);
                            player.playerIndex = i;
                            player.goalIndex = goalIndex;
                            team.add(player);
                        }
                    }
                    break;
                
                case CORNER_KICK:
                    double cornerX = getGoalCoordinates(goalIndex)[0];
                    double cornerY = getGoalCoordinates(goalIndex)[1];
                    
                    for (int i = 0; i < teamSize; i++) {
                        double angle = 2 * Math.PI * i / teamSize;
                        double radius = 20;
                        double x = cornerX + radius * Math.cos(angle);
                        double y = cornerY + radius * Math.sin(angle);
                        double z = is3D ? x : 0;
                        
                        Vertex player = new Vertex(x, y, z, teamId);
                        player.playerIndex = i;
                        player.goalIndex = goalIndex;
                        team.add(player);
                    }
                    break;
                
                case GOAL_KICK:
                    // Position players in a defensive formation
                    double goalX = getGoalCoordinates(goalIndex)[0];
                    double goalY = getGoalCoordinates(goalIndex)[1];
                    
                    for (int i = 0; i < teamSize; i++) {
                        double angle = 2 * Math.PI * i / teamSize;
                        double radius = 15;
                        double x = goalX + radius * Math.cos(angle);
                        double y = goalY + radius * Math.sin(angle);
                        double z = is3D ? x : 0;
                        
                        Vertex player = new Vertex(x, y, z, teamId);
                        player.playerIndex = i;
                        player.goalIndex = goalIndex;
                        team.add(player);
                    }
                    break;
            }
            teams.add(team);
        }
        
        // Update players list
        players.clear();
        for (List<Vertex> team : teams) {
            players.addAll(team);
        }
        // Set initial ball controller based on scenario
        currentTeamWithBall = (scenario == StartingScenario.GOAL_KICK || 
                             scenario == StartingScenario.CORNER_KICK) ? 0 : 1;
        ballController = teams.get(currentTeamWithBall).get(0);
        ballController.hasBall = true;
        
        updatePlayersList();
    }
    
    public void initializeTeamsWithFormations(List<Integer> teamSizes, List<List<Integer>> formations, StartingScenario scenario) {
        teams.clear();
        for (int t = 0; t < teamSizes.size(); t++) {
            int teamSize = teamSizes.get(t);
            char teamId = (char)('A' + t); // Team A, B, C, etc.
            List<Vertex> team = new ArrayList<>();
            
            // Create players with initial positions (0,0)
            for (int i = 0; i < teamSize; i++) {
                Vertex player = new Vertex(0, 0, teamId);
                player.playerIndex = i;
                team.add(player);
            }
            positionPlayersInFormation(team, formations.get(t), t);
            adjustPositionsForScenario(team, scenario, t);
            teams.add(team);
        }
        
        // Set initial ball controller based on scenario
        currentTeamWithBall = (scenario == StartingScenario.GOAL_KICK || 
                             scenario == StartingScenario.CORNER_KICK) ? 0 : 1;
        ballController = teams.get(currentTeamWithBall).get(0);
        ballController.hasBall = true;
        
        // Update the players list
        updatePlayersList();
    }
    
    private void positionPlayersInFormation(List<Vertex> team, List<Integer> formation, int teamIndex) {
        int playerIndex = 0;
        int numLines = formation.size();

        // Position players in each line
        for (int lineIndex = 0; lineIndex < numLines; lineIndex++) {
            int playersInLine = formation.get(lineIndex);
            
            // Calculate y-spacing for this line
            double lineY = -bound + (bound * 2.0 * (lineIndex + 1)) / (numLines + 1);
            
            // Position players in this line
            for (int i = 0; i < playersInLine; i++) {
                if (playerIndex < team.size()) {
                    Vertex player = team.get(playerIndex);
                    
                    // Calculate x-position with spacing
                    double spacing = 2000.0 / (playersInLine + 1);
                    double playerX = -1000 + spacing * (i + 1);
                    
                    // Set position
                    player.x = (int) playerX;
                    player.y = (int) lineY;
                    
                    playerIndex++;
                }
            }
        }ensureMinimumDistance(team);
    }
    
    private void ensureMinimumDistance(List<Vertex> team) {
        boolean changed;
        do {
            changed = false;
            for (int i = 0; i < team.size(); i++) {
                for (int j = i + 1; j < team.size(); j++) {
                    Vertex p1 = team.get(i);
                    Vertex p2 = team.get(j);
                    
                    double dx = p1.x - p2.x;
                    double dy = p1.y - p2.y;
                    double distance = Math.sqrt(dx * dx + dy * dy);
                    
                    if (distance < adjacentRadius) {
                        // Move players apart
                        double moveX = (dx / distance) * (adjacentRadius - distance) / 2;
                        double moveY = (dy / distance) * (adjacentRadius - distance) / 2;
                        
                        p1.x += moveX;
                        p1.y += moveY;
                        p2.x -= moveX;
                        p2.y -= moveY;
                        
                        // Ensure within boundaries
                        p1.x = Math.max(-boundX, Math.min(boundX, p1.x));
                        p1.y = Math.max(-bound, Math.min(bound, p1.y));
                        p2.x = Math.max(-boundX, Math.min(boundX, p2.x));
                        p2.y = Math.max(-bound, Math.min(bound, p2.y));
                        
                        changed = true;
                    }
                }
            }
        } while (changed);
    }
    
    private void adjustPositionsForScenario(List<Vertex> team, StartingScenario scenario, int teamIndex) {
        switch (scenario) {
            case GOAL_KICK:
                if (teamIndex == 0) {
                    for (Vertex player : team) {
                        player.x = (int)(player.x * 0.3);
                        player.y = (int)(player.y * 0.8);
                    }
                } else {
                    for (Vertex player : team) {
                        player.x = (int)(player.x * 0.7);
                        player.y = (int)(player.y * 0.6);
                    }
                }
                break;
                
            case CORNER_KICK:
                if (teamIndex == 0) {
                    for (Vertex player : team) {
                        if (player.playerIndex == 0) {
                            player.x = boundX - 5;
                            player.y = bound - 5;
                        } else {
                            // Other players in attacking position
                            player.x = (int)(player.x * 0.7 + boundX * 0.2);
                            player.y = (int)(player.y * 0.6 + bound * 0.2);
                        }
                    }
                } else {
                    // Defending team in defensive position
                    for (Vertex player : team) {
                        player.x = (int)(player.x * 0.4);
                        player.y = (int)(player.y * 0.4);
                    }
                }
                break;
                
            case KICK_OFF:
                if (teamIndex == 0) {
                    for (Vertex player : team) {
                        if (player.playerIndex == 0) {
                            // Center spot
                            player.x = 0;
                            player.y = 0;
                        } else {
                            // Own half
                            player.x = (int)(player.x * 0.8 - boundX * 0.1);
                            player.y = (int)(player.y * 0.8);
                        }
                    }
                } else {
                    // Other team in their half
                    for (Vertex player : team) {
                        player.x = (int)(player.x * 0.8 + boundX * 0.1);
                        player.y = (int)(player.y * 0.8);
                    }
                }
                break;
        }
    }
    // Update the players list whenever teams are modified
    private void updatePlayersList() {
        players.clear();
        for (List<Vertex> team : teams) {
            players.addAll(team);
        }
    }
    
    public List<Vertex> getTeammates(Vertex player) {
        for (List<Vertex> team : teams) {
            if (!team.isEmpty() && team.get(0).team == player.team) {
                return team.stream()
                    .filter(p -> p != player)
                    .collect(Collectors.toList());
            }
        }return new ArrayList<>();
    }
    public List<Vertex> getOpponents(Vertex player) {
        List<Vertex> opponents = new ArrayList<>();
        for (List<Vertex> team : teams) {
            if (!team.isEmpty() && team.get(0).team != player.team) {
                opponents.addAll(team);
            }
        }
        return opponents;
    }
    

    public double distanceBetween(Vertex player1, Vertex player2) {
        double dx = player1.x - player2.x;
        double dy = player1.y - player2.y;
        double dz = player1.z - player2.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public double distanceToOwnGoal(Vertex player) {
        double goalX = 0;
        double goalY = (player.team == 'A') ? -bound : bound;
        double goalZ = 0;
        return Math.sqrt(Math.pow(player.x - goalX, 2) + Math.pow(player.y - goalY, 2) + Math.pow(player.z - goalZ, 2));
    }

    public double[] getGoalCoordinates(int goalIndex) {
        double[] coordinates = new double[3];
        switch (goalIndex) {
            case 0: // x = 0, y = -bound, z = 0
                coordinates[0] = 0;
                coordinates[1] = -bound;
                coordinates[2] = 0;
                break;
            case 1: // x = 0, y = bound, z = 0
                coordinates[0] = 0;
                coordinates[1] = bound;
                coordinates[2] = 0;
                break;
            case 2: // x = 100, y = 0, z = 0
                coordinates[0] = 100;
                coordinates[1] = 0;
                coordinates[2] = 0;
                break;
            case 3: // x = -100, y = 0, z = 0
                coordinates[0] = -100;
                coordinates[1] = 0;
                coordinates[2] = 0;
                break;
            case 4: // x = 0, y = 0, z = bound (for 3D)
                coordinates[0] = 0;
                coordinates[1] = 0;
                coordinates[2] = bound;
                break;
            case 5: // x = 0, y = 0, z = -bound (for 3D)
                coordinates[0] = 0;
                coordinates[1] = 0;
                coordinates[2] = -bound;
        }
        return coordinates;
    }

    public void movePlayer(Vertex player, double dx, double dy, double dz) {
        // Calculate the distance moved
        double distance = Math.sqrt(dx*dx + dy*dy + dz*dz);
        totalMovingDistance += distance;
        player.move(dx, dy, dz, boundX, bound, bound);
    }

    public void movePlayer(Vertex player, double dx, double dy) {
        movePlayer(player, dx, dy, 0);
    }

    public void movePlayerTowardsOpponentGoal(Vertex player) {
        // Move towards the closest opponent goal
        int teamGoalIndex = getOpponentGoalIndex(player.team);
        
        // Find the closest opponent goal
        int closestGoalIndex = teamGoalIndex;
        double minDistance = Double.MAX_VALUE;
        
        for (int i = 0; i < numGoals; i++) {
            if (i != teamGoalIndex) {
                double[] goalCoords = getGoalCoordinates(i);
                double distance = Math.sqrt(
                    Math.pow(player.x - goalCoords[0], 2) +
                    Math.pow(player.y - goalCoords[1], 2) +
                    Math.pow(player.z - goalCoords[2], 2)
                );
                if (distance < minDistance) {
                    minDistance = distance;
                    closestGoalIndex = i;
                }
            }
        }
        
        // Move towards the closest goal
        double[] goalCoords = getGoalCoordinates(closestGoalIndex);
        double dx = goalCoords[0] - player.x;
        double dy = goalCoords[1] - player.y;
        double dz = goalCoords[2] - player.z;
        
        // Normalize direction
        double magnitude = Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (magnitude > 0) {
            dx /= magnitude;
            dy /= magnitude;
            dz /= magnitude;
        }
        
        // Move 0.125 units in that direction
        player.move(dx * 0.125, dy * 0.125, dz * 0.125, boundX, bound, bound);
    }

    public boolean canPass(Vertex passer, Vertex receiver) {
        // Calculate social radius for this pass
        double dx = passer.x - receiver.x;
        double dy = passer.y - receiver.y;
        double dz = passer.z - receiver.z;
        double distance = Math.sqrt(dx*dx + dy*dy + dz*dz);
        double socialRadius = Math.pow(distance, 0.25); // As per requirements: ((passer.x-receiver.x)^2 + (passer.y - receiver.y)^2)^(1/4)
        
        // Count opponents within social radius of the receiver
        List<Vertex> opponents = getOpponents(receiver);
        int socialCount = 0;
        for (Vertex opponent : opponents) {
            if (distanceBetween(receiver, opponent) <= socialRadius) {
                socialCount++;
            }
        }
        // Check if pass is possible based on social count
        return socialCount <= 1; // Guaranteed if 0, 50% chance if 1, impossible if >1
    }

    public void movePlayerAwayFromOpponents(Vertex player, List<Vertex> opponents) {
        double dx = 0, dy = 0, dz = 0;
        
        for (Vertex opponent : opponents) {
            // Calculate repulsion force
            double repulsion = 1.0 / Math.sqrt(
                Math.pow(player.x - opponent.x, 2) +
                Math.pow(player.y - opponent.y, 2) +
                Math.pow(player.z - opponent.z, 2)
            );
            
            dx += repulsion * (player.x - opponent.x);
            dy += repulsion * (player.y - opponent.y);
            dz += repulsion * (player.z - opponent.z);
        }
        
        if (dx != 0 || dy != 0 || dz != 0) {
            // Normalize the direction vector
            double magnitude = Math.sqrt(dx*dx + dy*dy + dz*dz);
            if (magnitude > 0) {
                dx /= magnitude;
                dy /= magnitude;
                dz /= magnitude;
            }
        }
    }

    public void movePlayerToBlockPassingLanes(Vertex player, Vertex ballController, List<Vertex> opponents) {
        List<Vertex> controllerTeammates = getTeammates(ballController);
        
        Vertex closestPassTarget = null;
        double minDistance = Double.MAX_VALUE;
        
        for (Vertex teammate : controllerTeammates) {
            if (canPass(ballController, teammate)) {
                double dist = distanceBetween(player, teammate);
                if (dist < minDistance) {
                    minDistance = dist;
                    closestPassTarget = teammate;
                }
            }
        }
        if (closestPassTarget != null) {
            // Calculate the midpoint between ball controller and pass target
            double midX = (ballController.x + closestPassTarget.x) / 2;
            double midY = (ballController.y + closestPassTarget.y) / 2;
            double midZ = (ballController.z + closestPassTarget.z) / 2;
            
            // Move towards this midpoint to block the passing lane
            double dx = Double.compare(midX, player.x);
            double dy = Double.compare(midY, player.y);
            double dz = Double.compare(midZ, player.z);
            movePlayer(player, dx, dy, dz);
        } else {
            movePlayerTowards(player, ballController);
        }
    }

    private int getOpponentGoalIndex(char team) {
        // Find the goal index that this team is trying to score in
        for (Map.Entry<Character, Integer> entry : teamGoals.entrySet()) {
            if (entry.getKey() == team) {
                return entry.getValue();
            }
        }
        return 0; // Default to first goal if not found
    }

    public void movePlayerTowards(Vertex player, Vertex target) {
        double dx = target.x - player.x;
        double dy = target.y - player.y;
        double dz = target.z - player.z;
        
        // Normalize direction
        double magnitude = Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (magnitude > 0) {
            dx /= magnitude;
            dy /= magnitude;
            dz /= magnitude;
        }
        
        // Move 0.125 units in that direction
        movePlayer(player, dx * 0.125, dy * 0.125, dz * 0.125);
    }

    public void movePlayerSlightlyTowards(Vertex player, Vertex target) {
        double dx = target.x - player.x;
        double dy = target.y - player.y;
        double dz = target.z - player.z;
        
        // Normalize direction
        double magnitude = Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (magnitude > 0) {
            dx /= magnitude;
            dy /= magnitude;
            dz /= magnitude;
        }
    }

    public boolean breakthrough(Vertex player) {
        // Move forward by 1 unit
        double dx = 1;
        double dy = 0;
        double dz = 0;
        
        // Check if there are opponents closer to their goal
        List<Vertex> opponents = getOpponents(player);
        boolean hasCloserOpponent = false;
        
        for (Vertex opponent : opponents) {
            int opponentGoalIndex = getOpponentGoalIndex(opponent.team);
            double[] opponentGoalCoords = getGoalCoordinates(opponentGoalIndex);
            double opponentDistanceToGoal = Math.sqrt(
                Math.pow(opponent.x - opponentGoalCoords[0], 2) +
                Math.pow(opponent.y - opponentGoalCoords[1], 2) +
                Math.pow(opponent.z - opponentGoalCoords[2], 2)
            );
            
            int playerGoalIndex = getOpponentGoalIndex(player.team);
            double[] playerGoalCoords = getGoalCoordinates(playerGoalIndex);
            double playerDistanceToGoal = Math.sqrt(
                Math.pow(player.x - playerGoalCoords[0], 2) +
                Math.pow(player.y - playerGoalCoords[1], 2) +
                Math.pow(player.z - playerGoalCoords[2], 2)
            );
            
            if (opponentDistanceToGoal < playerDistanceToGoal) {
                hasCloserOpponent = true;
                break;
            }
        }
        
        if (hasCloserOpponent && Math.random() < 0.5) {
            // Lose the ball
            List<Vertex> nearbyOpponents = opponents.stream()
                .filter(opponent -> distanceBetween(player, opponent) <= adjacentRadius)
                .collect(Collectors.toList());
            
            if (!nearbyOpponents.isEmpty()) {
                Vertex closestOpponent = nearbyOpponents.get(0);
                closestOpponent.hasBall = true;
                player.hasBall = false;
                ballController = closestOpponent;
                return false;
            }
        }
        
        // Move forward
        movePlayer(player, dx, dy, dz);
        return true;
    }

private void resetAfterGoal(StartingScenario scenario) {
    // Reset player positions based on the scenario
    // For simplicity, we'll just use the same initialization logic
    List<Integer> teamSizes = new ArrayList<>();
    for (List<Vertex> team : teams) {
        teamSizes.add(team.size());
    }
    initializeTeams(teamSizes, scenario);
}

    public SimulationResult runMultiTeamSimulation(List<SoccerStrategy> strategies, StartingScenario scenario, int maxIterations) {
        // Initialize teams based on the number of strategies
        List<Integer> teamSizes = new ArrayList<>();
        for (int i = 0; i < strategies.size(); i++) {
            teamSizes.add(11); // Standard team size
        }
        
        // Initialize the simulation
        initializeTeams(teamSizes, scenario);
        
        // Run the simulation for the specified number of iterations
        int iterations = 0;
        SoccerStrategy winningStrategy = null;
        Map<SoccerStrategy, Integer> strategyScores = new HashMap<>();
        Map<Character, Integer> teamScores = new HashMap<>();
        
        // Initialize scores
        for (int i = 0; i < strategies.size(); i++) {
            strategyScores.put(strategies.get(i), 0);
            teamScores.put((char)('A' + i), 0);
        }
        // Reset stats
        successfulPasses = failedPasses = 0;
        totalMovingDistance = 0;
        
        while (true) {
            // Apply each strategy in turn
            boolean goalScored = simulateIteration(strategies);
            
            // If a goal was scored, update scores and check for winner
            if (goalScored) {
                // Update the score for the scoring team and strategy
                char scoringTeam = teams.get(currentTeamWithBall).get(0).team;
                int teamIndex = scoringTeam - 'A';
                SoccerStrategy scoringStrategy = strategies.get(teamIndex);
                
                strategyScores.put(scoringStrategy, strategyScores.get(scoringStrategy) + 1);
                teamScores.put(scoringTeam, teamScores.get(scoringTeam) + 1);
                
                // Check if a strategy has won
                if (strategyScores.get(scoringStrategy) >= 3) { // First to 3 goals wins
                    winningStrategy = scoringStrategy;
                    break;
                }
                resetAfterGoal(scenario);
            }// Reset for next kickoff
            iterations++;
        }
        
        // If no winner after max iterations, select the strategy with the most goals
        if (winningStrategy == null) {
            int maxGoals = -1;
            for (Map.Entry<SoccerStrategy, Integer> entry : strategyScores.entrySet()) {
                if (entry.getValue() > maxGoals) {
                    maxGoals = entry.getValue();
                    winningStrategy = entry.getKey();
                }
            }
        }return new SimulationResult(winningStrategy, iterations, strategyScores, teamScores,
                successfulPasses, failedPasses, totalMovingDistance);
    }


    public boolean simulateIteration(List<SoccerStrategy> strategies) {
        // Get the current team with the ball
        List<Vertex> currentTeam = teams.get(currentTeamWithBall);
        char teamChar = currentTeam.get(0).team;
        int teamIndex = teamChar - 'A';
        // Apply the appropriate strategy
        if (teamIndex < strategies.size()) {
            SoccerStrategy strategy = strategies.get(teamIndex);
            // Since we don't know the exact method signature, let's try a generic approach
            // that works with the existing SoccerStrategy interface
            return strategy.apply(this);
        }
        return false; // No goal scored if no strategy applied
    }


    public boolean pass(Vertex passer, Vertex receiver) {
        if (canPass(passer, receiver)) {
            double successProbability = 1.0;

            for (Vertex opponent : getOpponents(passer)) {
                if (distanceToLine(opponent, passer, receiver) < adjacentRadius) {
                    successProbability *= 0.8; // Reduce success probability for each opponent
                }
            }
            if (Math.random() < successProbability) {
                ballController = receiver;
                successfulPasses++;
                return true;
            } else {
                // Pass intercepted by closest opponent
                Vertex closestOpponent = null;
                double minDistance = Double.MAX_VALUE;
                List<Vertex> opponents = getOpponents(passer);
                for (Vertex opponent : opponents) {
                    double dist = distanceBetween(receiver, opponent);
                    if (dist < minDistance) {
                        minDistance = dist;
                        closestOpponent = opponent;
                    }
                }
                if (closestOpponent != null) {
                    ballController = closestOpponent;
                    currentTeamWithBall = (currentTeamWithBall + 1) % teams.size(); // Switch team
                }
                failedPasses++;
                return false;
            }
        }return false; // Cannot pass
    }

    private double distanceToLine(Vertex point, Vertex lineStart, Vertex lineEnd) {
        double x0 = point.x;
        double y0 = point.y;
        double z0 = point.z;
        double x1 = lineStart.x;
        double y1 = lineStart.y;
        double z1 = lineStart.z;
        double x2 = lineEnd.x;
        double y2 = lineEnd.y;
        double z2 = lineEnd.z;
        
        // Calculate line vector
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        
        double segmentLengthSquared = dx*dx + dy*dy + dz*dz;
        
        if (segmentLengthSquared == 0) {
            // Line segment is a point
            return Math.sqrt((x0-x1)*(x0-x1) + (y0-y1)*(y0-y1) + (z0-z1)*(z0-z1));
        }
        // Calculate projection of point onto line
        double t = ((x0-x1)*dx + (y0-y1)*dy + (z0-z1)*dz) / segmentLengthSquared;
        
        if (t < 0) {
            // Closest point is start of segment
            return Math.sqrt((x0-x1)*(x0-x1) + (y0-y1)*(y0-y1) + (z0-z1)*(z0-z1));
        } else if (t > 1) {
            // Closest point is end of segment
            return Math.sqrt((x0-x2)*(x0-x2) + (y0-y2)*(y0-y2) + (z0-z2)*(z0-z2));
        } else {
            // Closest point is on segment
            double projX = x1 + t * dx;
            double projY = y1 + t * dy;
            double projZ = z1 + t * dz;
            return Math.sqrt((x0-projX)*(x0-projX) + (y0-projY)*(y0-projY) + (z0-projZ)*(z0-projZ));
        }
    }

    public boolean shoot(Vertex shooter) {
        int goalIndex = teamGoals.getOrDefault(shooter.team, shooter.team - 'A');
        double[] goalCoords = getGoalCoordinates((goalIndex + 1) % numGoals); // Opponent's goal
        double dx = goalCoords[0] - shooter.x;
        double dy = goalCoords[1] - shooter.y;
        double dz = goalCoords[2] - shooter.z;
        double distanceToGoal = Math.sqrt(dx*dx + dy*dy + dz*dz);
        double successProbability = 1 - (distanceToGoal / (2 * boundX));
        List<Vertex> opponents = getOpponents(shooter);

        for (Vertex opponent : opponents) {
            double x0 = opponent.x;
            double y0 = opponent.y;
            double z0 = opponent.z;
            double x1 = shooter.x;
            double y1 = shooter.y;
            double z1 = shooter.z;
            double x2 = goalCoords[0];
            double y2 = goalCoords[1];
            double z2 = goalCoords[2];
            
            double lineVectorX = x2 - x1;
            double lineVectorY = y2 - y1;
            double lineVectorZ = z2 - z1;
            
            double segmentLengthSquared = lineVectorX*lineVectorX + lineVectorY*lineVectorY + lineVectorZ*lineVectorZ;
            
            double distanceToLane;
            if (segmentLengthSquared == 0) {
                distanceToLane = Math.sqrt((x0-x1)*(x0-x1) + (y0-y1)*(y0-y1) + (z0-z1)*(z0-z1));
            } else {
                // Calculate projection of point onto line
                double t = ((x0-x1)*lineVectorX + (y0-y1)*lineVectorY + (z0-z1)*lineVectorZ) / segmentLengthSquared;
                
                if (t < 0) {
                    // Closest point is start of segment
                    distanceToLane = Math.sqrt((x0-x1)*(x0-x1) + (y0-y1)*(y0-y1) + (z0-z1)*(z0-z1));
                } else if (t > 1) {
                    // Closest point is end of segment
                    distanceToLane = Math.sqrt((x0-x2)*(x0-x2) + (y0-y2)*(y0-y2) + (z0-z2)*(z0-z2));
                } else {
                    // Closest point is on segment
                    double projX = x1 + t * lineVectorX;
                    double projY = y1 + t * lineVectorY;
                    double projZ = z1 + t * lineVectorZ;
                    distanceToLane = Math.sqrt((x0-projX)*(x0-projX) + (y0-projY)*(y0-projY) + (z0-projZ)*(z0-projZ));
                }
            }
            if (distanceToLane < adjacentRadius) {
                successProbability *= 0.7; // Reduce success probability for each opponent
            }
        }
        if (Math.random() < successProbability) {
            return true;
        } else {// Shot missed, giving to the player closest to the goal
            Double minDistance = Double.MAX_VALUE;
            double distance;
            Vertex closestOpponent = null;
            
            for(Vertex opponent: players){
                distance = opponent.distanceToGoal(dx, dy, dz, goalIndex);
                if (opponent.distanceToGoal(dx, dy, dz, goalIndex) < minDistance) {
                    minDistance = distance;
                    closestOpponent = opponent;
                }
            }
            closestOpponent.hasBall = true;
            return false;
        }
    }

    public double distanceToOpponentGoal(Vertex player) {
        int goalIndex = teamGoals.getOrDefault(player.team, player.team - 'A');
        double[] goalCoords = getGoalCoordinates((goalIndex + 1) % numGoals); // Opponent's goal
        
        double dx = goalCoords[0] - player.x;
        double dy = goalCoords[1] - player.y;
        double dz = goalCoords[2] - player.z;
        return Math.sqrt(dx*dx + dy*dy + dz*dz);
    }

    public int countOpponentsInPenaltyArea(char team) {
        int goalIndex = teamGoals.getOrDefault(team, team - 'A');
        double[] goalCoords = getGoalCoordinates((goalIndex + 1) % numGoals); // Opponent's goal
        
        List<Vertex> opponents = new ArrayList<>();
        for (List<Vertex> t : teams) {
            if (!t.isEmpty() && t.get(0).team != team) {
                opponents.addAll(t);
            }
        }
        int count = 0;
        for (Vertex opponent : opponents) {
            double dx = goalCoords[0] - opponent.x;
            double dy = goalCoords[1] - opponent.y;
            double dz = goalCoords[2] - opponent.z;
            double distance = Math.sqrt(dx*dx + dy*dy + dz*dz);   
            if (distance < penaltyAreaDistance) {//within penalty area
                count++;
            }
        }return count;
    }

    public List<Vertex> getAdjacentOpponents(Vertex player) {
        List<Vertex> opponents = getOpponents(player);
        List<Vertex> adjacentOpponents = new ArrayList<>();
        
        for (Vertex opponent : opponents) {
            if (distanceBetween(player, opponent) <= adjacentRadius) {
                adjacentOpponents.add(opponent);
            }
        }return adjacentOpponents;
    }
    public double calculateSocialRadius(Vertex player) {
        double dx = ballController.x - player.x;
        double dy = ballController.y - player.y;
        double dz = ballController.z - player.z;
        return Math.sqrt(dx*dx + dy*dy + dz*dz);
    }
    public int countOpponentsInSocialRadius(Vertex player, double socialRadius) {
        int count = 0;
        for (Vertex opponent : getOpponents(player)) {
            double dx = opponent.x - player.x;
            double dy = opponent.y - player.y;
            double dz = opponent.z - player.z;
            if (Math.sqrt(dx*dx + dy*dy + dz*dz) < socialRadius) {
                count++;
            }
        }return count;
    }
}