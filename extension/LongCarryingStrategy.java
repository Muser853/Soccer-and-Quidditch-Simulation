import java.util.List;

public class LongCarryingStrategy extends SoccerStrategy {
    public LongCarryingStrategy() {
        this.name = "LongCarrying";
    }

    @Override
    public void execute(SoccerSimulation simulation, Vertex ballController) {
        apply(simulation);
    }

    @Override
    protected String determineAction(SoccerSimulation simulation, Vertex ballController, 
                                   List<Vertex> teammates, List<Vertex> opponents) {
        // Count adjacent opponents
        long adjacentOpponents = ballController.adjacentVertices().stream()
            .filter(opponent -> opponent.team != ballController.team)
            .count();
            
        // Break through if there are 2 or fewer adjacent opponents
        return adjacentOpponents <= 2 ? "breakthrough" : "pass";
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