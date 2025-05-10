import java.util.List;

/**
 * Short Pass Strategy: Focuses on making short, safe passes to nearby teammates
 * and almost never attempting to break through the opponent's defense.
 */
public class ShortPassStrategy extends SoccerStrategy {
    
    public ShortPassStrategy() {
        this.name = "ShortPass";
    }

    @Override
    public void execute(SoccerSimulation simulation, Vertex ballController) {
        apply(simulation);
    }

    @Override
    protected String determineAction(SoccerSimulation simulation, Vertex ballController, 
                                   List<Vertex> teammates, List<Vertex> opponents) {
        return "pass";
    }

    @Override
    protected Vertex findBestPassTarget(SoccerSimulation simulation, Vertex ballController, 
                                     List<Vertex> teammates, List<Vertex> opponents) {
        Vertex bestTarget = null;
        double minDistanceToGoal = Double.MAX_VALUE;
        
        for (Vertex teammate : teammates) {
            if (teammate.socialCount == 0) {
                double distanceToGoal = Math.sqrt(Math.pow(teammate.x, 2) + Math.pow(teammate.y, 2));
                if (distanceToGoal < minDistanceToGoal) {
                    minDistanceToGoal = distanceToGoal;
                    bestTarget = teammate;
                }
            }
        }
        
        return bestTarget;
    }
}