package io.flowing.trip.saga.camunda.springboot;

import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.camunda.bpm.engine.variable.Variables;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.flowing.trip.saga.camunda.simple.TripBookingSaga;

public class TripBookJUnitTest {

  @Rule
  public ProcessEngineRule processEngineRule = new ProcessEngineRule();

  @Before
  public void deploy() {
    // IMPORTANT: THIS IS NOT THE BEST PRACTICE TO USE THE ENGINE OR TEST CASES
    // I JUST USED A public static void main TO MAKE THE EXAMPLE SUPER SIMPLE!!
    processEngineRule.getProcessEngine().getRepositoryService().createDeployment()
      .addModelInstance("trip.bpmn", TripBookingSaga.createSaga()) //
      .deploy();
  }
  
  @Test
  public void ruleUsageExample() {
    // Start an instance
    ProcessInstance processInstance = processEngineRule.getRuntimeService() //
        .startProcessInstanceByKey("trip", Variables.createVariables().putValue("someVariableToPass", "123"));

    // which will run through fully automated
    assertThat(processInstance).isEnded();

    // as everything is hard coded, these assertions does not make too much sense, but it might help you to get the idea
    assertThat(processInstance).variables().containsEntry("someVariableToPass", "123");
    assertThat(processInstance).hasPassed("car", "hotel", "flight", "CancelHotel", "CancelCar");

  }
}
