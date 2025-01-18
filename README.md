# Optimization Testing
Our approach leverages tailored code construction strategies to generate input programs that meet optimization conditions. Subsequently, it applies various compiler optimization transformations to produce semantically equivalent test programs. By comparing the outputs of the pre- and post-transformation programs, this method effectively identifies incorrect optimization bugs. The detailed process is illustrated in the figure below. 

By default, Creal uses Csmith to produce seed programs. 
![workflow image](https://github.com/Elowen-jjw/test-opt/blob/main/workflow.jpg)
