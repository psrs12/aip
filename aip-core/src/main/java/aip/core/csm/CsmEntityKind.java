package aip.core.csm;

/**
 * The Canonical Software Model's closed, versioned set of core
 * structural entity kinds, per the {@code CSM Conceptual Vocabulary}
 * requirement: "The core structural entity kinds SHALL include, at
 * minimum: Repository, Project, Module, Package, Type, Architecture
 * Component, and External System. Method is also part of the core
 * vocabulary, but its instantiation by a given analyzer is optional."
 *
 * <p>This enum is exhaustive as of the currently archived
 * specification. Introducing a new entity kind requires a deliberate,
 * versioned specification change — never a silent addition here (see
 * {@code Entity expressed using core vocabulary}).
 */
public enum CsmEntityKind {
  REPOSITORY,
  PROJECT,
  MODULE,
  PACKAGE,
  TYPE,
  METHOD,
  ARCHITECTURE_COMPONENT,
  EXTERNAL_SYSTEM
}
