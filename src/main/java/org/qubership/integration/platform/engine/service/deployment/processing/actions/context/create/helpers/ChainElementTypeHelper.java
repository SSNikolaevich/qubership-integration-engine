package org.qubership.integration.platform.engine.service.deployment.processing.actions.context.create.helpers;

import org.qubership.integration.platform.engine.model.ChainElementType;
import org.qubership.integration.platform.engine.model.deployment.update.ElementProperties;
import org.qubership.integration.platform.engine.model.constants.CamelConstants.ChainProperties;

public final class ChainElementTypeHelper {
    public static boolean isServiceCallOrAsyncApiTrigger(ChainElementType chainElementType) {
        return ChainElementType.SERVICE_CALL.equals(chainElementType)
            || ChainElementType.ASYNCAPI_TRIGGER.equals(chainElementType);
    }

    public static boolean isHttpTriggerElement(ElementProperties elementProperties) {
        String elementType = elementProperties.getProperties().get(ChainProperties.ELEMENT_TYPE);
        ChainElementType chainElementType = ChainElementType.fromString(elementType);
        return ChainElementType.HTTP_TRIGGER.equals(chainElementType);
    }
}
