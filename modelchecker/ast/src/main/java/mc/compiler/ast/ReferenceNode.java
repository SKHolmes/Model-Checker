package mc.compiler.ast;

import mc.util.Location;

/**
 * Created by sheriddavi on 24/01/17.
 */
public class ReferenceNode extends ASTNode {

    // fields
    private String reference;

    public ReferenceNode(String reference, Location location){
        super(location);
        this.reference = reference;
    }

    public String getReference(){
        return reference;
    }

    public boolean equals(Object obj){
        boolean result = super.equals(obj);
        if(!result){
            return false;
        }
        if(obj == this){
            return true;
        }
        if(obj instanceof ReferenceNode){
            ReferenceNode node = (ReferenceNode)obj;
            return reference.equals(node.getReference());
        }

        return false;
    }
}
