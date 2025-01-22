package org.qubership.integration.platform.engine.service.deployment.processing.actions.context.stop;

import org.apache.camel.spring.SpringCamelContext;
import org.qubership.integration.platform.engine.model.deployment.update.DeploymentConfiguration;
import org.qubership.integration.platform.engine.model.deployment.update.DeploymentInfo;
import org.qubership.integration.platform.engine.service.SdsService;
import org.qubership.integration.platform.engine.service.deployment.processing.DeploymentProcessingAction;
import org.qubership.integration.platform.engine.service.deployment.processing.qualifiers.OnStopDeploymentContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(SdsService.class)
@OnStopDeploymentContext
public class SdsSchedulerRemoveJobsAction implements DeploymentProcessingAction {
    private final SdsService sdsService;

    @Autowired
    public SdsSchedulerRemoveJobsAction(SdsService sdsService) {
        this.sdsService = sdsService;
    }

    @Override
    public void execute(
        SpringCamelContext context, DeploymentInfo deploymentInfo,
            DeploymentConfiguration deploymentConfiguration) {
        sdsService.removeSchedulerJobs(deploymentInfo.getDeploymentId());
    }

}
