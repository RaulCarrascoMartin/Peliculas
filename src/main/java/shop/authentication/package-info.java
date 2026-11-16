/**
 * Module {@code authentication}. Only the {@code api.facade} named interface (if any) is visible to other modules;
 * {@code api.http} is the HTTP contract for external clients and {@code internal} is private.
 */
@ApplicationModule(displayName = "Authentication", allowedDependencies = {"shared"})
package shop.authentication;

import org.springframework.modulith.ApplicationModule;
