import java.util.Comparator;
import java.util.List;
/**
 * Active Pressing Strategy: Aggressive defense in the opponent's half.
 * Players will press high up the field and try to win the ball back quickly.
 */
public class ActivePressingStrategy extends SoccerStrategy {
    
    public ActivePressingStrategy() {
        this.name = "Active Pressing";
    }
    
    @Override
    public void execute(SoccerSimulation simulation, Vertex ballController) {
        List<Vertex> teammates = simulation.getTeammates(ballController);
        List<Vertex> opponents = simulation.getOpponents(ballController);
        
        // If we have the ball
        if (ballController.team == (teammates.get(0).team)) {
            // Move the two players closest to opponent goal forward
            List<Vertex> forwardPlayers = teammates.stream()
                .filter(p -> p != ballController)
                .sorted(Comparator.comparingDouble(p -> 
                    simulation.distanceToOpponentGoal(p)))
                .limit(2)
                .toList();
            
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
            // Press aggressively - move players towards the ball controller
            // Focus on high pressing in opponent's half

                // If the ball is in opponent's half, press more aggressively
            if (Math.signum(ballController.y) != Math.signum(ballController.goalIndex)) {
                for (Vertex player : teammates) {
                    // Move directly towards ball controller for aggressive pressing
                    simulation.movePlayerTowards(player, ballController);
                }
            }else {
                for(Vertex player : teammates){
                    // In our own half, have the closest player press aggressively
                    if (player == teammates.stream()
                            .min(Comparator.comparingDouble(p -> 
                                Math.sqrt(Math.pow(p.x - ballController.x, 2) + 
                                          Math.pow(p.y - ballController.y, 2))))
                            .orElse(null)) {
                        simulation.movePlayerTowards(player, ballController);
                    } else {
                        // Other players maintain defensive positions
                        simulation.movePlayerAwayFromOpponents(player, opponents);
                    }
                }
            }
        }
    }
    @Override
    protected String determineAction(SoccerSimulation simulation, Vertex ballController, 
                                   List<Vertex> teammates, List<Vertex> opponents) {
        // Check if there are adjacent opponents
        List<Vertex> adjacentOpponents = simulation.getAdjacentOpponents(ballController);
        
        // If we're close to goal, try to shoot
        if (simulation.distanceToOpponentGoal(ballController) < SoccerSimulation.penaltyAreaDistance) {
            return "shoot";
        }
        if (!adjacentOpponents.isEmpty()) {
            // 50% chance to try breakthrough, otherwise pass
            return Math.random() < 0.5 ? "breakthrough" : "pass";
        }
        // If we have a good passing option and we're in our own half
        if (simulation.distanceToOwnGoal(ballController) < simulation.bound) {
            Vertex target = findBestPassTarget(simulation, ballController, teammates, opponents);
            if (target != null) {
                return "pass";
            }
        }return "move";
    }

    @Override
    protected Vertex findBestPassTarget(SoccerSimulation simulation, Vertex ballController, 
                                      List<Vertex> teammates, List<Vertex> opponents) {
        // In pressing strategy, we prefer forward passes to players in advanced positions
        return teammates.stream()
            .filter(p -> p != ballController)
            .filter(p -> simulation.distanceToOpponentGoal(p) < simulation.distanceToOpponentGoal(ballController))
            .filter(p -> simulation.canPass(ballController, p))
            .min(Comparator.comparingDouble(p -> 
                simulation.distanceToOpponentGoal(p) + 
                simulation.countOpponentsInSocialRadius(p, simulation.calculateSocialRadius(p)) * 10))
            .orElse(null);
    }
}