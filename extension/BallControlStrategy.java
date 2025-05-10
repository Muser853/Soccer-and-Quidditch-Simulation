import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Ball Control Strategy: Maximizing ball possession by passing to nearby teammates,
 * often around the midfield or within their own half.
 */
public class BallControlStrategy extends SoccerStrategy {
    
    public BallControlStrategy() {
        this.name = "Ball Control";
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
            
            // Other players should maintain formation but avoid opponents
            for (Vertex player : teammates) {
                if (!forwardPlayers.contains(player) && player != ballController) {
                    simulation.movePlayerAwayFromOpponents(player, opponents);
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
            // More conservative approach - maintain formation and block passing lanes
            for (Vertex player : teammates) {
                // Move towards ball but maintain defensive shape
                if (simulation.distanceBetween(player, ballController) < simulation.adjacentRadius * 2) {
                    simulation.movePlayerTowards(player, ballController);
                } else {
                    // Stay in position but slightly shift towards the ball
                    simulation.movePlayerSlightlyTowards(player, ballController);
                }
            }
        }
    }

    @Override
    protected String determineAction(SoccerSimulation simulation, Vertex ballController, 
                                   List<Vertex> teammates, List<Vertex> opponents) {
        // Check if there are adjacent opponents
        List<Vertex> adjacentOpponents = simulation.getAdjacentOpponents(ballController);
        
        // If we're close to goal and have a clear shot, try to shoot
        if (simulation.distanceToOpponentGoal(ballController) < SoccerSimulation.penaltyAreaDistance * 0.7 &&
            simulation.countOpponentsInPenaltyArea(ballController.team) < 3) {
            return "shoot";
        }
        
        // If there are adjacent opponents
        if (!adjacentOpponents.isEmpty()) {
            // In ball control strategy, we prefer passing over breakthrough
            return "pass";
        }
        
        // If we have a good passing option
        Vertex target = findBestPassTarget(simulation, ballController, teammates, opponents);
        if (target != null) {
            return "pass";
        }
        
        // If close to goal and no good passing options
        if (simulation.distanceToOpponentGoal(ballController) < SoccerSimulation.penaltyAreaDistance * 1.5) {
            return "shoot";
        }
        
        // Otherwise, move forward
        return "move";
    }

    @Override
    protected Vertex findBestPassTarget(SoccerSimulation simulation, Vertex ballController, 
                                      List<Vertex> teammates, List<Vertex> opponents) {
        // For BallControlStrategy, we prioritize passes that are guaranteed to succeed (socialCount == 0)
        // First, look for targets with socialCount == 0
        List<Vertex> guaranteedTargets = teammates.stream()
            .filter(p -> p != ballController)
            .filter(p -> simulation.canPass(ballController, p))
            .filter(p -> {
                double socialRadius = simulation.calculateSocialRadius(p);
                int socialCount = simulation.countOpponentsInSocialRadius(p, socialRadius);
                return socialCount == 0; // Only consider targets with guaranteed success
            })
            .collect(Collectors.toList());
        
        // If we have guaranteed targets, choose the best one based on position
        if (!guaranteedTargets.isEmpty()) {
            return guaranteedTargets.stream()
                .min(Comparator.comparingDouble(p -> {
                    // Prefer targets that are closer to opponent's goal but not too far from ball controller
                    double distanceToGoal = simulation.distanceToOpponentGoal(p);
                    double distanceFromBallController = Math.sqrt(
                        Math.pow(p.x - ballController.x, 2) + 
                        Math.pow(p.y - ballController.y, 2));
                    return distanceToGoal * 0.7 + distanceFromBallController * 0.3;
                }))
                .orElse(null);
        }
        // If no guaranteed targets, fall back to regular target selection
        return teammates.stream()
            .filter(p -> p != ballController)
            .filter(p -> simulation.canPass(ballController, p))
            .min(Comparator.comparingDouble(p -> {
                double socialRadius = simulation.calculateSocialRadius(p);
                // Prioritize players with fewer opponents around them
                return simulation.countOpponentsInSocialRadius(p, socialRadius) * 100 
                + simulation.distanceBetween(ballController, p);
            }))
            .orElse(null);
    }
}