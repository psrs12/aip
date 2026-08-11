package aip.core.csm;

import java.util.Objects;

/**
 * A CSM element or relationship's stable identity.
 *
 * <p>This type deliberately holds a single opaque, stable string
 * value rather than prescribing a derivation scheme — how that string
 * is computed (e.g. as a function of a Repository Evidence identity)
 * is a CSM Builder concern, not a domain-model concern. See
 * {@code CSM Element Identity Derivation} and
 * {@code CSM Relationship Identity Derivation} in
 * {@code openspec/specs/csm-builder/spec.md} for the derivation rules
 * this identity type supports.
 *
 * @param value the opaque, stable identity string. Never blank.
 */
public record CsmElementId(String value) {

  public CsmElementId {
    Objects.requireNonNull(value, "value");
    if (value.isBlank()) {
      throw new IllegalArgumentException("CsmElementId value must not be blank");
    }
  }

  @Override
  public String toString() {
    return value;
  }
}
