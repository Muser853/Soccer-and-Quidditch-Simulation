import java.util.*;

public class SoccerSimulationRunner {
    private static final int NUM_TRIALS = 1000;

    // Results storage
    private static final Map<String, Double> strategyWinRates = new HashMap<>();
    private static final Map<String, Double> strategyMovingDistances = new HashMap<>();
    private static final Map<String, Double> strategySuccessfulPasses = new HashMap<>();
    private static final Map<String, Double> strategyFailedPasses = new HashMap<>();
    
    public static void main(String[] args) {
        System.out.println("Starting Soccer Simulation...");
        
        // Run simulations for different bounds
        for (int bound = 1; bound <= 1000; bound++) {
            if (bound % 10 == 0) {
                System.out.println("Running simulations for bound: " + bound);
            }
            
            // Run simulations for different adjacent radii
            for (double adjacentRadius = 1.0; adjacentRadius <= 10.0; adjacentRadius += 0.125) {
                if (bound % 10 == 0 && adjacentRadius == 1.0) {
                    System.out.println("  Adjacent radius: " + adjacentRadius);
                }
                runSimulationsForTeamSizes(bound, adjacentRadius);
            }
        }
        System.out.println("\n=== FINAL RESULTS ===");
        
        System.out.println("\nStrategy Win Rates:");
        strategyWinRates.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .forEach(entry -> System.out.printf("%s: %.2f%%\n", entry.getKey(), entry.getValue() * 100));
        
        System.out.println("\nAverage Moving Distances:");
        strategyMovingDistances.entrySet().stream()
            .sorted(Map.Entry.comparingByValue())
            .forEach(entry -> System.out.printf("%s: %.2f\n", entry.getKey(), entry.getValue()));
        
        System.out.println("\nAverage Successful Passes:");
        strategySuccessfulPasses.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .forEach(entry -> System.out.printf("%s: %.2f\n", entry.getKey(), entry.getValue()));
        
        System.out.println("\nAverage Failed Passes:");
        strategyFailedPasses.entrySet().stream()
            .sorted(Map.Entry.comparingByValue())
            .forEach(entry -> System.out.printf("%s: %.2f\n", entry.getKey(), entry.getValue()));
    }
    
    private static void runSimulationsForTeamSizes(int bound, double adjacentRadius) {
        // For 2 teams
        for (int teamSize1 = 2; teamSize1 <= 11; teamSize1++) {
            for (int teamSize2 = 2; teamSize2 <= 11; teamSize2++) {
                List<Integer> teamSizes = Arrays.asList(teamSize1, teamSize2);
                runSimulationsForTeamCombination(bound, adjacentRadius, teamSizes, 2);
            }
        }
        
        // For 3 to 6 teams, generate all combinations of team sizes
        for (int numTeams = 3; numTeams <= 6; numTeams++) {
            // Generate all combinations of team sizes
            int[] teamSizesArray = new int[numTeams];
            int maxTeamSize = 11; // Maximum team size
            
            // Initialize with minimum team size
            for (int i = 0; i < numTeams; i++) {
                teamSizesArray[i] = 2;
            }
            
            do {
                List<Integer> teamSizes = new ArrayList<>();
                for (int size : teamSizesArray) {
                    teamSizes.add(size);
                }
                runSimulationsForTeamCombination(bound, adjacentRadius, teamSizes, numTeams);
                
                // Increment the team sizes array
                for (int i = 0; i < numTeams; i++) {
                    if (teamSizesArray[i] < maxTeamSize) {
                        teamSizesArray[i]++;
                        break;
                    } else {
                        teamSizesArray[i] = 2;
                        if (i == numTeams - 1) {
                            // All teams reached max size, break out of loop
                            break;
                        }
                    }
                }
            } while (teamSizesArray[numTeams - 1] <= maxTeamSize);
        }
    }
    
    private static void runSimulationsForTeamCombination(int bound, double adjacentRadius, 
                                                      List<Integer> teamSizes, int numGoals) {
        List<List<SoccerStrategy>> strategyCombs = getAllStrategyCombinations(teamSizes.size());
        List<List<List<Integer>>> initialDistributions = getAllInitialDistributions(teamSizes);
        
        SoccerSimulation.StartingScenario[] scenarios = new SoccerSimulation.StartingScenario[] {
            SoccerSimulation.StartingScenario.GOAL_KICK,
            SoccerSimulation.StartingScenario.CORNER_KICK,
            SoccerSimulation.StartingScenario.KICK_OFF
        };
        for (SoccerSimulation.StartingScenario scenario : scenarios) {
            for (List<SoccerStrategy> strategies : strategyCombs) {
                for (List<List<Integer>> distribution : initialDistributions) {
                    runTrials(new SoccerSimulation(bound, adjacentRadius, numGoals), 
                    teamSizes, strategies, distribution, scenario);
                }
            }
        }
    }
    private static void runTrials(SoccerSimulation simulation, List<Integer> teamSizes, 
                               List<SoccerStrategy> strategies, List<List<Integer>> distributions,
                               SoccerSimulation.StartingScenario scenario) {
        int totalWins = 0;
        double totalMovingDistance = 0;
        int totalSuccessfulPasses = 0;
        int totalFailedPasses = 0;
        
        for (int trial = 0; trial < NUM_TRIALS; trial++) {
            simulation.initializeTeamsWithFormations(teamSizes, distributions, scenario);
            
            SoccerSimulation.SimulationResult result = simulation.runMultiTeamSimulation(strategies, scenario, 1000);
            totalWins++;
            totalMovingDistance += result.totalMovingDistance;
            totalSuccessfulPasses += result.successfulPasses;
            totalFailedPasses += result.failedPasses;
        }
        double winRate = (double) totalWins / NUM_TRIALS;
        double avgMovingDistance = totalWins > 0 ? totalMovingDistance / totalWins : 0;
        double avgSuccessfulPasses = totalWins > 0 ? (double) totalSuccessfulPasses / totalWins : 0;
        double avgFailedPasses = totalWins > 0 ? (double) totalFailedPasses / totalWins : 0;

        for (int i = 0; i < strategies.size(); i++) {
            String strategyName = strategies.get(i).name;
            String key = strategyName + "_Team" + (i + 1);
            
            strategyWinRates.put(key, strategyWinRates.getOrDefault(key, 0.0) + winRate);
            strategyMovingDistances.put(key, strategyMovingDistances.getOrDefault(key, 0.0) + avgMovingDistance);
            strategySuccessfulPasses.put(key, strategySuccessfulPasses.getOrDefault(key, 0.0) + avgSuccessfulPasses);
            strategyFailedPasses.put(key, strategyFailedPasses.getOrDefault(key, 0.0) + avgFailedPasses);
        }
    }
    
    private static List<List<SoccerStrategy>> getAllStrategyCombinations(int numTeams) {
        List<SoccerStrategy> allStrategies = Arrays.asList(
            new ActivePressingStrategy(),
            new ShortPassStrategy(),
            new BallControlStrategy(),
            new VoronoiInspiredStrategy()
        );
        List<List<SoccerStrategy>> combinations = new ArrayList<>();
        generateStrategyCombinations(allStrategies, new ArrayList<>(), combinations, numTeams);
        return combinations;
    }
    private static void generateStrategyCombinations(List<SoccerStrategy> allStrategies, 
                                                  List<SoccerStrategy> current,
                                                  List<List<SoccerStrategy>> result, 
                                                  int remaining) {
        if (remaining == 0) {
            result.add(new ArrayList<>(current));
            return;
        }
        for (SoccerStrategy strategy : allStrategies) {
            current.add(strategy);
            generateStrategyCombinations(allStrategies, current, result, remaining - 1);
            current.removeLast();
        }
    }
    
    private static List<List<List<Integer>>> getAllInitialDistributions(List<Integer> teamSizes) {
        List<List<List<Integer>>> allDistributions = new ArrayList<>();
        List<List<Integer>> currentDistribution = new ArrayList<>();
        
        for (int teamSize : teamSizes) {
            List<List<Integer>> possibleFormations = generateAlignments(teamSize);
            currentDistribution.add(possibleFormations.getFirst()); // Start with first formation
        }
        generateAllDistributions(teamSizes, 0, currentDistribution, allDistributions);
        return allDistributions;
    }
    
    private static void generateAllDistributions(List<Integer> teamSizes, int teamIndex,
                                              List<List<Integer>> current,
                                              List<List<List<Integer>>> result) {
        if (teamIndex == teamSizes.size()) {
            result.add(new ArrayList<>(current));
            return;
        }
        List<List<Integer>> possibleFormations = generateAlignments(teamSizes.get(teamIndex));
        for (List<Integer> formation : possibleFormations) {
            current.set(teamIndex, formation);
            generateAllDistributions(teamSizes, teamIndex + 1, current, result);
        }
    }
        public static List<List<Integer>> findCombinations(int target, int start, List<Integer> path, List<List<Integer>> result) {
        if (result == null) {
            result = new ArrayList<>();
        }
        if (path == null) {
            path = new ArrayList<>();
        }
        int currentSum = path.stream().mapToInt(Integer::intValue).sum();

        if (currentSum == target) {
            result.add(new ArrayList<>(path));
            return result;
        }
        else if (currentSum < target)
        for (int i = start; i <= target; i++) {
            path.add(i);
            findCombinations(target, i, path, result);
            path.removeLast();
        }
        return result;
    }

    public static List<List<Integer>> findPermutations(List<Integer> combination, List<List<Integer>> result) {
        if (result == null) {
            result = new ArrayList<>();
        }
        if (combination.size() == 1) {
            result.add(new ArrayList<>(combination));
            return result;
        }
        for (int i = 0; i < combination.size(); i++) {
            int current = combination.get(i);
            List<Integer> remaining = new ArrayList<>(combination);
            remaining.remove(i);
            findPermutations(remaining, result);
            for (List<Integer> perm : new ArrayList<>(result)) {
                List<Integer> newPerm = new ArrayList<>();
                newPerm.add(current);
                newPerm.addAll(perm);
                result.add(newPerm);
            }
            result.removeLast(); // Remove the last added permutation to avoid duplicates
        }
        return result;
    }

    public static Set<List<Integer>> uniquePermutations(List<Integer> combination) {
        Set<List<Integer>> permutationsSet = new HashSet<>();
        generatePermutations(combination, new ArrayList<>(), permutationsSet);
        return permutationsSet;
    }

    private static void generatePermutations(List<Integer> combination, List<Integer> current, Set<List<Integer>> result) {
        if (combination.isEmpty()) {
            result.add(new ArrayList<>(current));
            return;
        }
        for (int i = 0; i < combination.size(); i++) {
            int num = combination.get(i);
            List<Integer> remaining = new ArrayList<>(combination);
            remaining.remove(i);
            current.add(num);
            generatePermutations(remaining, current, result);
            current.removeLast();
        }
    }
    public static List<List<Integer>> generateAlignments(int teamSize){
        Set<List<Integer>> allPermutations = new HashSet<>();
        for(int sum=1; sum <= teamSize; sum ++) {
            List<List<Integer>> combinations = findCombinations(teamSize, 1, null, null);

            for (List<Integer> combination : combinations) {
                Set<List<Integer>> perms = uniquePermutations(combination);
                allPermutations.addAll(perms);
            }
            allPermutations.stream()
                .sorted((a, b) -> {
                    if (a.size() != b.size()) {
                        return Integer.compare(a.size(), b.size());
                    }
                    return a.toString().compareTo(b.toString());
                });
            }
        return new ArrayList<>(allPermutations);
    }
}