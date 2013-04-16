package br.ufla.dcc.plugin.view.views;

import java.util.HashMap;

import org.eclipse.jdt.core.IType;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.zest.core.widgets.Graph;
import org.eclipse.zest.core.widgets.GraphConnection;
import org.eclipse.zest.core.widgets.GraphNode;
import org.eclipse.zest.core.widgets.ZestStyles;
import org.eclipse.zest.layouts.LayoutStyles;
import org.eclipse.zest.layouts.algorithms.SpringLayoutAlgorithm;

/**
 * This class implements a GraphView inside Eclipse perspective.
 * @author jlucasps
 */
public class GraphView extends ViewPart {

	/** GraphView Id*/
	public static final String ID_FANIN_GRAPH_VIEW = "br.ufla.dcc.plugin.fanin.graphView";
	/** Parent widget composite. */
	private Composite parent;
	/** Graph of Zest API*/
	private Graph graph;
	
	public GraphView() {
	}

	@Override
	public void createPartControl(Composite parent) {
		this.graph = new Graph(parent, SWT.NONE);
		this.parent = parent;
	}

	/**
	 * Updates view.
	 * @param treeObject TreeObject representing a Methods and its callers
	 */
	public void update(TreeObject treeObject){
		
		HashMap<IType, Color> colors = this.getContrastedColors(treeObject);
		
		GraphNode methodNode = new GraphNode(this.getGraph(), SWT.NONE,  treeObject.getMethod().getDeclaringType().getElementName() +"."+ treeObject.getMethod().getElementName());
		methodNode.setBackgroundColor(parent.getDisplay().getSystemColor(SWT.COLOR_RED));
		// Creates nodes in Zest plugin
		GraphNode[] childrenNode = new GraphNode[treeObject.getChildren().size()];
		for(int i=0; i<childrenNode.length; i++){
			childrenNode[i] = new GraphNode(this.getGraph(), SWT.NONE, treeObject.getChildren().get(i).getMethod().getDeclaringType().getElementName() +"."+ treeObject.getChildren().get(i).getMethod().getElementName());
			new GraphConnection(this.getGraph(), ZestStyles.CONNECTIONS_DIRECTED, childrenNode[i], methodNode);
			childrenNode[i].setBackgroundColor(colors.get(treeObject.getChildren().get(i).getMethod().getDeclaringType()));
		}
		graph.setLayoutAlgorithm(new SpringLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING), true);
	}

	/**
	 * Return different colors.
	 * @param parent Structure of method and its callers.
	 * @return Maps containing types and colors
	 */
	public HashMap<IType, Color> getContrastedColors(TreeObject parent){
		
		HashMap<IType, Color> colors= new HashMap<IType, Color>();
		colors.put( parent.getMethod().getDeclaringType(), this.getParent().getDisplay().getSystemColor(SWT.COLOR_RED));
		int currentColor = 5;
		for(int i=0; i<parent.getChildren().size(); i++){
			if(!colors.containsKey(parent.getChildren().get(i).getMethod().getDeclaringType())){
				colors.put(parent.getChildren().get(i).getMethod().getDeclaringType(), this.getParent().getDisplay().getSystemColor(currentColor + 2));
				currentColor += 2;
			}
		}
		return colors;
	}

	// Getters and setters.
	@Override
	public void setFocus() {
		// TODO Auto-generated method stub
	}

	public Composite getParent() {
		return parent;
	}

	public void setParent(Composite parent) {
		this.parent = parent;
	}

	public Graph getGraph() {
		return graph;
	}

	public void setGraph(Graph graph) {
		this.graph = graph;
	}
}
