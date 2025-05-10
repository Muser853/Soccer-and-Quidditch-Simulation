import java.util.List;

public abstract class SoccerStrategy {
    protected String name;
    
    public abstract void execute(SoccerSimulation simulation, Vertex ballController);
    
    public boolean apply(SoccerSimulation simulation) {
        Vertex ballController = simulation.ballController;
        List<Vertex> teammates = simulation.getTeammates(ballController);
        List<Vertex> opponents = simulation.getOpponents(ballController);
        
        switch(determineAction(simulation, ballController, teammates, opponents)){
        
        case "pass":
            return simulation.pass(ballController, findBestPassTarget(simulation, ballController, teammates, opponents));
        case "shoot": return simulation.shoot(ballController);
        case "breakthrough": 
            return simulation.breakthrough(ballController);
        case "move":
            simulation.movePlayerTowardsOpponentGoal(ballController);
            return false;
        default: return false;
        }
    }
    
    protected abstract String determineAction(SoccerSimulation simulation, Vertex ballController, 
                                           List<Vertex> teammates, List<Vertex> opponents);
    
    protected abstract Vertex findBestPassTarget(SoccerSimulation simulation, Vertex ballController, 
                                              List<Vertex> teammates, List<Vertex> opponents);
}