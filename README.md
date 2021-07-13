# UET-UCT-order
This repository contains all the code that was used in the experiments in the Chapter 9 of the following thesis:
TODO: Add the citation of the thesis when it is fixed

## Installing

This project is a standard maven project in Java. As such an executable jar file can be built with the following command:

```
mvn clean package
```

## Running

As soon as maven has finished building the executable jar file, you can find it in the target folder. Copy and paste the jar file named UETUCT-order.jar into the home directory of this project. Finally, you can execute it to solve an instance of the UET-UCT with the method of your choice.

```
java -jar UETUCT-order.jar ConfigurationName timeLimitInMinutes pathToInstanceJSONFile
```

For example, if you want to solve the instance at data/STG/50/50_2_rand0000.json with the ORDER approach with a 5 minutes time limit (as we use for our benchmark), you should execute the following command:

```
java -jar UETUCT-order.jar ORDER 5 "data/STG/50/50_2_rand0000.json"
```

By the end of any execution, the final line that was printed indicate the solving statistics as such:

```
instanceName;timeToProof;timeToBest;Objective;nbNodes;nbBacktracks;nbFails;
```

timeToProof and timeToBest are both expressed in milliseconds (ms). In the case of our example, the final line should look like to something like this:

```
50_2_rand0000;112;106;25;50;99;49;
```

## Look into the code

If you want to have a look at the code, here is its packages organisation:
* **constraint**: 
  * The class PropOrderUETUCT.java is the propagator for the Order constraint specified for the duplication UET-UCT.
  * Finally, the class UETUCTModel.java builds a configurable Choco model of the UET-UCT for the instance given in parameter. The class also contains a main method easy to execute inside an IDE and showing how to easy configure the model to be solved.
* **data**: this package contains code useful for input/output processing, especially inside the Factory.java class.

## Having a problem ?
For any encountered problem, do not hesitate to raise an issue or to directly contact me at arth.godet@gmail.com. I would be happy to answer any question with the code.
