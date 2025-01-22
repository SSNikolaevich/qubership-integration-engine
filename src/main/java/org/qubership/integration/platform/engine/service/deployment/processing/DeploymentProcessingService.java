package org.qubership.integration.platform.engine.service.deployment.processing;

import java.util.Collection;

import org.apache.camel.spring.SpringCamelContext;
import org.qubership.integration.platform.engine.model.deployment.update.DeploymentConfiguration;
import org.qubership.integration.platform.engine.model.deployment.update.DeploymentInfo;
import org.qubership.integration.platform.engine.service.deployment.processing.qualifiers.OnBeforeDeploymentContextCreated;
import org.qubership.integration.platform.engine.service.deployment.processing.qualifiers.OnAfterDeploymentContextCreated;
import org.qubership.integration.platform.engine.service.deployment.processing.qualifiers.OnStopDeploymentContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DeploymentProcessingService {
    private final Collection<DeploymentProcessingAction> beforeContextCreatedActions;
    private final Collection<DeploymentProcessingAction> afterContextCreatedActions;
    private final Collection<DeploymentProcessingAction> stopContextActions;

    @Autowired
    public DeploymentProcessingService(
        @OnBeforeDeploymentContextCreated Collection<DeploymentProcessingAction> beforeContextCreatedActions,
        @OnAfterDeploymentContextCreated Collection<DeploymentProcessingAction> afterContextCreatedActions,
        @OnStopDeploymentContext Collection<DeploymentProcessingAction> stopContextActions
    ) {
        this.beforeContextCreatedActions = beforeContextCreatedActions;
        this.afterContextCreatedActions = afterContextCreatedActions;
        this.stopContextActions = stopContextActions;
    }

    public void processBeforeContextCreated(
        DeploymentInfo deploymentInfo,
        DeploymentConfiguration deploymentConfiguration
    ) {
        log.debug("Applying deployment processing actions before context created for deployment {}",
            deploymentInfo.getDeploymentId());
        executeActions(beforeContextCreatedActions, null, deploymentInfo, deploymentConfiguration);
    }

    public void processAfterContextCreated(
        SpringCamelContext context,
        DeploymentInfo deploymentInfo,
        DeploymentConfiguration deploymentConfiguration
    ) {
        log.debug("Applying deployment processing actions after context created for deployment {}",
            deploymentInfo.getDeploymentId());
        executeActions(afterContextCreatedActions, context, deploymentInfo, deploymentConfiguration);
    }

    public void processStopContext(
        SpringCamelContext context,
        DeploymentInfo deploymentInfo,
        DeploymentConfiguration deploymentConfiguration
    ) {
        log.debug("Applying deployment processing actions on context stop for deployment {}",
            deploymentInfo.getDeploymentId());
        executeActions(stopContextActions, context, deploymentInfo, deploymentConfiguration);
    }

    private void executeActions(
        Collection<DeploymentProcessingAction> actions,
        SpringCamelContext context,
        DeploymentInfo deploymentInfo,
        DeploymentConfiguration deploymentConfiguration
    ) {
        actions.forEach(action -> executeAction(action, context, deploymentInfo, deploymentConfiguration));
    }

    private void executeAction(
        DeploymentProcessingAction action,
        SpringCamelContext context,
        DeploymentInfo deploymentInfo,
        DeploymentConfiguration deploymentConfiguration
    ) {
        log.debug("Applying deployment processing action {} for deployment {}",
                action.getClass().getSimpleName(), deploymentInfo.getDeploymentId());
        action.execute(context, deploymentInfo, deploymentConfiguration);
    }
}
