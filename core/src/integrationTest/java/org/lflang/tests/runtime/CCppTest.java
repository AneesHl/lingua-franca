package org.lflang.tests.runtime;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.lflang.ast.ASTUtils;
import org.lflang.target.Target;
import org.lflang.tests.Configurators;
import org.lflang.tests.TestBase;
import org.lflang.tests.TestRegistry.TestCategory;

/**
 * Collection of tests for the CCpp target.
 *
 * <p>NOTE: This test does not inherit any tests because it directly extends TestBase.
 *
 * @author Marten Lohstroh
 */
public class CCppTest extends TestBase {

  /**
   * This target selects the C target it has no tests defined for it. Instead, it reconfigures
   * existing C tests to adopt the CCpp target.
   */
  public CCppTest() {
    super(Target.C);
  }

  /** Run C tests with the target CCpp. */
  @Test
  public void runAsCCpp() {
    Assumptions.assumeFalse(isWindows(), Message.NO_WINDOWS_SUPPORT);
    runTestsForTargets(
        Message.DESC_AS_CCPP,
        CCppTest::isExcludedFromCCpp,
        resource -> ASTUtils.changeTargetName(resource, Target.CCPP.getDisplayName()),
        Configurators::noChanges,
        TestLevel.EXECUTION,
        true);
  }

  /** Exclusion function for runAsCCpp test */
  private static boolean isExcludedFromCCpp(TestCategory category) {
    boolean excluded = category == TestCategory.SERIALIZATION;
    excluded |= isWindows() && (category == TestCategory.DOCKER_FEDERATED);
    excluded |=
        isMac() && (category == TestCategory.DOCKER_FEDERATED || category == TestCategory.DOCKER);
    excluded |= category == TestCategory.ZEPHYR_UNTHREADED;
    excluded |= category == TestCategory.ZEPHYR_THREADED;
    excluded |= category == TestCategory.ZEPHYR_BOARDS;
    excluded |= category == TestCategory.ARDUINO;
    excluded |= category == TestCategory.PATMOS;
    excluded |= category == TestCategory.NO_INLINING;
    excluded |= category == TestCategory.VERIFIER;
    return !excluded;
  }
}
