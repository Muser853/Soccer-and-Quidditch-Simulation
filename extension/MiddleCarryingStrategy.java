import java.util.List;

public class MiddleCarryingStrategy extends SoccerStrategy {
    public MiddleCarryingStrategy() {
        this.name = "MiddleCarrying";
    }

    @Override
    public void execute(SoccerSimulation simulation, Vertex ballController) {
        apply(simulation);
    }

    @Override
    protected String determineAction(SoccerSimulation simulation, Vertex ballController, 
                                   List<Vertex> teammates, List<Vertex> opponents) {
        // Check if there's an opponent closer to goal than ball controller
        for (Vertex opponent : opponents) {
            double opponentDistanceToGoal = Math.sqrt(Math.pow(opponent.x, 2) + Math.pow(opponent.y, 2));
            double ballControllerDistanceToGoal = Math.sqrt(Math.pow(ballController.x, 2) + Math.pow(ballController.y, 2));
            
            if (opponentDistanceToGoal < ballControllerDistanceToGoal) {
                // Check if this opponent is closer to ball controller than any other opponent
                double closestOpponentDistance = Double.MAX_VALUE;
                for (Vertex otherOpponent : opponents) {
                    if (otherOpponent != opponent) {
                        double distance = ballController.distanceTo(otherOpponent);
                        if (distance < closestOpponentDistance) {
                            closestOpponentDistance = distance;
                        }
                    }
                }
                
                if (ballController.distanceTo(opponent) < closestOpponentDistance) {
                    return "breakthrough";
                }
            }
        }
        
        // If no such opponent exists, pass to the closest teammate to goal
        return "pass";
    }

    @Override
    protected Vertex findBestPassTarget(SoccerSimulation simulation, Vertex ballController, 
                                     List<Vertex> teammates, List<Vertex> opponents) {
        Vertex bestTarget = null;
        double minDistanceToGoal = Double.MAX_VALUE;
        
        for (Vertex teammate : teammates) {
            double distanceToGoal = Math.sqrt(Math.pow(teammate.x, 2) + Math.pow(teammate.y, 2));
            if (distanceToGoal < minDistanceToGoal) {
                minDistanceToGoal = distanceToGoal;
                bestTarget = teammate;
            }
        }
        
        return bestTarget;
    }
}