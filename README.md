# Sports Simulation Framework

This project provides a comprehensive framework for simulating both soccer and quidditch games. The framework is designed to be extensible and allows for various team configurations, strategies, and game scenarios.

## Components

### 1. SoccerSimulation
The core soccer simulation engine that handles:
- Game mechanics and physics
- Player movements and interactions
- Ball control and passing
- Goal scoring and team management
- Different starting scenarios (Kick-off, Corner kick, Goal kick)
- Support for both 2D and 3D simulations

Key Features:
- Customizable field dimensions
- Adjustable player behavior parameters
- Multiple team support
- Detailed game statistics tracking
- Strategy performance evaluation

### 2. QuidditchSimulation
A specialized simulation for the magical sport of Quidditch, featuring:
- 3D game space with width, height, and depth
- Unique game elements:
  - Quaffle (main ball)
  - Bludgers (aggressive balls)
  - Golden Snitch (special capture target)
- Player roles:
  - Chasers (score goals)
  - Beaters (handle bludgers)
  - Keeper (defend goals)
  - Seeker (catch the golden snitch)
- Advanced physics for flying movements

### 3. SoccerSimulationRunner
The main simulation runner that orchestrates:
- Multiple simulation runs with different parameters
- Systematic team size combinations
- Strategy performance analysis
- Result aggregation and reporting

Key Features:
- Parameterized simulation runs
- Team size combination generation
- Strategy win rate calculation
- Performance metrics tracking
- Detailed result visualization

## Simulation Parameters

### SoccerSimulation Parameters
- Field dimensions
- Player movement bounds
- Adjacent radius for player interactions
- Number of teams and players per team
- Starting scenarios
- 2D/3D simulation mode

### QuidditchSimulation Parameters
- Field dimensions (3D)
- Ball speeds (Quaffle, Bludger, Golden Snitch)
- Maximum turn count
- Goal dimensions
- Player roles distribution

## Running the Simulations

1. Soccer Simulation:
```java
java SoccerSimulationRunner
```
The runner will automatically:
- Run simulations for different field bounds (1-1000)
- Test various adjacent radii (1.0 to 10.0)
- Generate all possible team size combinations
- Evaluate different strategies
- Generate comprehensive results

2. Quidditch Simulation:
```java
java QuidditchSimulation
```
The simulation will run with default parameters, but these can be customized by modifying the constants in the QuidditchSimulation class.

## Performance Metrics

The framework tracks and reports several key performance metrics:

1. Strategy Performance:
- Win rates
- Moving distances
- Successful passes
- Failed passes

2. Team Performance:
- Goal scoring
- Player movements
- Ball control efficiency
- Team coordination

3. Game Statistics:
- Total iterations
- Ball possession time
- Player interactions
- Strategy effectiveness

## Extensibility

The framework is designed to be easily extensible:

1. New Strategies:
- Implement custom player strategies
- Add new behavior patterns
- Create specialized team tactics

2. New Game Elements:
- Add new types of balls
- Introduce new player roles
- Create custom game scenarios

3. New Metrics:
- Add custom performance indicators
- Track additional game statistics
- Implement new evaluation methods

## Results Analysis

The simulation runner provides detailed analysis of the results, including:

1. Strategy Comparison:
- Win rate percentages
- Moving distance analysis
- Pass success/failure rates

2. Team Performance:
- Goal scoring efficiency
- Team coordination metrics
- Player movement patterns

3. Parameter Impact:
- Effect of field size on gameplay
- Influence of adjacent radius
- Impact of team size combinations

## Contributing

To contribute to the project:

1. Fork the repository
2. Create a feature branch
3. Implement your changes
4. Submit a pull request

Please ensure that any new features maintain compatibility with the existing framework and include appropriate documentation.
