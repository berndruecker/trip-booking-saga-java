package io.flowing.trip.saga.camunda.springboot.conf;



import org.camunda.bpm.engine.ProcessEngineConfiguration;
import org.camunda.bpm.engine.spring.SpringProcessEngineConfiguration;
import org.camunda.bpm.spring.boot.starter.configuration.CamundaHistoryLevelAutoHandlingConfiguration;
import org.camunda.bpm.spring.boot.starter.configuration.impl.AbstractCamundaConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CamundaEngineHistoryConfiguration extends AbstractCamundaConfiguration implements CamundaHistoryLevelAutoHandlingConfiguration {

  @Override
  public void preInit(SpringProcessEngineConfiguration configuration) {
    configuration.setHistory(ProcessEngineConfiguration.HISTORY_FULL);
  }
}
