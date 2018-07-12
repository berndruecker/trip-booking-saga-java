package io.flowing.trip.saga.camunda.springboot;


import org.camunda.bpm.BpmPlatform;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.variable.Variables;
import org.camunda.bpm.spring.boot.starter.annotation.EnableProcessApplication;
import org.h2.tools.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableAutoConfiguration
@EnableProcessApplication
@ComponentScan
public class Application {

  public static void main(String... args) throws Exception {
    SpringApplication.run(Application.class, args);

    // do default setup of platform (everything is only applied if not yet there)
    ProcessEngine engine = BpmPlatform.getDefaultProcessEngine();
    
    // start a Saga right away
    engine.getRuntimeService().startProcessInstanceByKey(
        "trip", 
        Variables.putValue("someVariableToPass", "someValue"));
    
    // Start H2 server to be able to connect to database from the outside
    Server.createTcpServer(new String[] { "-tcpPort", "8092", "-tcpAllowOthers" }).start();
  }

}
