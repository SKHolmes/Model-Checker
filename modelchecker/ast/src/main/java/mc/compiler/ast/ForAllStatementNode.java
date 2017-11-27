package mc.compiler.ast;

import lombok.ToString;
import mc.util.Location;
@ToString
public class ForAllStatementNode extends ASTNode {

	// fields
	private RangesNode ranges;
	private ASTNode process;

	public ForAllStatementNode(RangesNode ranges, ASTNode process, Location location){
		super(location);
		this.ranges = ranges;
		this.process = process;
	}

	public RangesNode getRanges(){
		return ranges;
	}

	public void setRanges(RangesNode ranges){
		this.ranges = ranges;
	}

	public ASTNode getProcess(){
		return process;
	}

	public void setProcess(ASTNode process){
		this.process = process;
	}

    public boolean equals(Object obj){
        boolean result = super.equals(obj);
        if(!result){
            return false;
        }
        if(obj == this){
            return true;
        }
        if(obj instanceof ForAllStatementNode){
            ForAllStatementNode node = (ForAllStatementNode)obj;
            if(!ranges.equals(node.getRanges())){
                return false;
            }
            return process.equals(node.getProcess());
        }

        return false;
    }
}
