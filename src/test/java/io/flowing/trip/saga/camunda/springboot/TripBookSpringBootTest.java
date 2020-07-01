package io.flowing.trip.saga.camunda.springboot;

import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.*;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.variable.Variables;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest( // webEnvironment = WebEnvironment.NONE, 
  // start web environment to work around https://forum.camunda.org/t/error-executing-upon-starting-within-unit-tests/16376/10
    classes = TripBookinApplication.class, //
    properties = { //
        "camunda.bpm.job-execution.enabled=false", //
        "camunda.bpm.auto-deployment-enabled=false", //
        "restProxyHost=api.example.org", //
        "restProxyPort=80" })
@Deployment(resources = { "order.bpmn" })
public class TripBookSpringBootTest {

  @Autowired
  private ProcessEngine processEngine;

  @Test
  public void testTripBooking() throws Exception {
    // Start an instance
    ProcessInstance processInstance = processEngine.getRuntimeService().startProcessInstanceByKey(
        "trip", 
        Variables.putValue("someVariableToPass", "123"));

    // which will run through fully automated
    assertThat(processInstance).isEnded();

    // as everything is hard coded, these assertions does not make too much sense, but it might help you to get the idea
    assertThat(processInstance).variables().containsEntry("someVariableToPass", "123");
    assertThat(processInstance).hasPassed("Activity-Reserve-car", "Activity-Book-hotel", "Activity-Book-flight", "Activity-Cancel-hotel-compensation", "Activity-Cancel-car-compensation");
  }
}
