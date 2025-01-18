# Optimization Testing
Our approach leverages tailored code construction strategies to generate input programs that meet optimization conditions. Subsequently, it applies various compiler optimization transformations to produce semantically equivalent test programs. By comparing the outputs of the pre- and post-transformation programs, this method effectively identifies incorrect optimization bugs. The detailed process is illustrated in the figure below. 

<img src="./workflow.jpg" alt="Workflow image" width="600" />

By default, we uses Csmith to produce seed programs. 

# Structure of the project
```
Core operations is under the main folder:
|-- AST_Information # The AST analysis implementation directory
|-- CsmithGen # The seed program generation implementation directory using Csmith
| |-- SwarmGen.java  # Updating configurations of Csmith
| |-- Main.java  # Main function of generation
|-- InconResultAnalysis # The programs with inconsistent results reduction implementation directory
|-- ObjectOperation # The common operations targeted different datatype
|-- TestResult # The programs testing execution and their results comparison implementation directory
| |-- TestMultiRunning.java  # Main function of testing and comparison

The script for applying Creal on a given seed program
|-- generate_csmith_seed.py # An auxiliary script for generating Csmith programs
|-- synthesizer # The synthesizer implementation directory
| |-- synthesizer.py # The synthesizer implementation of Creal
|-- profiler # Profiling tools
| |-- src # The code for the profiler
| |-- build # The compiled profiler(./build/bin/profile) used by synthesizer
|-- databaseconstructor # Constructing function database
| |-- functionextractor
| | |-- extractor.py # For extracting valid functions from a C/C++ project
| |-- generate.py # For generating IO for functions
```
