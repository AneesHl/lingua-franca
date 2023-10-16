package org.lflang.target.property;

import java.util.Arrays;
import java.util.List;
import org.lflang.MessageReporter;
import org.lflang.Target;
import org.lflang.TargetProperty;
import org.lflang.ast.ASTUtils;
import org.lflang.lf.Element;
import org.lflang.target.property.type.BuildTypeType;
import org.lflang.target.property.type.BuildTypeType.BuildType;

/**
 * Directive to specify the target build type such as 'Release' or 'Debug'. This is also used in the
 * Rust target to select a Cargo profile.
 */
public class BuildTypeProperty extends TargetProperty<BuildType, BuildTypeType> {

  /** Singleton target property instance. */
  public static final BuildTypeProperty INSTANCE = new BuildTypeProperty();

  private BuildTypeProperty() {
    super(new BuildTypeType());
  }

  @Override
  public Element toAstElement(BuildType value) {
    return ASTUtils.toElement(value.toString());
  }

  @Override
  public BuildType initialValue() {
    return BuildTypeType.BuildType.RELEASE;
  }

  @Override
  public BuildType fromAst(Element node, MessageReporter reporter) {
    return fromString(ASTUtils.elementToSingleString(node), reporter);
  }

  @Override
  protected BuildType fromString(String string, MessageReporter reporter) {
    return ((BuildTypeType) this.type).forName(string);
  }

  @Override
  public String name() {
    return "build-type";
  }

  @Override
  public List<Target> supportedTargets() {
    return Arrays.asList(Target.C, Target.CCPP, Target.CPP, Target.Rust);
  }
}
