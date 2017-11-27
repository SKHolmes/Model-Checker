package mc.compiler.ast;

import mc.util.Location;

import java.util.List;

public class RelabelNode extends ASTNode {

	// fields
	private List<RelabelElementNode> relabels;

	public RelabelNode(List<RelabelElementNode> relabels, Location location){
		super(location);
		this.relabels = relabels;
	}

	public List<RelabelElementNode> getRelabels(){
		return relabels;
	}

	public void setRelabels(List<RelabelElementNode> relabels){
		this.relabels = relabels;
	}

    public boolean equals(Object obj){
        boolean result = super.equals(obj);
        if(!result){
            return false;
        }
        if(obj == this){
            return true;
        }
        if(obj instanceof RelabelNode){
            RelabelNode node = (RelabelNode)obj;
            return relabels.equals(node.getRelabels());
        }

        return false;
    }

}
