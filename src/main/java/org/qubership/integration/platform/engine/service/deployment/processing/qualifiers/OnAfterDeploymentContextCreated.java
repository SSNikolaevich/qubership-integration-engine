package org.qubership.integration.platform.engine.service.deployment.processing.qualifiers;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

import org.springframework.beans.factory.annotation.Qualifier;

@Target({
    ElementType.FIELD, 
    ElementType.METHOD,
    ElementType.TYPE, 
    ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Qualifier
public @interface OnAfterDeploymentContextCreated {
}
