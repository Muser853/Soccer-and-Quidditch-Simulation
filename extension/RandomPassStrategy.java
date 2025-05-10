import java.util.List;
import java.util.Random;

public class RandomPassStrategy extends SoccerStrategy {
    private Random random;
    
    public RandomPassStrategy() {
        this.name = "RandomPass";
        this.random = new Random();
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
        // Filter teammates with socialCount = 0
        List<Vertex> validTargets = teammates.stream()
            .filter(teammate -> teammate.socialCount == 0)
            .toList();
            
        if (validTargets.isEmpty()) {
            return null;
        }
        
        // Randomly select from valid targets
        return validTargets.get(random.nextInt(validTargets.size()));
    }
}