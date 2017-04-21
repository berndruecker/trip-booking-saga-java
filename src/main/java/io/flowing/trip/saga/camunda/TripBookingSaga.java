package io.flowing.trip.saga.camunda;

import java.io.File;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.model.bpmn.AssociationDirection;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.builder.AbstractActivityBuilder;
import org.camunda.bpm.model.bpmn.builder.AbstractFlowNodeBuilder;
import org.camunda.bpm.model.bpmn.instance.Association;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.BoundaryEvent;
import org.camunda.bpm.model.bpmn.instance.BpmnModelElementInstance;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.camunda.bpm.model.bpmn.instance.SubProcess;
import org.camunda.bpm.model.xml.ModelInstance;
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
  
    saga = addActivity(saga, "Reserve car", ReserveCarAdapter.class);
    saga = addCompensationActivity(saga, "Cancel car", CancelCarAdapter.class);
    
    saga = addActivity(saga, "Book hotel", BookHotelAdapter.class);
    saga = addCompensationActivity(saga, "Cancel hotel", CancelHotelAdapter.class);

    saga = addActivity(saga, "Book flight", BookFlightAdapter.class);
    saga = addCompensationActivity(saga, "Cancel flight", CancelFlightAdapter.class);
    
    saga = addCompensationTriggerOnAnyError(saga);

    BpmnModelInstance modelInstance = saga.endEvent().done();
    camunda.getRepositoryService().createDeployment() //
     .addModelInstance("trip.bpmn", modelInstance) //
     .deploy();
    
    File file = new File("result.bpmn");
    Bpmn.writeModelToFile(file, modelInstance);
  }    
  
  
  private AbstractFlowNodeBuilder addCompensationTriggerOnAnyError(AbstractFlowNodeBuilder saga) {    
    ModelInstance modelInstance = saga.getElement().getModelInstance();        
    BpmnModelElementInstance parent = (BpmnModelElementInstance) saga.getElement().getParentElement(); // WHooha

    SubProcess eventSubProcess = modelInstance.newInstance(SubProcess.class);
    parent.addChildElement(eventSubProcess);

    eventSubProcess.builder().triggerByEvent().embeddedSubProcess()
      .startEvent().error()
      .intermediateThrowEvent().compensateEventDefinition().compensateEventDefinitionDone()
      .endEvent()
      .subProcessDone();
    
    return saga;
  }


  private AbstractFlowNodeBuilder addActivity(AbstractFlowNodeBuilder saga, String name, Class adapterClass) {
    // this is very handy and could also be done inline above directly
    return saga.serviceTask().name(name).camundaClass(adapterClass.getName());
  }

  private AbstractFlowNodeBuilder addCompensationActivity(AbstractFlowNodeBuilder saga, String name, Class adapterClass) {
    // but unfortunately this is currently pretty unhandy in the model API (to be improved!) so I extracted that to an own helper method    
    String currentElementId = saga.getElement().getAttributeValue("id");    
    String boundaryEventId = currentElementId + "-compensation-event";
    String compensationTaskId = currentElementId + "-compensation";

    ModelInstance modelInstance = saga.getElement().getModelInstance();
    
    saga = ((AbstractActivityBuilder)saga).boundaryEvent(boundaryEventId)
      .compensateEventDefinition()
      .compensateEventDefinitionDone()
      .moveToActivity(currentElementId);
    
    BoundaryEvent boundaryEvent = modelInstance.getModelElementById(boundaryEventId);
    BaseElement scope = (BaseElement) boundaryEvent.getParentElement();

    ServiceTask compensationHandler = modelInstance.newInstance(ServiceTask.class);
    compensationHandler.setId(compensationTaskId);
    compensationHandler.setForCompensation(true);
    compensationHandler.setCamundaClass(adapterClass.getName());
    scope.addChildElement(compensationHandler);

    Association association = modelInstance.newInstance(Association.class);
    association.setAssociationDirection(AssociationDirection.One);
    association.setSource(boundaryEvent);
    association.setTarget(compensationHandler);
    scope.addChildElement(association);
    
    return saga;
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
