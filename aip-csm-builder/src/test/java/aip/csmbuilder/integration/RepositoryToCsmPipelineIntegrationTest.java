package aip.csmbuilder.integration;

/**
 * RESERVED — not implemented by this change (tasks.md 22.3).
 *
 * <p>This class's name and package are the fixed seam for the future
 * real-pipeline integration test exercising the full {@code
 * Repository → Repository Understanding → Repository Evidence Model →
 * CSM Builder → CSM} chain, once a real Repository Understanding
 * implementation exists to supply the middle of it. That work belongs
 * to the future {@code implement-software-repository-understanding}
 * change (see {@code openspec/changes/implement-csm-builder/design.md}
 * Decision 6, and invariants 8 and 9) — not to this change, which
 * builds CSM Builder against a fixture-based Repository Evidence
 * contract only, per the locked sequencing decision in {@code
 * explore.md}/{@code proposal.md}.
 *
 * <p>Every fixture-based test this change delivers is deliberately
 * <strong>not</strong> this test — see the {@code *FixtureTest}/{@code
 * *FixtureEndToEndTest} naming convention and the {@code
 * aip.csmbuilder.test.fixtures} package, neither of which claims
 * real-pipeline coverage (enforced by {@code
 * scripts/check-test-naming.sh}, tasks.md 22.2).
 *
 * <p>No test body is written here on purpose: a placeholder {@code
 * @Test} method — even a skipped or trivially-passing one — would
 * misleadingly suggest partial real-pipeline coverage exists before it
 * does. This class intentionally declares no test methods and is
 * never picked up as an active test by the build.
 */
public final class RepositoryToCsmPipelineIntegrationTest {

  private RepositoryToCsmPipelineIntegrationTest() {}
}
