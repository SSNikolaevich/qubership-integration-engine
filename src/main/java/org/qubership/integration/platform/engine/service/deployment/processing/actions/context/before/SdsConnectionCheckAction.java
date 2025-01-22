package org.qubership.integration.platform.engine.service.deployment.processing.actions.context.before;

import org.apache.camel.spring.SpringCamelContext;
import org.qubership.integration.platform.engine.errorhandling.DeploymentRetriableException;
import org.qubership.integration.platform.engine.model.ChainElementType;
import org.qubership.integration.platform.engine.model.constants.CamelConstants.ChainProperties;
import org.qubership.integration.platform.engine.model.deployment.update.DeploymentInfo;
import org.qubership.integration.platform.engine.model.deployment.update.ElementProperties;
import org.qubership.integration.platform.engine.service.SdsService;
import org.qubership.integration.platform.engine.service.deployment.processing.ElementProcessingAction;
import org.qubership.integration.platform.engine.service.deployment.processing.qualifiers.OnBeforeDeploymentContextCreated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnBean(SdsService.class)
@OnBeforeDeploymentContextCreated
public class SdsConnectionCheckAction extends ElementProcessingAction {
    private final SdsService sdsService;

    @Autowired
    public SdsConnectionCheckAction(SdsService sdsService) {
        this.sdsService = sdsService;
    }
    
    @Override
    public boolean applicableTo(ElementProperties properties) {
        ChainElementType chainElementType = ChainElementType.fromString(
                properties.getProperties().get(ChainProperties.ELEMENT_TYPE));
        return ChainElementType.isSdsTriggerElement(chainElementType);
    }

    @Override
    public void apply(
        SpringCamelContext context,
        ElementProperties properties,
        DeploymentInfo deploymentInfo
    ) {
        try {
            sdsService.getJobsMetadata();
        } catch (Exception exception) {
            log.warn("Sds trigger predeploy check failed. Please check scheduling-service");
            throw new DeploymentRetriableException(
                    "Sds trigger predeploy check failed. Please check scheduling-service",
                    exception);
        }
    }
}
