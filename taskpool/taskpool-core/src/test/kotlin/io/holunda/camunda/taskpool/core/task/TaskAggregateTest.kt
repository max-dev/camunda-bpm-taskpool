package io.holunda.camunda.taskpool.core.task

import io.holunda.camunda.taskpool.api.business.addCorrelation
import io.holunda.camunda.taskpool.api.business.newCorrelations
import io.holunda.camunda.taskpool.api.task.*
import org.axonframework.test.aggregate.AggregateTestFixture
import org.camunda.bpm.engine.variable.Variables
import org.camunda.bpm.engine.variable.Variables.stringValue
import org.junit.Before
import org.junit.Test
import java.util.*

class TaskAggregateTest {

  private val fixture: AggregateTestFixture<TaskAggregate> = AggregateTestFixture<TaskAggregate>(TaskAggregate::class.java)
  private lateinit var now: Date

  private val processReference = ProcessReference(
    definitionKey = "process_key",
    instanceId = "0815",
    executionId = "12345",
    definitionId = "76543",
    name = "My process",
    applicationName = "myExample"
  )

  @Before
  fun setUp() {
    now = Date()
  }

  @Test
  fun `should create task`() {
    fixture
      .givenNoPriorActivity()
      .`when`(
        CreateTaskCommand(
          id = "4711",
          name = "Foo",
          createTime = now,
          eventName = "create",
          owner = "kermit",
          taskDefinitionKey = "foo",
          formKey = "some",
          businessKey = "business123",
          enriched = true,
          sourceReference = processReference,
          candidateUsers = listOf("kermit", "gonzo"),
          candidateGroups = listOf("muppets"),
          assignee = "kermit",
          priority = 51,
          description = "Funky task",
          payload = Variables.createVariables().putValueTyped("key", stringValue("value")),
          correlations = newCorrelations().addCorrelation("Request", "business123")
        ))
      .expectEvents(
        TaskCreatedEngineEvent(
          id = "4711",
          name = "Foo",
          createTime = now,
          owner = "kermit",
          taskDefinitionKey = "foo",
          formKey = "some",
          businessKey = "business123",
          sourceReference = processReference,
          candidateUsers = listOf("kermit", "gonzo"),
          candidateGroups = listOf("muppets"),
          assignee = "kermit",
          priority = 51,
          description = "Funky task",
          payload = Variables.createVariables().putValueTyped("key", stringValue("value")),
          correlations = newCorrelations().addCorrelation("Request", "business123")
        ))
  }

  @Test
  fun `should complete task`() {
    fixture
      .given(
        TaskCreatedEngineEvent(
          id = "4711",
          name = "Foo",
          createTime = now,
          owner = "kermit",
          taskDefinitionKey = "foo",
          formKey = "some",
          businessKey = "business123",
          sourceReference = processReference,
          candidateUsers = listOf("kermit", "gonzo"),
          candidateGroups = listOf("muppets"),
          assignee = "kermit",
          priority = 51,
          description = "Funky task",
          payload = Variables.createVariables().putValueTyped("key", stringValue("value")),
          correlations = newCorrelations().addCorrelation("Request", "business123")
        ))
      .`when`(
        CompleteTaskCommand(
          id = "4711",
          name = "Foo",
          createTime = now,
          eventName = "create",
          owner = "kermit",
          taskDefinitionKey = "foo",
          formKey = "some",
          businessKey = "business123",
          enriched = true,
          sourceReference = processReference,
          dueDate = null,
          candidateUsers = listOf("kermit", "gonzo"),
          candidateGroups = listOf("muppets"),
          assignee = "kermit",
          priority = 51,
          description = "Funky task",
          payload = Variables.createVariables().putValueTyped("another", stringValue("some")),
          correlations = newCorrelations().addCorrelation("Request", "business456")
        ))
      .expectEvents(
        TaskCompletedEngineEvent(
          id = "4711",
          name = "Foo",
          createTime = now,
          owner = "kermit",
          taskDefinitionKey = "foo",
          formKey = "some",
          businessKey = "business123",
          enriched = true,
          sourceReference = processReference,
          dueDate = null,
          candidateUsers = listOf("kermit", "gonzo"),
          candidateGroups = listOf("muppets"),
          assignee = "kermit",
          priority = 51,
          description = "Funky task",
          payload = Variables.createVariables().putValueTyped("another", stringValue("some")),
          correlations = newCorrelations().addCorrelation("Request", "business456")
        )
      )
  }

  @Test
  fun `should assign task`() {
    fixture
      .given(
        TaskCreatedEngineEvent(
          id = "4711",
          name = "Foo",
          taskDefinitionKey = "foo",
          formKey = "some",
          sourceReference = processReference
        ))
      .`when`(
        AssignTaskCommand(
          id = "4711",
          name = "Foo",
          createTime = now,
          eventName = "create",
          owner = "kermit",
          taskDefinitionKey = "foo",
          formKey = "some",
          businessKey = "business123",
          enriched = true,
          sourceReference = processReference,
          dueDate = null,
          candidateUsers = listOf("kermit", "gonzo"),
          candidateGroups = listOf("muppets"),
          assignee = "kermit",
          priority = 51,
          description = "Funky task",
          payload = Variables.createVariables().putValueTyped("another", stringValue("some")),
          correlations = newCorrelations().addCorrelation("Request", "business456")
        ))
      .expectEvents(
        TaskAssignedEngineEvent(
          id = "4711",
          name = "Foo",
          createTime = now,
          owner = "kermit",
          taskDefinitionKey = "foo",
          formKey = "some",
          businessKey = "business123",
          enriched = true,
          sourceReference = processReference,
          dueDate = null,
          candidateUsers = listOf("kermit", "gonzo"),
          candidateGroups = listOf("muppets"),
          assignee = "kermit",
          priority = 51,
          description = "Funky task",
          payload = Variables.createVariables().putValueTyped("another", stringValue("some")),
          correlations = newCorrelations().addCorrelation("Request", "business456")
        )
      )
  }


  @Test
  fun `should not re-assign task`() {
    fixture
      .given(
        TaskCreatedEngineEvent(
          id = "4711",
          name = "Foo",
          taskDefinitionKey = "foo",
          formKey = "some",
          sourceReference = processReference
        ),
        TaskAssignedEngineEvent(
          id = "4711",
          name = "Foo",
          taskDefinitionKey = "foo",
          formKey = "some",
          assignee = "kermit",
          sourceReference = processReference
        )
      )
      .`when`(
        AssignTaskCommand(
          id = "4711",
          name = "Foo",
          createTime = now,
          taskDefinitionKey = "foo",
          formKey = "some",
          assignee = "kermit",
          sourceReference = processReference
        ))
      .expectNoEvents()
  }


  @Test
  fun `should not complete deleted task`() {
    fixture
      .given(
        TaskCreatedEngineEvent(
          id = "4711",
          name = "Foo",
          createTime = now,
          owner = "kermit",
          taskDefinitionKey = "foo",
          formKey = "some",
          businessKey = "business123",
          sourceReference = processReference,
          candidateUsers = listOf("kermit", "gonzo"),
          candidateGroups = listOf("muppets"),
          assignee = "kermit",
          priority = 51,
          description = "Funky task",
          payload = Variables.createVariables().putValueTyped("key", stringValue("value")),
          correlations = newCorrelations().addCorrelation("Request", "business123")
        ),
        TaskDeletedEngineEvent(
          id = "4711",
          name = "Foo",
          createTime = now,
          owner = "kermit",
          taskDefinitionKey = "foo",
          formKey = "some",
          businessKey = "business123",
          enriched = true,
          sourceReference = processReference,
          dueDate = null,
          candidateUsers = listOf("kermit", "gonzo"),
          candidateGroups = listOf("muppets"),
          assignee = "kermit",
          priority = 51,
          description = "Funky task",
          payload = Variables.createVariables().putValueTyped("another", stringValue("some1")),
          correlations = newCorrelations().addCorrelation("Request", "business789"),
          deleteReason = "Deleted, because not needed"
        ))
      .`when`(
        CompleteTaskCommand(
          id = "4711",
          name = "Foo",
          createTime = now,
          eventName = "create",
          owner = "kermit",
          taskDefinitionKey = "foo",
          formKey = "some",
          businessKey = "business123",
          enriched = true,
          sourceReference = processReference,
          dueDate = null,
          candidateUsers = listOf("kermit", "gonzo"),
          candidateGroups = listOf("muppets"),
          assignee = "kermit",
          priority = 51,
          description = "Funky task",
          payload = Variables.createVariables().putValueTyped("another", stringValue("some")),
          correlations = newCorrelations().addCorrelation("Request", "business456")
        )
      ).expectNoEvents()
  }

  @Test
  fun `should not delete deleted task`() {
    fixture
      .given(
        TaskCreatedEngineEvent(
          id = "4711",
          name = "Foo",
          createTime = now,
          owner = "kermit",
          taskDefinitionKey = "foo",
          formKey = "some",
          businessKey = "business123",
          sourceReference = processReference,
          candidateUsers = listOf("kermit", "gonzo"),
          candidateGroups = listOf("muppets"),
          assignee = "kermit",
          priority = 51,
          description = "Funky task",
          payload = Variables.createVariables().putValueTyped("key", stringValue("value")),
          correlations = newCorrelations().addCorrelation("Request", "business123")
        ),
        TaskDeletedEngineEvent(
          id = "4711",
          name = "Foo",
          createTime = now,
          owner = "kermit",
          taskDefinitionKey = "foo",
          formKey = "some",
          businessKey = "business123",
          enriched = true,
          sourceReference = processReference,
          dueDate = null,
          candidateUsers = listOf("kermit", "gonzo"),
          candidateGroups = listOf("muppets"),
          assignee = "kermit",
          priority = 51,
          description = "Funky task",
          payload = Variables.createVariables().putValueTyped("another", stringValue("some1")),
          correlations = newCorrelations().addCorrelation("Request", "business789"),
          deleteReason = "Deleted, because not needed"
        ))
      .`when`(
        DeleteTaskCommand(
          id = "4711",
          taskDefinitionKey = "foo",
          deleteReason = "Not possible",
          sourceReference = processReference
        )
      ).expectNoEvents()
  }

  @Test
  fun `should delete task`() {
    fixture
      .given(
        TaskCreatedEngineEvent(
          id = "4711",
          name = "Foo",
          createTime = now,
          owner = "kermit",
          taskDefinitionKey = "foo",
          formKey = "some",
          businessKey = "business123",
          sourceReference = processReference,
          candidateUsers = listOf("kermit", "gonzo"),
          candidateGroups = listOf("muppets"),
          assignee = "kermit",
          priority = 51,
          description = "Funky task",
          payload = Variables.createVariables().putValueTyped("key", stringValue("value")),
          correlations = newCorrelations().addCorrelation("Request", "business123")
        ))
      .`when`(
        DeleteTaskCommand(
          id = "4711",
          name = "Foo",
          createTime = now,
          eventName = "create",
          owner = "kermit",
          taskDefinitionKey = "foo",
          formKey = "some",
          businessKey = "business123",
          enriched = true,
          sourceReference = processReference,
          dueDate = null,
          candidateUsers = listOf("kermit", "gonzo"),
          candidateGroups = listOf("muppets"),
          assignee = "kermit",
          priority = 51,
          description = "Funky task",
          payload = Variables.createVariables().putValueTyped("another", stringValue("some1")),
          correlations = newCorrelations().addCorrelation("Request", "business789"),
          deleteReason = "Deleted, because not needed"
        ))
      .expectEvents(
        TaskDeletedEngineEvent(
          id = "4711",
          name = "Foo",
          createTime = now,
          owner = "kermit",
          taskDefinitionKey = "foo",
          formKey = "some",
          businessKey = "business123",
          enriched = true,
          sourceReference = processReference,
          dueDate = null,
          candidateUsers = listOf("kermit", "gonzo"),
          candidateGroups = listOf("muppets"),
          assignee = "kermit",
          priority = 51,
          description = "Funky task",
          payload = Variables.createVariables().putValueTyped("another", stringValue("some1")),
          correlations = newCorrelations().addCorrelation("Request", "business789"),
          deleteReason = "Deleted, because not needed"
        )
      )
  }

  @Test
  fun `should claim unassigned task`() {
    fixture
      .given(
        TaskCreatedEngineEvent(
          id = "4711",
          name = "Foo",
          createTime = now,
          owner = "kermit",
          taskDefinitionKey = "foo",
          formKey = "some",
          businessKey = "business123",
          sourceReference = processReference,
          candidateUsers = listOf("kermit", "gonzo"),
          candidateGroups = listOf("muppets"),
          assignee = null,
          priority = 51,
          description = "Funky task",
          payload = Variables.createVariables().putValueTyped("key", stringValue("value")),
          correlations = newCorrelations().addCorrelation("Request", "business123")
        ))
      .`when`(
        ClaimInteractionTaskCommand(
          id = "4711",
          sourceReference = processReference,
          taskDefinitionKey = "foo",
          assignee = "piggy"
        )
      ).expectEvents(
        TaskClaimedEvent(
          id = "4711",
          sourceReference = processReference,
          taskDefinitionKey = "foo",
          assignee = "piggy"
        )
      )
  }

  @Test
  fun `should claim assigned task`() {
    fixture
      .given(
        TaskCreatedEngineEvent(
          id = "4711",
          name = "Foo",
          createTime = now,
          owner = "kermit",
          taskDefinitionKey = "foo",
          formKey = "some",
          businessKey = "business123",
          sourceReference = processReference,
          candidateUsers = listOf("kermit", "gonzo"),
          candidateGroups = listOf("muppets"),
          assignee = "kermit",
          priority = 51,
          description = "Funky task",
          payload = Variables.createVariables().putValueTyped("key", stringValue("value")),
          correlations = newCorrelations().addCorrelation("Request", "business123")
        ))
      .`when`(
        ClaimInteractionTaskCommand(
          id = "4711",
          sourceReference = processReference,
          taskDefinitionKey = "foo",
          assignee = "piggy"
        )
      ).expectEvents(
        TaskUnclaimedEvent(
          id = "4711",
          sourceReference = processReference,
          taskDefinitionKey = "foo"
        ),
        TaskClaimedEvent(
          id = "4711",
          sourceReference = processReference,
          taskDefinitionKey = "foo",
          assignee = "piggy"
        )
      )
  }

  @Test
  fun `should unclaim assigned task`() {
    fixture
      .given(
        TaskCreatedEngineEvent(
          id = "4711",
          name = "Foo",
          createTime = now,
          owner = "kermit",
          taskDefinitionKey = "foo",
          formKey = "some",
          businessKey = "business123",
          sourceReference = processReference,
          candidateUsers = listOf("kermit", "gonzo"),
          candidateGroups = listOf("muppets"),
          assignee = "kermit",
          priority = 51,
          description = "Funky task",
          payload = Variables.createVariables().putValueTyped("key", stringValue("value")),
          correlations = newCorrelations().addCorrelation("Request", "business123")
        ))
      .`when`(
        UnclaimInteractionTaskCommand(
          id = "4711",
          sourceReference = processReference,
          taskDefinitionKey = "foo"
        )
      ).expectEvents(
        TaskUnclaimedEvent(
          id = "4711",
          sourceReference = processReference,
          taskDefinitionKey = "foo"
        )
      )
  }

  @Test
  fun `should unclaim unassigned task`() {
    fixture
      .given(
        TaskCreatedEngineEvent(
          id = "4711",
          name = "Foo",
          createTime = now,
          owner = "kermit",
          taskDefinitionKey = "foo",
          formKey = "some",
          businessKey = "business123",
          sourceReference = processReference,
          candidateUsers = listOf("kermit", "gonzo"),
          candidateGroups = listOf("muppets"),
          assignee = null,
          priority = 51,
          description = "Funky task",
          payload = Variables.createVariables().putValueTyped("key", stringValue("value")),
          correlations = newCorrelations().addCorrelation("Request", "business123")
        ))
      .`when`(
        UnclaimInteractionTaskCommand(
          id = "4711",
          sourceReference = processReference,
          taskDefinitionKey = "foo"
        )
      ).expectNoEvents()
  }

  @Test
  fun `should mark task to be completed`() {

    val completionPayload = Variables.createVariables().putValueTyped("user-input", stringValue("whatever"));

    fixture
      .given(
        TaskCreatedEngineEvent(
          id = "4711",
          name = "Foo",
          createTime = now,
          owner = "kermit",
          taskDefinitionKey = "foo",
          formKey = "some",
          businessKey = "business123",
          sourceReference = processReference,
          assignee = "kermit",
          candidateUsers = listOf("kermit", "gonzo"),
          candidateGroups = listOf("muppets"),
          priority = 51,
          description = "Funky task",
          payload = Variables.createVariables().putValueTyped("key", stringValue("value")),
          correlations = newCorrelations().addCorrelation("Request", "business123")
        ))
      .`when`(
        CompleteInteractionTaskCommand(
          id = "4711",
          sourceReference = processReference,
          taskDefinitionKey = "foo",
          payload = completionPayload,
          assignee = "gonzo"
        )
      ).expectEvents(
        TaskUnclaimedEvent(
          id = "4711",
          sourceReference = processReference,
          taskDefinitionKey = "foo"
        ),
        TaskClaimedEvent(
          id = "4711",
          sourceReference = processReference,
          taskDefinitionKey = "foo",
          assignee = "gonzo"
        ),
        TaskToBeCompletedEvent(
          id = "4711",
          sourceReference = processReference,
          taskDefinitionKey = "foo",
          payload = completionPayload
        )
      )
  }

}
