/**
 * Module {@code rating}. Only the {@code api.facade} named interface (if any) is visible to other modules;
 * {@code api.http} is the HTTP contract for external clients and {@code internal} is private.
 */
@ApplicationModule(displayName = "Rating", allowedDependencies = {"catalog::facade", "shared"})
package shop.rating;

import org.springframework.modulith.ApplicationModule;
