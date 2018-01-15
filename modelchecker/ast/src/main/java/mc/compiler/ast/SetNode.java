package mc.compiler.ast;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import mc.util.Location;

/**
 * SetNode contains a set of transitions, or ranges.
 * <p>
 * This is used for indexing, function optional arguments and for event hiding.
 * Syntactically this is {@code SET :: "{" (ACTION ",")* ACTION "}"}
 *
 * @author David Sheridan
 * @author Sanjay Govind
 * @author Jacob Beal
 * @see HidingNode
 * @see FunctionNode
 * @see ASTNode
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SetNode extends ASTNode {

  /**
   * The string items in the set.
   */
  private List<String> set;

  /**
   * The items within the set that are ranges.
   */
  private Map<Integer, RangesNode> rangeMap;

  /**
   * Instantiate a new set node, with ranges in.
   *
   * @param set      the strings within the set. {@link #set}
   * @param rangeMap the ranges (e.g. {@code [0..2]}) {@link #rangeMap}
   * @param location the location of this node within the users code {@link ASTNode#location}
   */
  public SetNode(List<String> set, Map<Integer, RangesNode> rangeMap, Location location) {
    super(location);
    this.set = set;
    this.rangeMap = rangeMap;
  }

  /**
   * Instantiate a new set node, without ranges in.
   *
   * @param set      the strings within the set. {@link #set}
   * @param location the location of this node within the users code {@link ASTNode#location}
   */
  public SetNode(List<String> set, Location location) {
    super(location);
    this.set = set;
    this.rangeMap = new HashMap<>();
  }
}
