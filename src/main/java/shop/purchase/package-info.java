/**
 * Module {@code purchase}. Only the {@code api.facade} named interface (if any) is visible to other modules;
 * {@code api.http} is the HTTP contract for external clients and {@code internal} is private.
 */
@ApplicationModule(displayName = "Purchase", allowedDependencies = {"cart::facade", "catalog::facade", "shared"})
package shop.purchase;

import org.springframework.modulith.ApplicationModule;
