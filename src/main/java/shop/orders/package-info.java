/**
 * Module {@code orders}. Only the {@code api.facade} named interface (if any) is visible to other modules;
 * {@code api.http} is the HTTP contract for external clients and {@code internal} is private.
 */
@ApplicationModule(displayName = "Orders", allowedDependencies = {"catalog::facade", "shared"})
package shop.orders;

import org.springframework.modulith.ApplicationModule;
