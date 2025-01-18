# Optimization Testing
Our approach leverages tailored code construction strategies to generate input programs that meet optimization conditions. Subsequently, it applies various compiler optimization transformations to produce semantically equivalent test programs. By comparing the outputs of the pre- and post-transformation programs, this method effectively identifies incorrect optimization bugs. The detailed process is illustrated in the figure below. 

<img src="./workflow.jpg" alt="Workflow image" width="600" />

By default, we uses Csmith to produce seed programs. 

# Structure of the project

```
Core operations is under the main folder:
|-- AST_Information # The AST analysis implementation directory
|-- mutations # The tailored code genertion strategies and mutate implementation directory target different optimizations
|-- csmith # The seed program generation implementation directory using Csmith
| |-- SwarmGen.java  # Updating configurations of Csmith
|-- RespectfulTestResult # The programs testing execution and their results comparison implementation directory
|-- ObjectOperation # The common operations targeted different datatype
|-- common # The common information extraction functions across overall process
|-- Overall # The overall testing process execution implemention directory
| |-- Main.java  # The entrance of overall testing
|-- processtimer # The process dealing and real-time memory check implemention directory
|-- sanitizer # The undefined behaviour filtering implemention directory
|-- utility # The java beans of overall testing process
```

# Usage

### Step 1: Install necessary packages

- Ubuntu >= 20
- Java >= 17
- Csmith (Please install it following [Csmith](https://github.com/csmith-project/csmith))
- CSMITH_HOME: After installing Csmith, please set the environment variable `CSMITH_HOME` to the installation path, with which we can locate `$CSMITH_HOME/include/csmith.h`.
- Clang >= 14
- add file `libsigar-amd64-linux.so` into `/usr/lib` and `/usr/lib64`

### Step 2: Test clang ast analysis functionality
using following instruction to test the clang ast analysis:
If there is a sample.c
```
```

`clang -fsyntax-only -Xclang -ast-dump example.c -w -Xanalyzer -analyzer-disable-all-checking -I $CSMITH_HOME/include`
then it outputs:
```

```
### Step 3: Update the corresponding folder information
In the `/src/Overall` folder, the `swarmDir` is a folder containing all seed programs generated using Csmith, while the muIndexPath is a folder that includes all test programs (i.e., both the original and transformed programs). Within this folder, each subfolder, named after the seed program, contains all the test programs generated for that specific seed. Furthermore, the execution results of these programs are also recorded in a text file. Finally, the execution results of all programs within the same folder are compared to check for consistency. If any discrepancies are found, the corresponding program is logged in a separate text file.

### Step 4: Open and Run the project
Open this project using `eclipse` or `idea`, and run the `Main.java` in the `/src/Overall`.

# Find Bugs
We conduct a preliminary evaluation of this approach on GCC and LLVM, and have successfully detected five incorrect optimization bugs.

# Bug List

| ID  | Compiler | Issue           | Status   |
| --- | -------- | --------------- | -------- |
| 1   | GCC      | [GCC-113709](https://gcc.gnu.org/bugzilla/show_bug.cgi?id=113709) | Fixed     |
| 2   | GCC      | [GCC-113702](https://gcc.gnu.org/bugzilla/show_bug.cgi?id=113702) | Fixed     |
| 3   | GCC      | [GCC-113669](https://gcc.gnu.org/bugzilla/show_bug.cgi?id=113669) | Confirmed |
| 4   | LLVM     | [LLVM-75809](https://github.com/llvm/llvm-project/issues/75809)   | Confirmed |
| 5   | LLVM     | [LLVM-112880](https://github.com/llvm/llvm-project/issues/112880) | Confirmed |






