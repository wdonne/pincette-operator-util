package net.pincette.operator.util;

import static java.time.Instant.now;
import static java.util.Collections.emptyList;
import static net.pincette.util.Collections.*;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.function.Supplier;
import net.pincette.util.StreamUtil;

/**
 * Represents a Kubernetes resource status object.
 *
 * @author Werner Donné
 */
public class Status {
  public static final String AVAILABLE = "Available";
  public static final String ERROR = "Error";
  public static final String EXCEPTION = "Exception";
  public static final String FALSE = "False";
  public static final String HEALTHY = "Healthy";
  public static final String OK = "OK";
  public static final String PENDING = "Pending";
  public static final String PROGRESSING = "Progressing";
  public static final String READY = "Ready";
  public static final String TRUE = "True";
  public static final String UNHEALTHY = "Unhealthy";
  public static final String UNKNOWN = "Unknown";

  @JsonProperty("conditions")
  public final List<Condition> conditions;

  @JsonProperty("health")
  public final Health health;

  @JsonProperty("phase")
  public final String phase;

  /** Creates a status without conditions and the phase "Ready". */
  public Status() {
    this(emptyList(), READY, Health.healthy());
  }

  private Status(final List<Condition> conditions, final String phase, final Health health) {
    this.conditions = conditions;
    this.phase = phase;
    this.health = health;
  }

  private Health health() {
    return StreamUtil.last(conditions.stream())
        .map(Condition::health)
        .orElseGet(() -> READY.equals(phase) ? Health.healthy() : Health.unknown());
  }

  /**
   * Returns new status with a new condition that comes after the existing ones. No more than five
   * conditions are retained. The phase is set to "Ready" is the condition is ready, otherwise it
   * becomes "Pending".
   *
   * @param condition the new condition.
   * @return The new status object.
   */
  public Status withCondition(final Condition condition) {
    return new Status(
        concat(last(conditions, 4), list(condition)),
        condition.isReady() ? READY : PENDING,
        condition.health());
  }

  public Status withError(final String message) {
    return withCondition(new Condition().withError(message));
  }

  public Status withException(final Throwable e) {
    return withCondition(new Condition().withException(e));
  }

  public Status withPhase(final String phase) {
    return new Status(conditions, phase, health());
  }

  public static class Condition {
    @JsonProperty("lastTransitionTime")
    public final String lastTransitionTime = now().toString();

    @JsonProperty("message")
    public final String message;

    @JsonProperty("reason")
    public final String reason;

    @JsonProperty("status")
    public final String status;

    @JsonProperty("type")
    public final String type;

    public Condition() {
      this(OK, OK, TRUE, READY);
    }

    private Condition(
        final String message, final String reason, final String status, final String type) {
      this.message = message;
      this.reason = reason;
      this.status = status;
      this.type = type;
    }

    private Health health() {
      final Supplier<Health> tryUnhealthy = () -> isError() ? Health.unhealthy() : Health.unknown();

      return isReady() ? Health.healthy() : tryUnhealthy.get();
    }

    private boolean isError() {
      return ERROR.equals(reason) || EXCEPTION.equals(reason);
    }

    private boolean isReady() {
      return READY.equals(type) && TRUE.equals(status);
    }

    public Condition withError(final String message) {
      return new Condition(message, ERROR, FALSE, READY);
    }

    public Condition withException(final Throwable e) {
      return new Condition(e.getMessage(), EXCEPTION, FALSE, READY);
    }

    public Condition withMessage(final String message) {
      return new Condition(message, reason, status, type);
    }

    public Condition withReason(final String reason) {
      return new Condition(message, reason, status, type);
    }

    public Condition withStatus(final String status) {
      return new Condition(message, reason, status, type);
    }

    public Condition withType(final String type) {
      return new Condition(message, reason, status, type);
    }
  }

  public static class Health {
    @JsonProperty("status")
    public final String status;

    public Health() {
      this(HEALTHY);
    }

    private Health(final String status) {
      this.status = status;
    }

    public static Health healthy() {
      return new Health(HEALTHY);
    }

    public static Health unhealthy() {
      return new Health(UNHEALTHY);
    }

    public static Health unknown() {
      return new Health(UNKNOWN);
    }
  }
}
