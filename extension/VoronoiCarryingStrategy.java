import java.util.List;
import java.util.ArrayList;

public class VoronoiCarryingStrategy extends SoccerStrategy {
    public VoronoiCarryingStrategy() {
        this.name = "VoronoiCarrying";
    }

    @Override
    public void execute(SoccerSimulation simulation, Vertex ballController) {
        apply(simulation);
    }

    @Override
    protected String determineAction(SoccerSimulation simulation, Vertex ballController, 
                                   List<Vertex> teammates, List<Vertex> opponents) {
        // Calculate Voronoi cells for all players
        List<Vertex> allPlayers = new ArrayList<>();
        allPlayers.addAll(teammates);
        allPlayers.addAll(opponents);
        
        double currentControlArea = calculateControlArea(ballController, allPlayers);
        
        // Find the best teammate to pass to that maximizes control area
        Vertex bestTarget = null;
        double maxControlArea = 0;
        
        for (Vertex teammate : teammates) {
            if (teammate != ballController) {
                double newControlArea = calculateControlArea(teammate, allPlayers);
                if (newControlArea > maxControlArea) {
                    maxControlArea = newControlArea;
                    bestTarget = teammate;
                }
            }
        }
        
        // Pass if we can increase control area significantly
        if (bestTarget != null && maxControlArea > currentControlArea * 1.1) {
            return "pass";
        }
        
        // Otherwise, try to break through
        return "breakthrough";
    }

    @Override
    protected Vertex findBestPassTarget(SoccerSimulation simulation, Vertex ballController, 
                                     List<Vertex> teammates, List<Vertex> opponents) {
        // Calculate Voronoi cells for all players
        List<Vertex> allPlayers = new ArrayList<>();
        allPlayers.addAll(teammates);
        allPlayers.addAll(opponents);
        
        Vertex bestTarget = null;
        double maxControlArea = 0;
        
        for (Vertex teammate : teammates) {
            if (teammate != ballController) {
                double newControlArea = calculateControlArea(teammate, allPlayers);
                if (newControlArea > maxControlArea) {
                    maxControlArea = newControlArea;
                    bestTarget = teammate;
                }
            }
        }
        
        return bestTarget;
    }

    private double calculateControlArea(Vertex player, List<Vertex> allPlayers) {
        double controlArea = 0;
        
        // For each other player, calculate the region where player has control
        for (Vertex otherPlayer : allPlayers) {
            if (otherPlayer != player) {
                double distance = player.distanceTo(otherPlayer);
                // Control area is proportional to the square of the distance
                // This is a simplified approximation of the Voronoi cell area
                controlArea += Math.pow(distance, 2);
            }
        }
        
        return controlArea;
    }
}
