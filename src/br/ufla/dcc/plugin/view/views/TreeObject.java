package br.ufla.dcc.plugin.view.views;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jdt.core.IMethod;

/**
 * Represents a method and its callers.
 * @author jlucasps
 *
 */
public class TreeObject{

	private IMethod method;
	private ArrayList<TreeObject> children;
	private TreeObject parent;
	
	public TreeObject(IMethod method){
		this.method = method;
		this.children = new ArrayList<TreeObject>();
	}
	
	public TreeObject(IMethod method, TreeObject[] children){
		this.method = method;
		List<TreeObject> list = Arrays.asList(children);
		this.children = new ArrayList<TreeObject>(list);
		for (int i = 0; i < this.children.size(); i++) {
		         this.children.get(i).setParent(this);
      }
	}
	
	public void addChild(TreeObject treeObject){
		treeObject.setParent(this);
		this.getChildren().add(treeObject);
	}
	
	public IMethod getMethod() {
		return method;
	}

	public void setMethod(IMethod method) {
		this.method = method;
	}

	public ArrayList<TreeObject> getChildren() {
		return children;
	}

	public void setChildren(ArrayList<TreeObject> children) {
		this.children = children;
	}

	public TreeObject getParent() {
		return parent;
	}

	public void setParent(TreeObject parent) {
		this.parent = parent;
	}
	
	public String toString(){
		String defaultLabel = this.getMethod().getDeclaringType().getElementName() + "."+this.getMethod().getElementName(); 
		if(this.getChildren().size() > 0){
			return "( " + this.getChildren().size() + " ) " + defaultLabel;
		}
		return defaultLabel;
	}
	
}
