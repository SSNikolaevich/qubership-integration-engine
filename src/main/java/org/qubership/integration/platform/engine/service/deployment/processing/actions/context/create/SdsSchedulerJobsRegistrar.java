package org.qubership.integration.platform.engine.service.deployment.processing.actions.context.create;

import java.util.List;
import java.util.Map;

import org.apache.camel.spring.SpringCamelContext;
import org.qubership.integration.platform.engine.model.ChainElementType;
import org.qubership.integration.platform.engine.model.constants.CamelConstants.ChainProperties;
import org.qubership.integration.platform.engine.model.deployment.update.DeploymentConfiguration;
import org.qubership.integration.platform.engine.model.deployment.update.DeploymentInfo;
import org.qubership.integration.platform.engine.model.deployment.update.ElementProperties;
import org.qubership.integration.platform.engine.service.SdsService;
import org.qubership.integration.platform.engine.service.deployment.processing.DeploymentProcessingAction;
import org.qubership.integration.platform.engine.service.deployment.processing.qualifiers.OnAfterDeploymentContextCreated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(SdsService.class)
@OnAfterDeploymentContextCreated
public class SdsSchedulerJobsRegistrar implements DeploymentProcessingAction {
    private final SdsService sdsService;

    @Autowired
    public SdsSchedulerJobsRegistrar(SdsService sdsService) {
        this.sdsService = sdsService;
    }

    @Override
    public void execute(
        SpringCamelContext context,
        DeploymentInfo deploymentInfo,
        DeploymentConfiguration deploymentConfiguration
    ) {
        List<Map<String, String>> sdsElementsProperties = deploymentConfiguration.getProperties()
            .stream()
            .filter(SdsSchedulerJobsRegistrar::isSdsTrigger)
            .map(ElementProperties::getProperties)
            .toList();
        sdsService.registerSchedulerJobs(context, deploymentInfo, sdsElementsProperties);    
    }

    private static boolean isSdsTrigger(ElementProperties elementProperties) {
        Map<String, String> properties = elementProperties.getProperties();
        ChainElementType elementType = ChainElementType.fromString(properties.get(ChainProperties.ELEMENT_TYPE));
        return ChainElementType.isSdsTriggerElement(elementType);
    }
}
