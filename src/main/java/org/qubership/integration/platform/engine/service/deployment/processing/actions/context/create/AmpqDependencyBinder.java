package org.qubership.integration.platform.engine.service.deployment.processing.actions.context.create;

import org.apache.camel.spring.SpringCamelContext;
import org.qubership.integration.platform.engine.model.deployment.update.DeploymentInfo;
import org.qubership.integration.platform.engine.model.deployment.update.ElementProperties;
import org.qubership.integration.platform.engine.model.ChainElementType;
import org.qubership.integration.platform.engine.model.constants.CamelConstants.ChainProperties;
import org.qubership.integration.platform.engine.service.debugger.metrics.MetricsStore;
import org.qubership.integration.platform.engine.service.deployment.processing.ElementProcessingAction;
import org.qubership.integration.platform.engine.model.ElementOptions;
import org.qubership.integration.platform.engine.camel.components.rabbitmq.NoOpMetricsCollector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import io.micrometer.core.instrument.Tag;
import org.apache.commons.lang3.StringUtils;
import com.rabbitmq.client.MetricsCollector;
import com.rabbitmq.client.impl.MicrometerMetricsCollector;

import static org.qubership.integration.platform.engine.service.deployment.processing.actions.context.create.helpers.ChainElementTypeHelper.isServiceCallOrAsyncApiTrigger;
import org.qubership.integration.platform.engine.service.deployment.processing.actions.context.create.helpers.MetricTagsHelper;
import org.qubership.integration.platform.engine.service.deployment.processing.qualifiers.OnAfterDeploymentContextCreated;

import static org.qubership.integration.platform.engine.service.debugger.metrics.MetricsStore.MAAS_CLASSIFIER;

import java.util.Collection;

@Component
@OnAfterDeploymentContextCreated
public class AmpqDependencyBinder extends ElementProcessingAction {
    private final MetricsStore metricsStore;
    private final MetricTagsHelper metricTagsHelper;

    @Autowired
    public AmpqDependencyBinder(
        MetricsStore metricsStore,
        MetricTagsHelper metricTagsHelper
    ) {
        this.metricsStore = metricsStore;
        this.metricTagsHelper = metricTagsHelper;
    }

    @Override
    public boolean applicableTo(ElementProperties properties) {
        String elementType = properties.getProperties().get(ChainProperties.ELEMENT_TYPE);
        ChainElementType chainElementType = ChainElementType.fromString(elementType);
        return ChainElementType.isAmqpAsyncElement(chainElementType) && (
            (!isServiceCallOrAsyncApiTrigger(chainElementType))
                || ChainProperties.OPERATION_PROTOCOL_TYPE_AMQP.equals(
                properties.getProperties().get(ChainProperties.OPERATION_PROTOCOL_TYPE_PROP)));
    }

    @Override
    public void apply(
        SpringCamelContext context,
        ElementProperties properties,
        DeploymentInfo deploymentInfo
    ) {
        Collection<Tag> tags = metricTagsHelper.buildMetricTagsLegacy(deploymentInfo, properties,
                deploymentInfo.getChainName());

        String maasClassifier = properties.getProperties()
            .get(ElementOptions.MAAS_DEPLOYMENT_CLASSIFIER_PROP);
        if (!StringUtils.isEmpty(maasClassifier)) {
            tags.add(Tag.of(MAAS_CLASSIFIER, maasClassifier));
        }

        MetricsCollector metricsCollector = metricsStore.isMetricsEnabled()
            ? new MicrometerMetricsCollector(
            metricsStore.getMeterRegistry(), "rabbitmq", tags)
            : new NoOpMetricsCollector();
        String elementId = properties.getElementId();
        context.getRegistry().bind(elementId, MetricsCollector.class, metricsCollector);
    }
}
