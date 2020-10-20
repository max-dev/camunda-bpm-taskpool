package io.holunda.camunda.taskpool.view.mongo.service

import com.mongodb.MongoCommandException
import com.mongodb.client.model.changestream.OperationType
import io.holunda.camunda.taskpool.api.business.dataIdentityString
import io.holunda.camunda.taskpool.view.Task
import io.holunda.camunda.taskpool.view.TaskWithDataEntries
import io.holunda.camunda.taskpool.view.mongo.repository.*
import io.holunda.camunda.taskpool.view.query.task.ApplicationWithTaskCount
import mu.KLogging
import org.bson.BsonValue
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.SignalType
import reactor.util.retry.Retry
import java.time.Duration
import java.util.logging.Level
import javax.annotation.PreDestroy

/**
 * Observes the change stream on the mongo db and provides `Flux`es of changes for the various result types of queries. Also makes sure that tasks marked as
 * deleted are 'really' deleted shortly after.
 * Only active if `camunda.taskpool.view.mongo.changeTrackingMode` is set to `CHANGE_STREAM`.
 */
@Component
@ConditionalOnProperty(prefix = "camunda.taskpool.view.mongo", name = ["changeTrackingMode"], havingValue = "CHANGE_STREAM", matchIfMissing = false)
class TaskChangeTracker(
  private val taskRepository: TaskRepository,
  private val dataEntryRepository: DataEntryRepository
) {
  companion object : KLogging()

  private var lastSeenResumeToken: BsonValue? = null

  private val changeStream: Flux<TaskDocument> = Flux.defer { taskRepository.getTaskUpdates(lastSeenResumeToken) }
    // When there are no more subscribers to the change stream, the flux is cancelled. When a new subscriber appears, they should not get any past updates.
    // This shouldn't happen at all because the `trulyDeleteChangeStream` subscription should always stay active, but we keep it as a last resort.
    .doOnCancel { lastSeenResumeToken = null }
    // Remember the last seen resume token if one is present
    .doOnNext { event -> lastSeenResumeToken = event.resumeToken ?: lastSeenResumeToken }
    // When the resume token is out of date, Mongo will throw an error 'resume of change stream was not possible, as the resume token was not found.'
    // Unfortunately, there is no way to identify exactly this error because error codes and messages vary by Mongo server version.
    // The closest we can get is reacting on any MongoCommandException and resetting the token so that upon the next retry, we start without a token.
    .doOnError(MongoCommandException::class.java) { lastSeenResumeToken = null }
    .filter { event ->
      when (event.operationType) {
        OperationType.INSERT, OperationType.UPDATE, OperationType.REPLACE -> {
          logger.debug { "Got ${event.operationType?.value} event: $event" }
          true
        }
        else -> {
          logger.trace { "Ignoring ${event.operationType?.value} event: $event" }
          false
        }
      }
    }
    .log(TaskChangeTracker::class.qualifiedName, Level.WARNING, SignalType.ON_ERROR)
    .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofMillis(100)).maxBackoff(Duration.ofSeconds(10)))
    .concatMap { event -> Mono.justOrEmpty(event.body) }
    .share()

  // Truly delete documents that have been marked deleted
  private val trulyDeleteChangeStreamSubscription: Disposable = changeStream
    .filter { it.deleted }
    .flatMap( { task ->
      taskRepository.deleteById(task.id)
        .doOnSuccess { logger.trace { "Deleted task ${task.id} from database." } }
        .doOnError { e -> logger.debug(e) { "Deleting task ${task.id} from database failed." } }
        .retryWhen(Retry.backoff(5, Duration.ofMillis(50)))
        .doOnError { e -> logger.warn(e) { "Deleting task ${task.id} from database failed and retries are exhausted." } }
        .onErrorResume { Mono.empty() }
    }, 10 )
    .subscribe()

  /**
   * Clear subscription.
   */
  @PreDestroy
  fun clearSubscription() {
    trulyDeleteChangeStreamSubscription.dispose()
  }

  /**
   * Adopt changes to task count by application stream.
   */
  fun trackTaskCountsByApplication(): Flux<ApplicationWithTaskCount> = changeStream
    .window(Duration.ofSeconds(1))
    .concatMap {
      it.reduce(setOf<String>()) { applicationNames, task ->
        applicationNames + task.sourceReference.applicationName
      }
    }
    .concatMap { Flux.fromIterable(it) }
    .concatMap { taskRepository.findTaskCountForApplication(it) }

  /**
   * Adopt changes to task update stream.
   */
  fun trackTaskUpdates(): Flux<Task> = changeStream
    .map { it.task() }

  /**
   * Adopt changes to task with data entries update stream.
   */
  fun trackTaskWithDataEntriesUpdates(): Flux<TaskWithDataEntries> = changeStream
    .concatMap { taskDocument ->
      val task = taskDocument.task()
      this.dataEntryRepository.findAllById(task.correlations.map { dataIdentityString(entryType = it.key, entryId = it.value.toString()) })
        .map { it.dataEntry() }
        .collectList()
        .map { TaskWithDataEntries(task = task, dataEntries = it) }
    }
}
