package aip.findings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import aip.core.csm.CsmElementId;
import aip.core.csm.CsmScopeInstance;
import aip.core.csm.CsmSnapshotId;
import org.junit.jupiter.api.Test;

/**
 * {@link ConcernedElementResolver} tests, per {@code
 * implement-finding-model/design.md} Decision 5.
 */
class ConcernedElementResolverTest {

  @Test
  void anchoredScopeInstanceResolvesToItsAnchorElement() {
    CsmElementId anchor = new CsmElementId("csm:element:m1");
    CsmSnapshotId snapshot = new CsmSnapshotId("repo", 1);
    assertEquals(anchor, ConcernedElementResolver.resolve(CsmScopeInstance.anchoredAt(anchor), snapshot));
  }

  @Test
  void unanchoredScopeInstanceResolvesToADeterministicSyntheticSubjectDerivedFromTheRepositoryIdentifier() {
    CsmSnapshotId snapshotOne = new CsmSnapshotId("repo-x", 1);
    CsmSnapshotId snapshotTwo = new CsmSnapshotId("repo-x", 2);

    CsmElementId subjectFromSnapshotOne = ConcernedElementResolver.resolve(CsmScopeInstance.wholeRepository(), snapshotOne);
    CsmElementId subjectFromSnapshotTwo = ConcernedElementResolver.resolve(CsmScopeInstance.wholeRepository(), snapshotTwo);

    // Same repository, two different snapshot sequence numbers -> the
    // same synthesized subject (stability across CSM Snapshots of the
    // same repository is the entire point).
    assertEquals(subjectFromSnapshotOne, subjectFromSnapshotTwo);
  }

  @Test
  void differentRepositoriesResolveToDistinguishableSyntheticSubjects() {
    CsmElementId subjectA =
        ConcernedElementResolver.resolve(CsmScopeInstance.wholeRepository(), new CsmSnapshotId("repo-a", 1));
    CsmElementId subjectB =
        ConcernedElementResolver.resolve(CsmScopeInstance.wholeRepository(), new CsmSnapshotId("repo-b", 1));
    assertNotEquals(subjectA, subjectB);
  }
}
