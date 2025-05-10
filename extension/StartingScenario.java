import java.util.*;

public enum StartingScenario {
    CENTER_KICKOFF {// center field
        @Override
        public void initialize(SoccerSimulation simulation, List<List<Vertex>> teams) {
            // Place teams symmetrically around center
            double centerX = 0;
            double centerY = 0;
            double spacing = 100 / 4;
            
            // First team (team A)
            List<Vertex> teamA = teams.get(0);
            for (int i = 0; i < teamA.size(); i++) {
                double angle = (2 * Math.PI * i) / teamA.size();
                teamA.get(i).x = centerX + spacing * Math.cos(angle);
                teamA.get(i).y = centerY + spacing * Math.sin(angle);
                teamA.get(i).z = 0;
            }
            
            // Second team (team B)
            List<Vertex> teamB = teams.get(1);
            for (int i = 0; i < teamB.size(); i++) {
                double angle = (2 * Math.PI * i) / teamB.size();
                teamB.get(i).x = centerX - spacing * Math.cos(angle);
                teamB.get(i).y = centerY - spacing * Math.sin(angle);
                teamB.get(i).z = 0;
            }
            
            // Place ball in center
            simulation.ballController.x = centerX;
            simulation.ballController.y = centerY;
            simulation.ballController.z = 0;
        }
    },
    GOAL_KICK {
        @Override
        public void initialize(SoccerSimulation simulation, List<List<Vertex>> teams) {
            // Place team A near their goal
            List<Vertex> teamA = teams.get(0);
            double goalY = -simulation.bound;
            double spacing = simulation.bound / 4;
            
            for (int i = 0; i < teamA.size(); i++) {
                double angle = (2 * Math.PI * i) / teamA.size();
                teamA.get(i).x = spacing * Math.cos(angle);
                teamA.get(i).y = goalY + spacing * Math.sin(angle);
                teamA.get(i).z = 0;
            }
            
            // Place team B in defensive positions
            List<Vertex> teamB = teams.get(1);
            double defenseSpacing = simulation.bound / 3;
            for (int i = 0; i < teamB.size(); i++) {
                double angle = (2 * Math.PI * i) / teamB.size();
                teamB.get(i).x = spacing * Math.cos(angle);
                teamB.get(i).y = goalY + defenseSpacing + spacing * Math.sin(angle);
                teamB.get(i).z = 0;
            }
            
            // Place ball near goal
            simulation.ballController.x = 0;
            simulation.ballController.y = goalY + spacing;
            simulation.ballController.z = 0;
        }
    },
    CORNER_KICK {
        @Override
        public void initialize(SoccerSimulation simulation, List<List<Vertex>> teams) {
            // Place team A near the corner
            List<Vertex> teamA = teams.get(0);
            double cornerX = -100;
            double cornerY = -simulation.bound;
            double spacing = simulation.bound / 4;
            
            for (int i = 0; i < teamA.size(); i++) {
                double angle = (2 * Math.PI * i) / teamA.size();
                teamA.get(i).x = cornerX + spacing * Math.cos(angle);
                teamA.get(i).y = cornerY + spacing * Math.sin(angle);
                teamA.get(i).z = 0;
            }
            
            // Place team B in defensive positions
            List<Vertex> teamB = teams.get(1);
            double defenseSpacing = simulation.bound / 3;
            for (int i = 0; i < teamB.size(); i++) {
                double angle = (2 * Math.PI * i) / teamB.size();
                teamB.get(i).x = cornerX + spacing * Math.cos(angle);
                teamB.get(i).y = cornerY + defenseSpacing + spacing * Math.sin(angle);
                teamB.get(i).z = 0;
            }
            
            // Place ball near corner
            simulation.ballController.x = cornerX + spacing;
            simulation.ballController.y = cornerY + spacing;
            simulation.ballController.z = 0;
        }
    };
    public abstract void initialize(SoccerSimulation simulation, List<List<Vertex>> teams);
}
