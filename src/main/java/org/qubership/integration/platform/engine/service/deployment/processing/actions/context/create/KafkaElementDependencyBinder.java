package org.qubership.integration.platform.engine.service.deployment.processing.actions.context.create;

import org.apache.camel.spring.SpringCamelContext;
import org.qubership.integration.platform.engine.model.deployment.update.DeploymentInfo;
import org.qubership.integration.platform.engine.model.deployment.update.ElementProperties;
import org.qubership.integration.platform.engine.camel.components.kafka.TaggedMetricsKafkaClientFactory;
import org.qubership.integration.platform.engine.model.ChainElementType;
import org.qubership.integration.platform.engine.model.constants.CamelConstants.ChainProperties;
import org.qubership.integration.platform.engine.service.debugger.metrics.MetricsStore;
import org.qubership.integration.platform.engine.service.deployment.processing.actions.context.create.helpers.MetricTagsHelper;
import org.qubership.integration.platform.engine.service.deployment.processing.ElementProcessingAction;
import org.qubership.integration.platform.engine.service.deployment.processing.qualifiers.OnAfterDeploymentContextCreated;
import org.qubership.integration.platform.engine.model.ElementOptions;
import org.apache.camel.component.kafka.DefaultKafkaClientFactory;
import org.apache.camel.component.kafka.KafkaClientFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import io.micrometer.core.instrument.Tag;
import org.apache.commons.lang3.StringUtils;

import static org.qubership.integration.platform.engine.service.debugger.metrics.MetricsStore.MAAS_CLASSIFIER;
import static org.qubership.integration.platform.engine.service.deployment.processing.actions.context.create.helpers.ChainElementTypeHelper.isServiceCallOrAsyncApiTrigger;

import java.util.Collection;

@Component
@Order(KafkaElementDependencyBinder.ORDER)
@OnAfterDeploymentContextCreated
public class KafkaElementDependencyBinder extends ElementProcessingAction {
    public static final int ORDER = Ordered.HIGHEST_PRECEDENCE;

    private final MetricsStore metricsStore;
    private final MetricTagsHelper metricTagsHelper;

    @Autowired
    public KafkaElementDependencyBinder(
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
        return ChainElementType.isKafkaAsyncElement(chainElementType) && (
            (!isServiceCallOrAsyncApiTrigger(chainElementType))
                || ChainProperties.OPERATION_PROTOCOL_TYPE_KAFKA.equals(
                properties.getProperties().get(ChainProperties.OPERATION_PROTOCOL_TYPE_PROP)));
    }

    @Override
    public void apply(
        SpringCamelContext context,
        ElementProperties properties,
        DeploymentInfo deploymentInfo
    ) {
        String elementId = properties.getElementId();
        DefaultKafkaClientFactory defaultFactory = new DefaultKafkaClientFactory();
        Collection<Tag> tags = metricTagsHelper.buildMetricTagsLegacy(deploymentInfo, properties, deploymentInfo.getChainName());

        String maasClassifier = properties.getProperties().get(ElementOptions.MAAS_DEPLOYMENT_CLASSIFIER_PROP);
        if (!StringUtils.isEmpty(maasClassifier)) {
            tags.add(Tag.of(MAAS_CLASSIFIER, maasClassifier));
        }

        // For camel 'kafka' and 'kafka-custom' component
        KafkaClientFactory kafkaClientFactory = metricsStore.isMetricsEnabled()
            ? new TaggedMetricsKafkaClientFactory(
            defaultFactory,
            metricsStore.getMeterRegistry(),
            tags)
            : defaultFactory;
        context.getRegistry().bind(elementId, KafkaClientFactory.class, kafkaClientFactory);
        context.getRegistry().bind(elementId + "-v2", KafkaClientFactory.class, kafkaClientFactory);
    }
}
