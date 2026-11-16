/**
 * Module {@code catalog}. Only the {@code api.facade} named interface (if any) is visible to other modules;
 * {@code api.http} is the HTTP contract for external clients and {@code internal} is private.
 */
@ApplicationModule(displayName = "Catalog", allowedDependencies = {"shared"})
package shop.catalog;

import org.springframework.modulith.ApplicationModule;
