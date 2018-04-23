package io.flowing.trip.saga.camunda.simple;

import java.io.File;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.camunda.bpm.engine.variable.Variables;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.builder.ProcessBuilder;

import io.flowing.trip.saga.camunda.adapter.BookFlightAdapter;
import io.flowing.trip.saga.camunda.adapter.BookHotelAdapter;
import io.flowing.trip.saga.camunda.adapter.CancelCarAdapter;
import io.flowing.trip.saga.camunda.adapter.CancelFlightAdapter;
import io.flowing.trip.saga.camunda.adapter.CancelHotelAdapter;
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
    ProcessBuilder flow = Bpmn.createExecutableProcess("trip");
    
    // - flow of activities and compensating actions
    flow.startEvent()
        .serviceTask("car").name("Reserve car").camundaClass(ReserveCarAdapter.class)
          .boundaryEvent().compensateEventDefinition().compensateEventDefinitionDone()
          .compensationStart().serviceTask("CancelCar").camundaClass(CancelCarAdapter.class).compensationDone()
        .serviceTask("hotel").name("Book hotel").camundaClass(BookHotelAdapter.class)
          .boundaryEvent().compensateEventDefinition().compensateEventDefinitionDone()
          .compensationStart().serviceTask("CancelHotel").camundaClass(CancelHotelAdapter.class).compensationDone()
        .serviceTask("flight").name("Book flight").camundaClass(BookFlightAdapter.class)
          .boundaryEvent().compensateEventDefinition().compensateEventDefinitionDone()
          .compensationStart().serviceTask("CancelFlight").camundaClass(CancelFlightAdapter.class).compensationDone()
        .endEvent();
    
    // - trigger compensation in case of any exception (other triggers are possible)
    flow.eventSubProcess()
        .startEvent().error("java.lang.Throwable")
        .intermediateThrowEvent().compensateEventDefinition().compensateEventDefinitionDone()
        .endEvent();     
    
    // ready
    BpmnModelInstance saga = flow.done();
    // optional: Write to file to be able to open it in Camunda Modeler
    //Bpmn.writeModelToFile(new File("trip.bpmn"), saga);

    // finish Saga and deploy it to Camunda
    camunda.getRepositoryService().createDeployment() //
        .addModelInstance("trip.bpmn", saga) //
        .deploy();
    
    // now we can start running instances of our saga - its state will be persisted
    camunda.getRuntimeService().startProcessInstanceByKey("trip", Variables.putValue("name", "trip1"));
    camunda.getRuntimeService().startProcessInstanceByKey("trip", Variables.putValue("name", "trip2"));
  }

}
