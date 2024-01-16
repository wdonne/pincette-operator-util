package net.pincette.operator.util;

import static io.javaoperatorsdk.operator.api.reconciler.Constants.DEFAULT_NAMESPACES_SET;
import static io.javaoperatorsdk.operator.api.reconciler.UpdateControl.noUpdate;
import static io.javaoperatorsdk.operator.api.reconciler.UpdateControl.patchStatus;
import static java.lang.System.getenv;
import static java.util.Optional.ofNullable;
import static net.pincette.util.Util.getSegments;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.RegisteredController;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Some utilities to work with operators.
 *
 * @author Werner Donné
 */
public class Util {

  private static final Set<String> WATCHED_NAMESPACES = watchedNamespaces();

  private static final String WATCH_NAMESPACES_ENV = "WATCH_NAMESPACES";

  private Util() {}

  /**
   * Returns all resources of some type in any namespace.
   *
   * @param client the Kubernetes client.
   * @param c the resource type.
   * @return The stream of resources.
   * @param <T> the resource type.
   */
  public static <T extends HasMetadata> Stream<T> allResources(
      final KubernetesClient client, final Class<T> c) {
    return client.resources(c).inAnyNamespace().list().getItems().stream();
  }

  public static <T extends HasMetadata> boolean exists(
      final KubernetesClient client, final T resource) {
    return client.resource(resource).get() != null;
  }

  /**
   * Sets the namespaces the controller should watch.
   *
   * @param controller the controller for which the watched namespaces are set.
   * @param namespaces the namespaces that should be watched. If it is <code>null</code>, the empty
   *     string or the value "*", then all namespaces are watched. Otherwise, the value should be a
   *     comma-delimited string.
   * @param <T> the controller type.
   */
  public static <T extends HasMetadata> void changeNamespaces(
      final RegisteredController<T> controller, final String namespaces) {
    controller.changeNamespaces(namespaces(namespaces));
  }

  /**
   * Sets the namespaces the controller should watch. It gets the value from the environment
   * variable <code>WATCH_NAMESPACES</code>.
   *
   * @param controller the controller for which the watched namespaces are set.
   * @param <T> the controller type.
   */
  public static <T extends HasMetadata> void changeNamespaces(
      final RegisteredController<T> controller) {
    controller.changeNamespaces(WATCHED_NAMESPACES);
  }

  /**
   * Uses the namespaces set in the <code>WATCH_NAMESPACES</code> environment variable to determine
   * if the resource be reconciled.
   *
   * @param resource the tested namespaced resource.
   * @return The result of the test.
   */
  public static boolean shouldReconcile(final HasMetadata resource) {
    return WATCHED_NAMESPACES.equals(DEFAULT_NAMESPACES_SET)
        || ofNullable(resource.getMetadata().getNamespace())
            .map(WATCHED_NAMESPACES::contains)
            .orElse(false);
  }

  private static Set<String> namespaces(final String namespaces) {
    return ofNullable(namespaces)
        .filter(env -> !env.equals("*") && !env.isEmpty())
        .map(env -> getSegments(env, ",").collect(Collectors.toSet()))
        .orElse(DEFAULT_NAMESPACES_SET);
  }

  public static <T extends HasMetadata> UpdateControl<T> replyUpdateIfExists(
      final KubernetesClient client, final T resource) {
    return Util.exists(client, resource) ? patchStatus(resource) : noUpdate();
  }

  public static Set<String> watchedNamespaces() {
    return namespaces(getenv(WATCH_NAMESPACES_ENV));
  }
}
