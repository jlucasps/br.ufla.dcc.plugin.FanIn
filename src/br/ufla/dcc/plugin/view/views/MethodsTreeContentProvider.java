package br.ufla.dcc.plugin.view.views;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;

/**
 * This class Implements a TreeContetProvider to show methods and its callers inside Eclipse view.
 * @author jlucasps
 */
public class MethodsTreeContentProvider extends ArrayContentProvider implements ITreeContentProvider{

	@Override
	public Object[] getChildren(Object parentElement) {
		TreeObject treeObject = (TreeObject) parentElement;
		
		return treeObject.getChildren().toArray();
	}

	@Override
	public Object getParent(Object element) {
		TreeObject treeObject = (TreeObject) element;
		return treeObject.getParent();
	}

	@Override
	public boolean hasChildren(Object element) {
		TreeObject treeObject = (TreeObject) element;
		return treeObject.getChildren().size() > 0;
	}


	
}
