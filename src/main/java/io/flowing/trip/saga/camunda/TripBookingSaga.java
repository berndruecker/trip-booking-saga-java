package io.flowing.trip.saga.camunda;

import java.io.File;

import javax.annotation.PostConstruct;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.flowing.trip.saga.camunda.builder.SagaBuilder;

@Component
//@Singleton
public class TripBookingSaga {

  @Autowired
  private ProcessEngine camunda;

  @PostConstruct
  public void defineSaga() {
    SagaBuilder saga = SagaBuilder.newSaga("trip") //
        .activity("Reserve car", ReserveCarAdapter.class) //
        .compensationActivity("Cancel car", CancelCarAdapter.class) //
        .activity("Book hotel", BookHotelAdapter.class) //
        .compensationActivity("Cancel hotel", CancelHotelAdapter.class) //
        .activity("Book flight", BookFlightAdapter.class) //
        .compensationActivity("Cancel flight", CancelFlightAdapter.class) //
        .end() //
        .triggerCompensationOnAnyError();

    camunda.getRepositoryService().createDeployment() //
        .addModelInstance("trip.bpmn", saga.getModel()) //
        .deploy();

    File file = new File("result.bpmn");
    Bpmn.writeModelToFile(file, saga.getModel());
  }

  //
  // private void addCompensationServiceTask(BpmnModelInstance saga, String
  // string, String string2, Class class1) {
  // // TODO Auto-generated method stub
  //
  // }
  //
  // private void addCompensationServiceTask(AbstractFlowNodeBuilder saga,
  // String string, Class<CancelReservationAdapter> class1) {
  // // TODO Auto-generated method stub
  //
  // }
  //
  // public void defineSaga() {
  // camunda.getRepositoryService().createDeployment()
  // .addModelInstance("trip.bpmn", Bpmn.createExecutableProcess("trip") //
  // .startEvent()
  //// .compensation().compensationDone()
  // .subProcess()
  // .subProcessDone()
  // .serviceTask().name("Reserve
  // car").camundaClass(ReserveCarAdapter.class.getName()) //
  // .boundaryEvent()
  //// .compensationHandler( BpmnPartials.serviceTask().name("Cancel
  // car").camundaClass(ReserveCarAdapter.class) )
  // .serviceTask().name("Reserve
  // car").camundaClass(ReserveCarAdapter.class.getName()) //
  // .compensation().serviceTask().name("Cancel
  // car").camundaClass(ReserveCarAdapter.class).compensationDone()
  // .serviceTask().name("Reserve
  // car").camundaClass(ReserveCarAdapter.class.getName()) //
  // .endEvent() //
  // .done());
  // }
  //
  // public void defineSaga2() {
  // BpmnModelInstance saga = Bpmn.createExecutableProcess("trip") //
  // .startEvent()
  // .serviceTask().name("Reserve
  // car").camundaClass(ReserveCarAdapter.class.getName()) //
  // .serviceTask().name("Reserve
  // car").camundaClass(ReserveCarAdapter.class.getName()) //
  // .serviceTask().name("Reserve
  // car").camundaClass(ReserveCarAdapter.class.getName()) //
  // .endEvent() //
  // .done();
  //
  // addCompensationServiceTask(saga, "Reserve Car", "Cancel Car",
  // CancelReservationAdapter.class);
  //
  // camunda.getRepositoryService().createDeployment() //
  // .addModelInstance("trip.bpmn", saga) //
  // .deploy();
  // }
  //
  //
  //
  // public void defineSaga3() {
  // AbstractFlowNodeBuilder saga =
  // Bpmn.createExecutableProcess("trip").startEvent();
  //
  // saga = saga.serviceTask().name("Reserve
  // car").camundaClass(ReserveCarAdapter.class.getName());
  // addCompensationServiceTask(saga, "Cancel Reservation",
  // CancelReservationAdapter.class);
  //
  // saga = saga.serviceTask().name("Reserve
  // car").camundaClass(ReserveCarAdapter.class.getName());
  // saga = saga.serviceTask().name("Reserve
  // car").camundaClass(ReserveCarAdapter.class.getName());
  //
  // camunda.getRepositoryService().createDeployment() //
  // .addModelInstance("trip.bpmn", saga.endEvent().done()) //
  // .deploy();
  // }

}
