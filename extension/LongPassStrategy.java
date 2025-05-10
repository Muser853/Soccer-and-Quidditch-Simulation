import java.util.List;

public class LongPassStrategy extends SoccerStrategy {
    public LongPassStrategy() {
        this.name = "LongPass";
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
            if (teammate.socialCount < 2) {
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