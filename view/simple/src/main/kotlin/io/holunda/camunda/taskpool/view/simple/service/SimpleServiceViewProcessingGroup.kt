package io.holunda.camunda.taskpool.view.simple.service

import io.holunda.camunda.taskpool.view.query.FilterQuery
import mu.KLogging
import org.axonframework.config.EventProcessingConfiguration
import org.axonframework.eventhandling.TrackingEventProcessor
import org.axonframework.queryhandling.QueryUpdateEmitter
import org.springframework.stereotype.Component

@Component
class SimpleServiceViewProcessingGroup(
  private val configuration: EventProcessingConfiguration
) {


  companion object : KLogging() {
    const val PROCESSING_GROUP = "io.holunda.camunda.taskpool.view.simple.service"
  }

  /**
   * Configure to run a event replay to fill the simple task view with events on start-up.
   */
  fun restore() {
    this.configuration
      .eventProcessorByProcessingGroup(PROCESSING_GROUP, TrackingEventProcessor::class.java)
      .ifPresent {
        TaskPoolService.logger.info { "VIEW-SIMPLE-002: Starting simple view event replay." }
        it.shutDown()
        it.resetTokens()
        it.start()
      }
  }

}

