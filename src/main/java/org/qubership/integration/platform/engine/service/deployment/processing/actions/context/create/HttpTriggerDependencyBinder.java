package org.qubership.integration.platform.engine.service.deployment.processing.actions.context.create;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.qubership.integration.platform.engine.model.deployment.update.DeploymentInfo;
import org.qubership.integration.platform.engine.model.deployment.update.ElementProperties;
import org.qubership.integration.platform.engine.service.deployment.processing.actions.context.create.helpers.MetricTagsHelper;
import org.qubership.integration.platform.engine.service.deployment.processing.ElementProcessingAction;
import org.qubership.integration.platform.engine.service.deployment.processing.qualifiers.OnAfterDeploymentContextCreated;
import org.qubership.integration.platform.engine.camel.components.servlet.ServletTagsProvider;
import org.apache.camel.spring.SpringCamelContext;
import io.micrometer.common.KeyValues;

import static org.qubership.integration.platform.engine.service.deployment.processing.actions.context.create.helpers.ChainElementTypeHelper.isHttpTriggerElement;

@Component
@OnAfterDeploymentContextCreated
public class HttpTriggerDependencyBinder extends ElementProcessingAction {
    private final MetricTagsHelper metricTagsHelper;

    @Autowired
    public HttpTriggerDependencyBinder(MetricTagsHelper metricTagsHelper) {
        this.metricTagsHelper = metricTagsHelper;
    }

    @Override
    public boolean applicableTo(ElementProperties properties) {
        return isHttpTriggerElement(properties);
    }

    @Override
    public void apply(
        SpringCamelContext context,
        ElementProperties elementProperties,
        DeploymentInfo deploymentInfo
    ) {
        KeyValues tags = metricTagsHelper.buildMetricTags(deploymentInfo, elementProperties,
            deploymentInfo.getChainName());
        ServletTagsProvider servletTagsProvider = new ServletTagsProvider(tags);
        String elementId = elementProperties.getElementId();
        context.getRegistry().bind(elementId, ServletTagsProvider.class, servletTagsProvider);
    }
}
