package io.holunda.camunda.taskpool.collector.process.definition

import io.holunda.camunda.taskpool.TaskCollectorProperties
import org.camunda.bpm.engine.spring.SpringProcessEngineConfiguration
import org.camunda.bpm.spring.boot.starter.util.SpringBootProcessEnginePlugin
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ProcessDefinitionEnginePlugin(
  private val properties: TaskCollectorProperties
) : SpringBootProcessEnginePlugin() {

  private val logger: Logger = LoggerFactory.getLogger(ProcessDefinitionEnginePlugin::class.java)

  override fun preInit(springConfiguration: SpringProcessEngineConfiguration) {
    if (properties.processDefinition.enabled) {
      logger.info("EVENTING-010: Process definition registration plugin activated.")
      springConfiguration.customPostBPMNParseListeners.add(
        RegisterProcessDefinitionParseListener(springConfiguration)
      )
    } else {
      logger.info("EVENTING-011: Process definition registration disabled by property.")
    }
  }
}
