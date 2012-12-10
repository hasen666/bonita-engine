package com.bonitasoft.engine.event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.bonitasoft.engine.CommonAPITest;
import org.bonitasoft.engine.bpm.model.ManualTaskInstance;
import org.bonitasoft.engine.bpm.model.ProcessDefinition;
import org.bonitasoft.engine.bpm.model.ProcessDefinitionBuilder;
import org.bonitasoft.engine.bpm.model.ProcessInstance;
import org.bonitasoft.engine.bpm.model.TaskPriority;
import org.bonitasoft.engine.bpm.model.UserTaskDefinitionBuilder;
import org.bonitasoft.engine.bpm.model.archive.ArchivedActivityInstance;
import org.bonitasoft.engine.bpm.model.event.trigger.TimerType;
import org.bonitasoft.engine.exception.BonitaException;
import org.bonitasoft.engine.exception.InvalidProcessDefinitionException;
import org.bonitasoft.engine.expression.Expression;
import org.bonitasoft.engine.expression.ExpressionBuilder;
import org.bonitasoft.engine.identity.User;
import org.bonitasoft.engine.process.TestStates;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("Not yet implemented")
public class TimerBoundaryEventTest extends CommonAPITest {

    private User donaBenta;

    @Before
    public void beforeTest() throws BonitaException {
        login();
        donaBenta = createUser("donabenta", "bpm");
        logout();
        loginWith("donabenta", "bpm");
    }

    @After
    public void afterTest() throws BonitaException {
        logout();
        login();
        deleteUser(donaBenta.getId());
        logout();
    }

    private ProcessDefinition deployProcessWithBoundaryEvent(final long timerValue) throws BonitaException, InvalidProcessDefinitionException {
        final Expression timerExpr = new ExpressionBuilder().createConstantLongExpression(timerValue);
        final ProcessDefinitionBuilder processDefinitionBuilder = new ProcessDefinitionBuilder().createNewInstance("pTimerBoundary", "2.0");
        final String actorName = "delivery";
        processDefinitionBuilder.addActor(actorName);
        processDefinitionBuilder.addStartEvent("start");
        final UserTaskDefinitionBuilder userTaskDefinitionBuilder = processDefinitionBuilder.addUserTask("step1", actorName);
        userTaskDefinitionBuilder.addBoundaryEvent("timer").addTimerEventTriggerDefinition(TimerType.DURATION, timerExpr);
        userTaskDefinitionBuilder.addUserTask("exceptionStep", actorName);
        userTaskDefinitionBuilder.addUserTask("step2", actorName);
        processDefinitionBuilder.addEndEvent("end");
        processDefinitionBuilder.addTransition("start", "step1");
        processDefinitionBuilder.addTransition("step1", "step2");
        processDefinitionBuilder.addTransition("timer", "exceptionStep");

        final ProcessDefinition processDefinition = deployAndEnableWithActor(processDefinitionBuilder.done(), actorName, donaBenta);
        return processDefinition;
    }

    @Test
    public void testTimerBoundaryEventTriggered() throws Exception {
        executeProcessWithTimerBoundary(false);
    }

    private void executeProcessWithTimerBoundary(final boolean addChild) throws Exception {
        final long timerDuration = 1000;
        final ProcessDefinition processDefinition = deployProcessWithBoundaryEvent(timerDuration);

        final ProcessInstance processInstance = getProcessAPI().startProcess(processDefinition.getId());
        final WaitForStep waitForStep1 = new WaitForStep(50, 1000, "step1", processInstance.getId());
        assertTrue(waitForStep1.waitUntil());
        ManualTaskInstance manualUserTask = null;
        if (addChild) {
            getProcessAPI().assignUserTask(waitForStep1.getStepId(), donaBenta.getId());
            manualUserTask = getProcessAPI().addManualUserTask(waitForStep1.getStepId(), "childOfStep1", "childOfStep1", donaBenta.getId(), "child task",
                    new Date(), TaskPriority.NORMAL);
        }

        Thread.sleep(timerDuration); // wait timer trigger

        final WaitForStep waitForExceptionStep = new WaitForStep(50, 500, "exceptionStep", processInstance.getId(), TestStates.getReadyState(null));
        assertTrue(waitForExceptionStep.waitUntil());
        ArchivedActivityInstance archActivityInst = getProcessAPI().getArchivedActivityInstance(waitForStep1.getStepId());
        assertEquals(TestStates.getAbortedState(), archActivityInst.getState());

        if (addChild) {
            archActivityInst = getProcessAPI().getArchivedActivityInstance(manualUserTask.getId());
            assertEquals(TestStates.getAbortedState(), archActivityInst.getState());
        }

        assignAndExecuteStep(waitForExceptionStep.getResult(), donaBenta.getId());
        assertTrue(isProcessInstanceFinishedAndArchived(50, 1000, processInstance, getProcessAPI()));

        disableAndDelete(processDefinition);
    }

    @Test
    public void testTimerBoundaryEventTriggeredSubtasks() throws Exception {
        executeProcessWithTimerBoundary(true);
    }

}
