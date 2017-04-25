package io.flowing.trip.saga.camunda.builder;

import static org.camunda.bpm.model.bpmn.builder.AbstractBaseElementBuilder.SPACE;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;

import org.camunda.bpm.model.bpmn.AssociationDirection;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.builder.AbstractActivityBuilder;
import org.camunda.bpm.model.bpmn.builder.AbstractBaseElementBuilder;
import org.camunda.bpm.model.bpmn.builder.AbstractBpmnModelElementBuilder;
import org.camunda.bpm.model.bpmn.builder.AbstractFlowNodeBuilder;
import org.camunda.bpm.model.bpmn.instance.Activity;
import org.camunda.bpm.model.bpmn.instance.Association;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.BoundaryEvent;
import org.camunda.bpm.model.bpmn.instance.BpmnModelElementInstance;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.camunda.bpm.model.bpmn.instance.SubProcess;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnEdge;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnPlane;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnShape;
import org.camunda.bpm.model.bpmn.instance.dc.Bounds;
import org.camunda.bpm.model.bpmn.instance.di.Waypoint;
import org.camunda.bpm.model.xml.ModelInstance;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;

public class SagaBuilder {

  @SuppressWarnings("rawtypes")
  private AbstractFlowNodeBuilder saga;
  private BpmnModelInstance bpmnModelInstance;
  private String name;
  
  public SagaBuilder(String name) {
    this.name = name;
  }

  public static SagaBuilder newSaga(String name) {
    SagaBuilder builder = new SagaBuilder(name);
    return builder.start();
  }
  
  public BpmnModelInstance getModel() {
    if (bpmnModelInstance==null) {
      bpmnModelInstance = saga.done();
    }
    return bpmnModelInstance;
  }

  public SagaBuilder start() {
    saga = Bpmn.createExecutableProcess(name).startEvent("Start-" + name);
    return this;
  }
  
  public SagaBuilder end() {
    saga = saga.endEvent("End-" + name);
    return this;
  }  

  @SuppressWarnings("rawtypes")
  public SagaBuilder activity(String name, Class adapterClass) {
    // this is very handy and could also be done inline above directly
    String id = "Activity-" + name.replace(" ", "-"); // risky thing ;-)
    saga = saga.serviceTask(id).name(name).camundaClass(adapterClass.getName());
    return this;
  }
  
  @SuppressWarnings("rawtypes")
  public SagaBuilder compensationActivity(String name, Class adapterClass) {
    /*
     * This is a bit unhandy with the Camunda Model API at the moment.
     * It will be adresses with 
     *  https://app.camunda.com/jira/browse/CAM-7682    
     */
    String currentElementId = getElement().getAttributeValue("id");    
    String boundaryEventId = currentElementId + "-toBeCompensated";
    String compensationTaskId = currentElementId + "-compensation";

    ModelInstance modelInstance = getElement().getModelInstance();
    
    saga = ((AbstractActivityBuilder)saga).boundaryEvent(boundaryEventId)
      .compensateEventDefinition()
      .compensateEventDefinitionDone()
      .moveToActivity(currentElementId);
    
    BoundaryEvent boundaryEvent = modelInstance.getModelElementById(boundaryEventId);
    BaseElement scope = (BaseElement) boundaryEvent.getParentElement();

    ServiceTask compensationHandler = modelInstance.newInstance(ServiceTask.class);
    compensationHandler.setId(compensationTaskId);
    compensationHandler.setName(name);
    compensationHandler.setForCompensation(true);
    compensationHandler.setCamundaClass(adapterClass.getName());
    scope.addChildElement(compensationHandler);

    Association association = modelInstance.newInstance(Association.class);
    association.setAssociationDirection(AssociationDirection.One);
    association.setSource(boundaryEvent);
    association.setTarget(compensationHandler);
    scope.addChildElement(association);
    
    BpmnShape shape = saga.createBpmnShape(compensationHandler);
    setCoordinates(shape);
    resizeSubProcess(shape);
    createBpmnEdge(association);
    
    // cannot use this as not yet fixed in master - did a poc hack on a branch though:
    // https://github.com/camunda/camunda-bpmn-model/commit/d38346380126113c1cf3cd3ae4cb737600ee03fa
//    saga.createDiagramInterchange(compensationHandler);
//    saga.createBpmnEdge(association); 

    saga = saga.moveToNode(currentElementId);
    
    return this;
  }
  
  public SagaBuilder triggerCompensationOnAnyError() {    
    /*
     * This is a bit unhandy with the Camunda Model API at the moment.
     * It will be adresses with 
     *  https://app.camunda.com/jira/browse/CAM-7683  
     */
    ModelInstance modelInstance = getElement().getModelInstance();        
    BpmnModelElementInstance parent = (BpmnModelElementInstance) getElement().getParentElement(); // WHooha

    SubProcess eventSubProcess = modelInstance.newInstance(SubProcess.class);
    eventSubProcess.setId("TriggerCompensation");
    parent.addChildElement(eventSubProcess);

    BpmnShape shape = saga.createBpmnShape(eventSubProcess);
    setCoordinates(shape);
    resizeSubProcess(shape);

    eventSubProcess.builder().triggerByEvent().embeddedSubProcess()
      .startEvent("ErrorCatched").error("java.lang.Throwable")
      .intermediateThrowEvent("ToBeCompensated").compensateEventDefinition().compensateEventDefinitionDone()
      .endEvent("ErrorHandled")
      .subProcessDone();    

    return this;
  }
  
  /**
   * CODE FROM HERE ON IS COPIED CODE FROM {@link AbstractBaseElementBuilder}
   * AND {@link AbstractBpmnModelElementBuilder}.
   * 
   * They currently do not yet support Compensation & Associations and do not 
   * allow to define them externally. So copying it was the fastest way to get around.
   * 
   * Wait for these issues to be resolved - then implementation can be radically simplified.
   * 
   * https://app.camunda.com/jira/browse/CAM-7680
   * https://app.camunda.com/jira/browse/CAM-7681
   * 
   */
  
  private ModelElementInstance getElement() {
    // This will be possible in 7.7 - but it is not yet
    // return saga.getElement();
    try {
      Method method = saga.getClass().getMethod("getElement");
      method.setAccessible(true);
      ModelElementInstance result = (ModelElementInstance) method.invoke(saga);
      return result;
    } catch (Exception ex) {
      throw new RuntimeException("Could not access element of Fluent Builder: " + ex.getMessage(), ex);
    }
  }
  
  private ModelInstance modelInstance() {
    return getElement().getModelInstance();
  }

  private BaseElement element() {
    return (BaseElement) getElement();
  }

  protected BpmnPlane findBpmnPlane() {
    Collection<BpmnPlane> planes = modelInstance().getModelElementsByType(BpmnPlane.class);
    return planes.iterator().next();
  }
 
  protected BpmnShape findBpmnShape(BaseElement node) {
    Collection<BpmnShape> allShapes = modelInstance().getModelElementsByType(BpmnShape.class);

    Iterator<BpmnShape> iterator = allShapes.iterator();
    while (iterator.hasNext()) {
      BpmnShape shape = iterator.next();
      if (shape.getBpmnElement().equals(node)) {
        return shape;
      }
    }
    return null;
  }

  protected void resizeSubProcess(BpmnShape innerShape) {

    BaseElement innerElement = innerShape.getBpmnElement();
    Bounds innerShapeBounds = innerShape.getBounds();

    ModelElementInstance parent = innerElement.getParentElement();

    while (parent instanceof SubProcess) {

      BpmnShape subProcessShape = findBpmnShape((SubProcess) parent);

      if (subProcessShape != null) {

        Bounds subProcessBounds = subProcessShape.getBounds();
        double innerX = innerShapeBounds.getX();
        double innerWidth = innerShapeBounds.getWidth();
        double innerY = innerShapeBounds.getY();
        double innerHeight = innerShapeBounds.getHeight();

        double subProcessY = subProcessBounds.getY();
        double subProcessHeight = subProcessBounds.getHeight();
        double subProcessX = subProcessBounds.getX();
        double subProcessWidth = subProcessBounds.getWidth();

        double tmpWidth = innerX + innerWidth + SPACE;
        double tmpHeight = innerY + innerHeight + SPACE;

        if (innerY == subProcessY) {
          subProcessBounds.setY(subProcessY - SPACE);
          subProcessBounds.setHeight(subProcessHeight + SPACE);
        }

        if (tmpWidth >= subProcessX + subProcessWidth) {
          double newWidth = tmpWidth - subProcessX;
          subProcessBounds.setWidth(newWidth);
        }

        if (tmpHeight >= subProcessY + subProcessHeight) {
          double newHeight = tmpHeight - subProcessY;
          subProcessBounds.setHeight(newHeight);
        }

        innerElement = (SubProcess) parent;
        innerShapeBounds = subProcessBounds;
        parent = innerElement.getParentElement();
      }
      else {
        break;
      }
    }
  }
  
  protected void setCoordinates(BpmnShape shape) {
    BpmnShape source = findBpmnShape(element());
    Bounds shapeBounds = shape.getBounds();

    double x = 0;
    double y = 0;

    if (source != null) {
      Bounds sourceBounds = source.getBounds();

      double sourceX = sourceBounds.getX();
      double sourceWidth = sourceBounds.getWidth();
      x = sourceX + sourceWidth + SPACE;
      
      if (shape.getBpmnElement() instanceof Activity && ((Activity)shape.getBpmnElement()).isForCompensation()) {
        x = sourceX + sourceWidth;
        y = sourceBounds.getY() + sourceBounds.getHeight() + SPACE;
      } else if (element() instanceof FlowNode) {      

        FlowNode flowNode = (FlowNode) element();
        Collection<SequenceFlow> outgoing = flowNode.getOutgoing();

        if (outgoing.size() == 0) {
          double sourceY = sourceBounds.getY();
          double sourceHeight = sourceBounds.getHeight();
          double targetHeight = shapeBounds.getHeight();
          y = sourceY + sourceHeight / 2 - targetHeight / 2;
        }
        else {
          SequenceFlow[] sequenceFlows = outgoing.toArray(new SequenceFlow[outgoing.size()]);
          SequenceFlow last = sequenceFlows[outgoing.size() - 1];

          BpmnShape targetShape = findBpmnShape(last.getTarget());
          if (targetShape != null) {
            Bounds targetBounds = targetShape.getBounds();
            double lastY = targetBounds.getY();
            double lastHeight = targetBounds.getHeight();
            y = lastY + lastHeight + AbstractBaseElementBuilder.SPACE;
          }

        }
      }
    }

    shapeBounds.setX(x);
    shapeBounds.setY(y);
  }
  
  protected <T extends BpmnModelElementInstance> T createInstance(Class<T> typeClass) {
    return modelInstance().newInstance(typeClass);
  }

  public BpmnEdge createBpmnEdge(BaseElement sequenceFlow) {
    BpmnPlane bpmnPlane = findBpmnPlane();
    if (bpmnPlane != null) {


       BpmnEdge edge = createInstance(BpmnEdge.class);
       edge.setBpmnElement(sequenceFlow);
       setWaypoints(edge);

       bpmnPlane.addChildElement(edge);
       return edge;
    }
    return null;

  }

  protected void setWaypoints(BpmnEdge edge) {
    BaseElement flowSource = null;
    BaseElement flowTarget = null;
    
    if (edge.getBpmnElement() instanceof SequenceFlow) {
      SequenceFlow sequenceFlow = (SequenceFlow) edge.getBpmnElement();  
      flowSource = sequenceFlow.getSource();
      flowTarget = sequenceFlow.getTarget();
    } else if (edge.getBpmnElement() instanceof Association){
      Association association = (Association)edge.getBpmnElement();
      flowSource = association.getSource();
      flowTarget = association.getTarget();
    }
  
    BpmnShape source = findBpmnShape(flowSource);
    BpmnShape target = findBpmnShape(flowTarget);

    if (source != null && target != null) {

      Bounds sourceBounds = source.getBounds();
      Bounds targetBounds = target.getBounds();

      double sourceX = sourceBounds.getX();
      double sourceY = sourceBounds.getY();
      double sourceWidth = sourceBounds.getWidth();
      double sourceHeight = sourceBounds.getHeight();

      double targetX = targetBounds.getX();
      double targetY = targetBounds.getY();
      double targetHeight = targetBounds.getHeight();

      Waypoint w1 = createInstance(Waypoint.class);

      if (flowSource instanceof SequenceFlow && ((FlowNode) flowSource).getOutgoing().size() == 1) {
        w1.setX(sourceX + sourceWidth);
        w1.setY(sourceY + sourceHeight / 2);

        edge.addChildElement(w1);
      }
      else {
        w1.setX(sourceX + sourceWidth / 2);
        w1.setY(sourceY + sourceHeight);

        edge.addChildElement(w1);

        Waypoint w2 = createInstance(Waypoint.class);
        w2.setX(sourceX + sourceWidth / 2);
        w2.setY(targetY + targetHeight / 2);

        edge.addChildElement(w2);
      }

      Waypoint w3 = createInstance(Waypoint.class);
      w3.setX(targetX);
      w3.setY(targetY + targetHeight / 2);

      edge.addChildElement(w3);
    }
  }
}
