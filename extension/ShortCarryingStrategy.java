import java.util.List;

public class ShortCarryingStrategy extends SoccerStrategy {
    public ShortCarryingStrategy() {
        this.name = "ShortCarrying";
    }

    @Override
    public void execute(SoccerSimulation simulation, Vertex ballController) {
        apply(simulation);
    }

    @Override
    protected String determineAction(SoccerSimulation simulation, Vertex ballController, 
                                   List<Vertex> teammates, List<Vertex> opponents) {
        // Pass if there's any adjacent opponent
        return ballController.adjacentVertices().stream()
            .anyMatch(opponent -> opponent.team != ballController.team) ? "pass" : "move";
    }

    @Override
    protected Vertex findBestPassTarget(SoccerSimulation simulation, Vertex ballController, 
                                     List<Vertex> teammates, List<Vertex> opponents) {
        // Find the closest teammate to the opponent goal
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