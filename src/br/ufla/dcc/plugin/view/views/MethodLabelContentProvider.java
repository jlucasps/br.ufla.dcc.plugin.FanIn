package br.ufla.dcc.plugin.view.views;

import org.eclipse.jface.viewers.LabelProvider;

/**
 * This class provides a label to method into view.
 * @author jlucasps
 */
public class MethodLabelContentProvider extends LabelProvider {

	public String getText(Object element){
		TreeObject treeObject = (TreeObject) element;
		
		String defaultLabel = treeObject.getMethod().getDeclaringType().getElementName() + "."+treeObject.getMethod().getElementName(); 
		
		if(treeObject.getChildren().size() > 0){
			return "( " + treeObject.getChildren().size() + " ) " + defaultLabel;
		}
		
		return defaultLabel;
	}
	
}
