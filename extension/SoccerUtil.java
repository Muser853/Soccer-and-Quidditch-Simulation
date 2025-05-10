import java.util.*;

public class SoccerUtil {
    public static double calculateSocialRadius(Vertex passer, Vertex receiver) {
        double dx = passer.x - receiver.x;
        double dy = passer.y - receiver.y;
        // As per requirements: ((passer.x-receiver.x)^2 + (passer.y - receiver.y)^2)^(1/4)
        return Math.pow(dx * dx + dy * dy, 0.25);
    }
    
    public static int countOpponentsInSocialRadius(Vertex player, List<Vertex> opponents, double socialRadius) {
        int count = 0;
        for (Vertex opponent : opponents) {
            double dx = player.x - opponent.x;
            double dy = player.y - opponent.y;
            double distance = Math.sqrt(dx * dx + dy * dy);
            
            if (distance <= socialRadius) {
                count++;
            }
        }return count;
    }
    
    public static double calculatePassSuccessProbability(int socialCount) {
        if (socialCount == 0) {
            return 1.0; // Guaranteed
        } else if (socialCount == 1) {
            return 0.5; // 50% chance
        } else {
            return 0.0; // Impossible
        }
    }
    
    public static double calculateShootSuccessProbability(double distanceToGoal, int opponentsInPenaltyArea) {
        if (opponentsInPenaltyArea == 0) {
            opponentsInPenaltyArea = 1; // Avoid division by zero
        }
        double probability = 1.0 / (distanceToGoal * opponentsInPenaltyArea);
        return Math.min(1.0, Math.max(0.0, probability));
    }
    
    public static boolean isOffside(Vertex player, List<Vertex> teammates, List<Vertex> opponents, 
                                    Vertex ballController, int boundY) {
        // Player must be on the same team as the ball controller
        if (player.team != ballController.team) {
            return false;
        }
        
        // Player can't be offside in their own half
        char team = player.team;
        int goalY = (team == 'A') ? boundY : -boundY;
        if ((team == 'A' && player.y <= 0) || (team == 'B' && player.y >= 0)) {
            return false;
        }
        
        // Player must be ahead of the ball
        if ((team == 'A' && player.y <= ballController.y) || 
            (team == 'B' && player.y >= ballController.y)) {
            return false;
        }
        
        // Find the second-to-last defender
        List<Vertex> sortedOpponents = new ArrayList<>(opponents);
        sortedOpponents.sort((o1, o2) -> {
            // Sort by distance to their own goal
            double d1 = Math.abs(goalY - o1.y);
            double d2 = Math.abs(goalY - o2.y);
            return Double.compare(d1, d2);
        });
        
        // Need at least 2 defenders for offside
        if (sortedOpponents.size() < 2) {
            return false;
        }
        
        // Get the second-to-last defender
        Vertex secondLastDefender = sortedOpponents.get(1);
        
        // Player is offside if they are closer to the goal line than both the ball and the second-to-last defender
        return (team == 'A' && player.y > secondLastDefender.y && player.y > ballController.y) || 
               (team == 'B' && player.y < secondLastDefender.y && player.y < ballController.y);
    }
    
    public static double distanceBetween(Vertex player1, Vertex player2) {
        double dx = player1.x - player2.x;
        double dy = player1.y - player2.y;
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    public static double distanceToGoal(Vertex player, int goalX, int goalY) {
        double dx = player.x - goalX;
        double dy = player.y - goalY;
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    public static boolean canPass(Vertex passer, Vertex receiver, List<Vertex> opponents, double adjacentRadius) {
        double dx = receiver.x - passer.x;
        double dy = receiver.y - passer.y;
        double passDistance = Math.sqrt(dx * dx + dy * dy);
        double dirX = dx / passDistance;
        double dirY = dy / passDistance;
        
        // Check each opponent for blocking
        for (Vertex opponent : opponents) {
            // Calculate distance between opponent and passer
            double oppDx = opponent.x - passer.x;
            double oppDy = opponent.y - passer.y;
            double oppDistance = Math.sqrt(oppDx * oppDx + oppDy * oppDy);
            
            // Check if opponent is within adjacent radius
            if (oppDistance <= adjacentRadius) {
                // Calculate direction to opponent
                double oppDirX = oppDx / oppDistance;
                double oppDirY = oppDy / oppDistance;
                
                // Calculate dot product to determine if directions are similar
                double dotProduct = dirX * oppDirX + dirY * oppDirY;
                
                // If angle is less than pi/4 (cos(pi/4) ≈ 0.7071), the pass is blocked
                if (dotProduct > 0.7071) {
                    // Check if opponent is closer to their goal than the ball controller
                    // If so, the ball controller has to choose to pass to available edges or break through
                    if ((passer.team == 'A' && opponent.y < passer.y) || 
                        (passer.team == 'B' && opponent.y > passer.y)) {
                        return false;
                    }
                }
            }
        }
        
        // Check social radius and count for pass success probability
        double socialRadius = calculateSocialRadius(passer, receiver);
        int socialCount = countOpponentsInSocialRadius(receiver, opponents, socialRadius);
        return calculatePassSuccessProbability(socialCount) > 0;
    }

}