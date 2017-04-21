package io.flowing.trip.saga.camunda;

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

public class SagaBuilder {

  @SuppressWarnings("rawtypes")
  private AbstractFlowNodeBuilder saga;
  private BpmnModelInstance bpmnModelInstance;
  
  public static SagaBuilder newSaga(String name) {
    SagaBuilder builder = new SagaBuilder();
    return builder.start();
  }


  public SagaBuilder start() {
    saga = Bpmn.createExecutableProcess("trip").startEvent();
    return this;
  }

  public SagaBuilder done() {
    bpmnModelInstance = saga.endEvent().done();
    return this;
  }
  
  public BpmnModelInstance getModel() {
    if (bpmnModelInstance==null) {
      done();
    }
    return bpmnModelInstance;
  }

  public SagaBuilder triggerCompensationOnAnyError() {    
    ModelInstance modelInstance = saga.getElement().getModelInstance();        
    BpmnModelElementInstance parent = (BpmnModelElementInstance) saga.getElement().getParentElement(); // WHooha

    SubProcess eventSubProcess = modelInstance.newInstance(SubProcess.class);
    parent.addChildElement(eventSubProcess);

    eventSubProcess.builder().triggerByEvent().embeddedSubProcess()
      .startEvent().error("java.lang.Throwable")
      .intermediateThrowEvent().compensateEventDefinition().compensateEventDefinitionDone()
      .endEvent()
      .subProcessDone();

    return this;
  }

  public SagaBuilder activity(String name, Class adapterClass) {
    // this is very handy and could also be done inline above directly
    saga = saga.serviceTask().name(name).camundaClass(adapterClass.getName());
    return this;
  }

  public SagaBuilder compensationActivity(String name, Class adapterClass) {
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
    
    return this;
  }
}
