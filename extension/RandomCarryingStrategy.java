import java.util.List;
import java.util.Random;

public class RandomCarryingStrategy extends SoccerStrategy {
    private Random random;
    
    public RandomCarryingStrategy() {
        this.name = "RandomCarrying";
        this.random = new Random();
    }

    @Override
    public void execute(SoccerSimulation simulation, Vertex ballController) {
        apply(simulation);
    }

    @Override
    protected String determineAction(SoccerSimulation simulation, Vertex ballController, 
                                   List<Vertex> teammates, List<Vertex> opponents) {
        // Randomly decide whether to break through or pass
        return random.nextBoolean() ? "breakthrough" : "pass";
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