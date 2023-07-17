module net.pincette.operator.util {
  requires kubernetes.client.api;
  requires kubernetes.model.core;
  requires net.pincette.common;
  requires com.fasterxml.jackson.annotation;
  requires kubernetes.model.common;

  exports net.pincette.operator.util;
}
