import java.util.ArrayList;
import java.util.List;

class Edge {
    private Vertex u, v;
    public double distance;

    public Edge(Vertex u, Vertex v, double distance){
        this.u = u;
        this.v = v;
        this.distance = distance;
    }

    public Vertex other(Vertex vertex){
        if(vertex == u) return v;
        else if(vertex == v) return u;
        else return null;
    }

    public Vertex[] vertices(){
        return new Vertex[]{u, v};
    }
}
public class Vertex {
    private final List<Edge> edges;
    public final char team;
    public double x, y, z;
    public int playerIndex;
    public int goalIndex; // The goal this player is defending
    public int socialCount; // Number of adjacent teammates
    
    public boolean hasBall;

    public Vertex(double x, double y, double z, char team) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.team = team;
        this.hasBall = false;
        this.edges = new ArrayList<>();
        this.playerIndex = -1; // Default value, will be set later
        this.goalIndex = -1; // Default value, will be set later
    }
    
    public Vertex(double x, double y, char team) {
        this(x, y, 0, team);
    }
    public void move(double dx, double dy, double dz, double boundX, double boundY, double boundZ) {
        x = Math.max(-boundX, Math.min(boundX, x + dx));
        y = Math.max(-boundY, Math.min(boundY, y + dy));
        z = Math.max(-boundZ, Math.min(boundZ, z + dz));
    }
    
    public void move(double dx, double dy, double boundX, double boundY) {
        move(dx, dy, 0, boundX, boundY, 0);
    }
    public void moveTowardsGoal(double boundX, double boundY, double boundZ, int goalIndex) {
        double[] goalCoordinates = getGoalCoordinates(boundX, boundY, boundZ, goalIndex);
        double goalX = goalCoordinates[0];
        double goalY = goalCoordinates[1];
        double goalZ = goalCoordinates[2];
        
        // Calculate direction vector
        double dx = goalX - this.x;
        double dy = goalY - this.y;
        double dz = goalZ - this.z;
        
        // Normalize the direction vector
        double magnitude = Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (magnitude > 0) {
            dx = dx / magnitude;
            dy = dy / magnitude;
            dz = dz / magnitude;
        }
        
        // Move one unit in the direction of the goal
        double newX = Math.max(-boundX, Math.min(boundX, this.x + dx));
        double newY = Math.max(-boundY, Math.min(boundY, this.y + dy));
        double newZ = Math.max(-boundZ, Math.min(boundZ, this.z + dz));
        
        this.x = newX;
        this.y = newY;
        this.z = newZ;
    }
    
    private double[] getGoalCoordinates(double boundX, double boundY, double boundZ, int goalIndex) {
        double[] coordinates = new double[3];
        
        switch (goalIndex) {
            case 0: // x = 0, y = -bound, z = 0
                coordinates[0] = 0;
                coordinates[1] = -boundY;
                coordinates[2] = 0;
                break;
            case 1: // x = 0, y = bound, z = 0
                coordinates[0] = 0;
                coordinates[1] = boundY;
                coordinates[2] = 0;
                break;
            case 2: // x = 100, y = 0, z = 0
                coordinates[0] = 100;
                coordinates[1] = 0;
                coordinates[2] = 0;
                break;
            case 3: // x = -100, y = 0, z = 0
                coordinates[0] = -100;
                coordinates[1] = 0;
                coordinates[2] = 0;
                break;
            case 4: // x = 0, y = 0, z = bound
                coordinates[0] = 0;
                coordinates[1] = 0;
                coordinates[2] = boundZ;
                break;
            case 5: // x = 0, y = 0, z = -bound
                coordinates[0] = 0;
                coordinates[1] = 0;
                coordinates[2] = -boundZ;
                break;
            default:
                coordinates[0] = 0;
                coordinates[1] = 0;
                coordinates[2] = 0;
        }
        return coordinates;
    }
    
    public void moveAwayFromOpponents(List<Vertex> opponents, double boundX, double boundY, double boundZ, boolean isControllingTeam, int goalIndex) {
        double dirX = 0, dirY = 0, dirZ = 0;
        
        // Calculate the direction vector based on the formula
        for (Vertex opp : opponents) {
            // Skip if coordinates are identical to avoid division by zero
            if (this.x != opp.x) {
                dirX += 1.0 / (this.x - opp.x);
            }
            if (this.y != opp.y) {
                dirY += 1.0 / (this.y - opp.y);
            }
            if (this.z != opp.z) {
                dirZ += 1.0 / (this.z - opp.z);
            }
        }
        
        // For team not controlling the ball, move in the negative direction
        // For team controlling the ball, move in the positive direction
        if (!isControllingTeam) {
            dirX = -dirX;
            dirY = -dirY;
            dirZ = -dirZ;
        }
        
        // Don't move if the direction vector is zero
        if (dirX == 0 && dirY == 0 && dirZ == 0) {
            return;
        }
        
        // Normalize the direction vector
        double magnitude = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        if (magnitude > 0) {
            dirX /= magnitude;
            dirY /= magnitude;
            dirZ /= magnitude;
            
            // Move player in calculated direction
            double newX = this.x + dirX;
            double newY = this.y + dirY;
            double newZ = this.z + dirZ;
            
            // Ensure player stays within bounds
            // Player coordinate range: -100 < x < 100, -bound < y < bound, -bound < z < bound
            newX = Math.max(-99.9, Math.min(99.9, newX));
            newY = Math.max(-boundY + 0.1, Math.min(boundY - 0.1, newY));
            newZ = Math.max(-boundZ + 0.1, Math.min(boundZ - 0.1, newZ));
            
            // For team controlling the ball, never move away from opponent's goal
            if (isControllingTeam) {
                double[] goalCoordinates = getGoalCoordinates(boundX, boundY, boundZ, goalIndex);
                double goalX = goalCoordinates[0];
                double goalY = goalCoordinates[1];
                double goalZ = goalCoordinates[2];
                
                // Calculate current and new distances to goal
                double currentDist = Math.sqrt(Math.pow(this.x - goalX, 2) + Math.pow(this.y - goalY, 2) + Math.pow(this.z - goalZ, 2));
                double newDist = Math.sqrt(Math.pow(newX - goalX, 2) + Math.pow(newY - goalY, 2) + Math.pow(newZ - goalZ, 2));
                
                // Check if the move would take the player away from the opponent's goal
                boolean movingAwayFromGoal = newDist > currentDist;
                
                if (movingAwayFromGoal) {
                    // Only update coordinates that don't increase distance to goal
                    if (Math.abs(newX - goalX) <= Math.abs(this.x - goalX)) this.x = newX;
                    if (Math.abs(newY - goalY) <= Math.abs(this.y - goalY)) this.y = newY;
                    if (Math.abs(newZ - goalZ) <= Math.abs(this.z - goalZ)) this.z = newZ;
                } else {
                    // Update all coordinates
                    this.x = newX;
                    this.y = newY;
                    this.z = newZ;
                }
            } else {
                // For defending team, update all coordinates
                this.x = newX;
                this.y = newY;
                this.z = newZ;
            }
        }
    }
    
    public double distanceTo(Vertex other) {
        return Math.sqrt(Math.pow(this.x - other.x, 2) + 
                        Math.pow(this.y - other.y, 2) + 
                        Math.pow(this.z - other.z, 2));
    }
    public double distanceToGoal(double boundX, double boundY, double boundZ, int goalIndex) {
        double[] goalCoords = getGoalCoordinates(boundX, boundY, boundZ, goalIndex);
        return Math.sqrt(Math.pow(this.x - goalCoords[0], 2) + 
                        Math.pow(this.y - goalCoords[1], 2) + 
                        Math.pow(this.z - goalCoords[2], 2));
    }
    
    public Edge getEdgeTo(Vertex vertex){
        for (Edge edge : edges) 
            if (edge.other(this) == vertex) return edge;
        return null;
    }
    
    public void addEdge(Edge edge) { edges.add(edge); }
    
    public void removeEdge(Edge edge) { edges.remove(edge); }
    

    public List<Vertex> adjacentVertices() {
        List<Vertex> adjacent = new ArrayList<Vertex>();
        for (Edge edge : edges) {
            Vertex other = edge.other(this);
            if (other != null && !adjacent.contains(other)) {
                adjacent.add(other);
            }
        }
        return adjacent;
    }
    
    public List<Edge> incidentEdges() { return new ArrayList<>(edges); }
    
    @Override
    public String toString() {
        return "Vertex{" +
                "team=" + team +
                ", x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", playerIndex=" + playerIndex +
                ", goalIndex=" + goalIndex +
                ", hasBall=" + hasBall +
                "}";
    }
}