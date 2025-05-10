import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Voronoi Inspired Strategy: Maximize the total control area calculated from VoronoiGraph
 * of all members in the team. Players position themselves to maximize territorial control.
 */
public class VoronoiInspiredStrategy extends SoccerStrategy {
    
    public VoronoiInspiredStrategy() {
        this.name = "Voronoi Inspired";
    }
    
    @Override
    public void execute(SoccerSimulation simulation, Vertex ballController) {
        List<Vertex> teammates = simulation.getTeammates(ballController);
        List<Vertex> opponents = simulation.getOpponents(ballController);
        
        // If we have the ball
        if (ballController.team == teammates.get(0).team) {
            // Move the two players closest to opponent goal forward
            List<Vertex> forwardPlayers = teammates.stream()
                .filter(p -> p != ballController)
                .sorted(Comparator.comparingDouble(p -> 
                    simulation.distanceToOpponentGoal(p)))
                .limit(2)
                .collect(Collectors.toList());
            
            for (Vertex player : forwardPlayers) {
                simulation.movePlayerTowardsOpponentGoal(player);
            }
            
            // Other players should position to maximize Voronoi area
            for (Vertex player : teammates) {
                if (!forwardPlayers.contains(player) && player != ballController) {
                    positionForMaximumVoronoiArea(simulation, player, teammates, opponents);
                }
            }
            
            // Determine action for ball controller
            String action = determineAction(simulation, ballController, teammates, opponents);
            
            switch (action) {
                case "pass":
                    Vertex target = findBestPassTarget(simulation, ballController, teammates, opponents);
                    if (target != null) {
                        simulation.pass(ballController, target);
                    }
                    break;
                case "shoot":
                    simulation.shoot(ballController);
                    break;
                case "breakthrough":
                    simulation.breakthrough(ballController);
                    break;
                case "move":
                default:
                    simulation.movePlayerTowardsOpponentGoal(ballController);
                    break;
            }
        } 
        // If opponents have the ball
        else {
            // Position players to cover maximum area and block passing lanes
            for (Vertex player : teammates) {
                if (simulation.distanceBetween(player, ballController) < simulation.adjacentRadius * 1.5) {
                    simulation.movePlayerTowards(player, ballController);
                } else {
                    positionForMaximumVoronoiArea(simulation, player, teammates, opponents);
                }
            }
        }
    }
    private void positionForMaximumVoronoiArea(SoccerSimulation simulation, Vertex player, 
                                              List<Vertex> teammates, List<Vertex> opponents) {
        // Try different potential moves and pick the one that maximizes area
        int bestDx = 0;
        int bestDy = 0;
        double maxArea = calculateVoronoiArea(simulation, player, teammates, opponents);
        
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                
                // Temporarily move player
                double originalX = player.x;
                double originalY = player.y;
                player.x += dx;
                player.y += dy;
                
                // Ensure player stays within field boundaries
                player.x = Math.max(-100, Math.min(100, player.x));
                player.y = Math.max(-simulation.bound, Math.min(simulation.bound, player.y));
                
                // Calculate new area
                double area = calculateVoronoiArea(simulation, player, teammates, opponents);
                
                // If this position is better, remember it
                if (area > maxArea) {
                    maxArea = area;
                    bestDx = dx;
                    bestDy = dy;
                }
                // Restore original position
                player.x = originalX;
                player.y = originalY;
            }
        }
        if (bestDx != 0 || bestDy != 0) {
            simulation.movePlayer(player, bestDx, bestDy);
        }
    }
    
    /**
     * Calculate the Voronoi area controlled by the team
     */
    private double calculateVoronoiArea(SoccerSimulation simulation, Vertex player, 
                                       List<Vertex> teammates, List<Vertex> opponents) {
        // Simple approximation of Voronoi area - count cells closer to team players than opponents
        double totalArea = 0;
        int fieldWidth = 100 * 2;
        double fieldHeight = simulation.bound * 2;
        int sampleSize = 20; // Number of sample points to check
        
        // Create a list of all players
        List<Vertex> teamPlayers = new ArrayList<>(teammates);
        
        // Sample points in the field to estimate Voronoi area
        for (int i = 0; i < sampleSize; i++) {
            for (int j = 0; j < sampleSize; j++) {
                // Calculate sample point coordinates
                double x = -100 + (fieldWidth * i / (double) sampleSize);
                double y = -simulation.bound + (fieldHeight * j / sampleSize);
                
                // Find closest player to this point
                Vertex closestPlayer = null;
                double minDistance = Double.MAX_VALUE;
                
                // Check team players
                for (Vertex teammate : teamPlayers) {
                    double distance = Math.sqrt(
                        (x-teammate.x)*(x-teammate.x) 
                        + (y-teammate.y)*(y-teammate.y)
                    );
                    if (distance < minDistance) {
                        minDistance = distance;
                        closestPlayer = teammate;
                    }
                }
                // Check opponent players
                for (Vertex opponent : opponents) {
                    double distance =Math.sqrt(
                        (x-opponent.x)*(x-opponent.x) 
                        + (y-opponent.y)*(y-opponent.y)
                    );
                    if (distance < minDistance) {
                        minDistance = distance;
                        closestPlayer = opponent;
                    }
                }
                // If closest player is from our team, add to area
                if (closestPlayer != null && closestPlayer.team == player.team) {
                    totalArea += 1.0;
                }
            }
        }// Normalize the area
        return totalArea / (sampleSize * sampleSize);
    }
    
    @Override
    protected String determineAction(SoccerSimulation simulation, Vertex ballController, 
                                   List<Vertex> teammates, List<Vertex> opponents) {
        // Check if there are adjacent opponents
        List<Vertex> adjacentOpponents = simulation.getAdjacentOpponents(ballController);
        
        // If we're close to goal and have a clear shot, try to shoot
        if (simulation.distanceToOpponentGoal(ballController) < SoccerSimulation.penaltyAreaDistance * 0.8) {
            double shootProb = 1.0 / (simulation.distanceToOpponentGoal(ballController) * 
                                    simulation.countOpponentsInPenaltyArea(ballController.team));
            if (shootProb > 0.3) {
                return "shoot";
            }
        }
        if (!adjacentOpponents.isEmpty()) {
            // In Voronoi strategy, we prefer passing over breakthrough
            // but will attempt breakthrough if no good passing options
            Vertex target = findBestPassTarget(simulation, ballController, teammates, opponents);
            if (target != null) {
                return "pass";
            } else {
                return "breakthrough";
            }
        }
        // If we have a good passing option
        Vertex target = findBestPassTarget(simulation, ballController, teammates, opponents);
        if (target != null) {
            return "pass";
        }
        // If close to goal and no good passing options
        if (simulation.distanceToOpponentGoal(ballController) < SoccerSimulation.penaltyAreaDistance * 1.2) {
            return "shoot";
        }
        return "move";// Otherwise, move forward
    }

    @Override
    protected Vertex findBestPassTarget(SoccerSimulation simulation, Vertex ballController, 
                                      List<Vertex> teammates, List<Vertex> opponents) {
        // In Voronoi strategy, we prefer passes that maximize our team's control area
        return teammates.stream()
            .filter(p -> p != ballController)
            .filter(p -> simulation.canPass(ballController, p))
            .min(Comparator.comparingDouble(p -> {
                // Calculate social radius and count
                double socialRadius = SoccerUtil.calculateSocialRadius(ballController, p);
                int socialCount = SoccerUtil.countOpponentsInSocialRadius(p, opponents, socialRadius);
                
                // Calculate pass success probability
                double successProb = SoccerUtil.calculatePassSuccessProbability(socialCount);
                
                // Return a score (lower is better)
                // We consider: distance to goal, social count, and area control
                double distanceToGoal = simulation.distanceToOpponentGoal(p);
                double areaControl = 1.0 - calculateVoronoiArea(simulation, p, teammates, opponents);
                
                // If pass has zero probability of success, return infinity
                if (successProb == 0.0) {
                    return Double.POSITIVE_INFINITY;
                }
                return distanceToGoal + socialCount * 50 + areaControl * 100;
            }))
            .orElse(null);
    }
}