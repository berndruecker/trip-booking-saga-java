package io.flowing.trip.saga.camunda;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.builder.AbstractFlowNodeBuilder;
import org.springframework.stereotype.Component;

@Component
@Singleton
@SuppressWarnings("rawtypes")
public class TripBookingSaga {

  @Inject
  private ProcessEngine camunda;
  
  @PostConstruct
  public void defineSaga() {
    AbstractFlowNodeBuilder saga = Bpmn.createExecutableProcess("trip").startEvent();
  
    addActivity(saga, "Reserve car", ReserveCarAdapter.class);
    addCompensationActivity(saga, "Cancel car", CancelCarAdapter.class);
    
    addActivity(saga, "Book hotel", BookHotelAdapter.class);
    addCompensationActivity(saga, "Cancel hotel", CancelHotelAdapter.class);

    addActivity(saga, "Book flight", BookFlightAdapter.class);
    addCompensationActivity(saga, "Cancel flight", CancelFlightAdapter.class);

    camunda.getRepositoryService().createDeployment() //
     .addModelInstance("trip.bpmn", saga.endEvent().done()) //
     .deploy();
  }    
  
  
  private void addActivity(AbstractFlowNodeBuilder saga, String name, Class adapterClass) {
    // this is very handy and could also be done inline above directly
    saga.serviceTask().name(name).camundaClass(adapterClass.getName());
  }

  private void addCompensationActivity(AbstractFlowNodeBuilder saga, String string, Class class1) {
    // but unfortunately this is currently pretty unhandy in the model API (to be improved!) so I extracted that to an own helper method    
    // TODO 

  }
  
  
//
//  private void addCompensationServiceTask(BpmnModelInstance saga, String string, String string2, Class class1) {
//    // TODO Auto-generated method stub
//    
//  }
//  
//  private void addCompensationServiceTask(AbstractFlowNodeBuilder saga, String string, Class<CancelReservationAdapter> class1) {
//    // TODO Auto-generated method stub
//    
//  }
//  
//  public void defineSaga() {
//    camunda.getRepositoryService().createDeployment()
//      .addModelInstance("trip.bpmn", Bpmn.createExecutableProcess("trip") //    
//          .startEvent()
////          .compensation().compensationDone()
//          .subProcess()
//          .subProcessDone()
//          .serviceTask().name("Reserve car").camundaClass(ReserveCarAdapter.class.getName()) //
//          .boundaryEvent()
////            .compensationHandler( BpmnPartials.serviceTask().name("Cancel car").camundaClass(ReserveCarAdapter.class) )
//          .serviceTask().name("Reserve car").camundaClass(ReserveCarAdapter.class.getName()) //
//            .compensation().serviceTask().name("Cancel car").camundaClass(ReserveCarAdapter.class).compensationDone()
//          .serviceTask().name("Reserve car").camundaClass(ReserveCarAdapter.class.getName()) //
//          .endEvent() //
//          .done());
//  }
//
//  public void defineSaga2() {
//     BpmnModelInstance saga = Bpmn.createExecutableProcess("trip") //    
//          .startEvent()
//          .serviceTask().name("Reserve car").camundaClass(ReserveCarAdapter.class.getName()) //
//          .serviceTask().name("Reserve car").camundaClass(ReserveCarAdapter.class.getName()) //
//          .serviceTask().name("Reserve car").camundaClass(ReserveCarAdapter.class.getName()) //
//          .endEvent() //
//          .done();
//     
//     addCompensationServiceTask(saga, "Reserve Car", "Cancel Car", CancelReservationAdapter.class);
//     
//     camunda.getRepositoryService().createDeployment() //
//       .addModelInstance("trip.bpmn", saga) //
//       .deploy();
//  }
//  
//
//  
//  public void defineSaga3() {
//    AbstractFlowNodeBuilder saga = Bpmn.createExecutableProcess("trip").startEvent();
//  
//    saga = saga.serviceTask().name("Reserve car").camundaClass(ReserveCarAdapter.class.getName());
//    addCompensationServiceTask(saga, "Cancel Reservation", CancelReservationAdapter.class);
//    
//    saga = saga.serviceTask().name("Reserve car").camundaClass(ReserveCarAdapter.class.getName());
//    saga = saga.serviceTask().name("Reserve car").camundaClass(ReserveCarAdapter.class.getName());
//       
//    camunda.getRepositoryService().createDeployment() //
//     .addModelInstance("trip.bpmn", saga.endEvent().done()) //
//     .deploy();
//  }  

}
