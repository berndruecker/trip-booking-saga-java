package io.flowing.trip.saga.camunda.simple;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.camunda.bpm.engine.variable.Variables;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.builder.ProcessBuilder;

import io.flowing.trip.saga.camunda.adapter.BookFlightAdapter;
import io.flowing.trip.saga.camunda.adapter.BookHotelAdapter;
import io.flowing.trip.saga.camunda.adapter.CancelCarAdapter;
import io.flowing.trip.saga.camunda.adapter.ReserveCarAdapter;

/**
 * One main class containing everything to run a Saga
 * using Camunda and BPMN. The class runs an in memory engine,
 * defines the Saga and run a couple of instances.
 */
public class TripBookingSaga {

  public static void main(String[] args) {
    // Configure and startup (in memory) engine
    ProcessEngine camunda = 
        new StandaloneInMemProcessEngineConfiguration()
          .buildProcessEngine();
    
    // define saga as BPMN process
    ProcessBuilder saga = Bpmn.createExecutableProcess("trip");
    
    // - flow of activities and compensating actions
    saga.startEvent()
        .serviceTask("car").name("Reserve car").camundaClass(ReserveCarAdapter.class)
          .boundaryEvent().compensateEventDefinition().compensateEventDefinitionDone()
          .compensationStart().serviceTask("car-compensate").name("Cancel car").camundaClass(CancelCarAdapter.class).compensationDone()
        .serviceTask("hotel").name("Book hotel").camundaClass(BookHotelAdapter.class)
          .boundaryEvent().compensateEventDefinition().compensateEventDefinitionDone()
          .compensationStart().serviceTask("hotel-compensate").name("Hotel car").camundaClass(CancelCarAdapter.class).compensationDone()
        .serviceTask("flight").name("Book flight").camundaClass(BookFlightAdapter.class)
          .boundaryEvent().compensateEventDefinition().compensateEventDefinitionDone()
          .compensationStart().serviceTask("flight-compensate").name("Cancel flight").camundaClass(CancelCarAdapter.class).compensationDone()
        .endEvent();
    
    // - trigger compensation in case of any exception (other triggers are possible)
    saga.eventSubProcess()
        .startEvent().error("java.lang.Throwable")
        .intermediateThrowEvent().compensateEventDefinition().compensateEventDefinitionDone()
        .endEvent();     

    // finish Saga and deploy it to Camunda
    camunda.getRepositoryService().createDeployment() //
        .addModelInstance("trip.bpmn", saga.done()) //
        .deploy();
    
    // now we can start running instances of our saga - its state will be persisted
    camunda.getRuntimeService().startProcessInstanceByKey("trip", Variables.putValue("name", "trip1"));
    camunda.getRuntimeService().startProcessInstanceByKey("trip", Variables.putValue("name", "trip2"));
  }

}
