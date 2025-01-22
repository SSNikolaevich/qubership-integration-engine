package org.qubership.integration.platform.engine.service.deployment.processing;

import java.util.Optional;

import org.apache.camel.spring.SpringCamelContext;
import org.qubership.integration.platform.engine.model.constants.CamelConstants.ChainProperties;
import org.qubership.integration.platform.engine.model.deployment.update.DeploymentConfiguration;
import org.qubership.integration.platform.engine.model.deployment.update.DeploymentInfo;
import org.qubership.integration.platform.engine.model.deployment.update.ElementProperties;
import org.slf4j.MDC;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class ElementProcessingAction implements DeploymentProcessingAction {
    @Override
    public void execute(
        SpringCamelContext context,
        DeploymentInfo deploymentInfo,
        DeploymentConfiguration deploymentConfiguration
    ) {
        Optional.ofNullable(deploymentConfiguration)
            .map(DeploymentConfiguration::getProperties)
            .ifPresent(properties -> properties.stream()
                    .filter(this::applicableTo)
                    .forEach(elementProperties -> processElement(context, elementProperties, deploymentInfo)));
    }

    private void processElement(
        SpringCamelContext context,
        ElementProperties elementProperties,
        DeploymentInfo deploymentInfo
    ) {
        try {
            String elementId = elementProperties.getProperties().get(ChainProperties.ELEMENT_ID);
            log.debug("Applying action {} for deployment {}, element {}",
                this.getClass().getSimpleName(), deploymentInfo.getDeploymentId(), elementId);
            MDC.put(ChainProperties.ELEMENT_ID, elementId);
            apply(context, elementProperties, deploymentInfo);
        } finally {
            MDC.remove(ChainProperties.ELEMENT_ID);
        }
    } 

    public abstract boolean applicableTo(ElementProperties properties);
    public abstract void apply(
        SpringCamelContext context,
        ElementProperties properties,
        DeploymentInfo deploymentInfo
    );
}
