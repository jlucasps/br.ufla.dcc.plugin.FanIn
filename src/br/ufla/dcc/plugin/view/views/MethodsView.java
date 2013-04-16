package br.ufla.dcc.plugin.view.views;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ViewPart;

import br.ufla.dcc.plugin.controller.FanInAnalysis;
import br.ufla.dcc.plugin.controller.FanInController;

public class MethodsView extends ViewPart{
	
	/** Logger for this class. */
	private static Logger log = Logger.getLogger(FanInAnalysis.class);
	/** Methods View Id*/
	public static final String ID_FANIN_METHODS_VIEW = "br.ufla.dcc.plugin.fanin.methodsView";

	/** Instance of FanIn Controller. */
	private FanInController fanInController;
	/** Toolkit form of Eclipse UI.*/
	private FormToolkit toolkit;
	/** Scroolled form of Eclipse UI.*/
	private ScrolledForm form;
	/** Section of Eclipse UI.*/
	private Section sectionParents;
	/** Section to show parent methods (called).*/
	private Composite sectionClientParents;
	/** Section of Eclipse widgets to show children methods (callers).*/
	private Section sectionChildren;
	private Composite sectionClientChildren;
	/** List of called.*/
	private List listParents;
	/** List of callers.*/
	private List listChildren;
	/** Eclipse layout grid.*/
	private GridData gd;
	/** Button. */
	private Button btnGraph;
	/** Label. */
	private Label label;
	
	/** Map to recover tree objects.*/
	private static Map<String, TreeObject> stringToTreeObjectParents;
	
	public MethodsView() {
	}

	/** {@inheritDoc} */
	@Override
	public void createPartControl(Composite parent) {
		
		toolkit = new FormToolkit(parent.getDisplay());
		form = toolkit.createScrolledForm(parent);
		toolkit.decorateFormHeading(form.getForm());
		form.setText("FanIn Analysis");
		GridLayout gridLayout = new GridLayout(2, false);
		form.getBody().setLayout(gridLayout);
		
		this.sectionParents = toolkit.createSection(form.getBody(), Section.DESCRIPTION | Section.TITLE_BAR | Section.EXPANDED | Section.TWISTIE);
		
		gd = new GridData();
		gd.horizontalAlignment = GridData.FILL;
		gd.grabExcessHorizontalSpace = true;
		
		sectionParents.setLayoutData(gd);
		sectionParents.addExpansionListener(new ExpansionAdapter() {
			public void expansionStateChanged(ExpansionEvent e) {
				form.reflow(true);
			}
		});
		sectionParents.setText("Methods sorted by FanIn");
		sectionClientParents = toolkit.createComposite(sectionParents);
		sectionClientParents.setLayout(new GridLayout());
		
		listParents = new List(sectionClientParents, SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL );
		gd = new GridData(GridData.FILL, GridData.FILL, true, true);
		gd.verticalSpan = 4;
		int listHeight = listParents.getItemHeight() * 12;
		Rectangle trim = listParents.computeTrim(0, 0, 0, listHeight);
		gd.heightHint = trim.height;
		listParents.setLayoutData(gd);
		
		sectionChildren = toolkit.createSection(form.getBody(), Section.DESCRIPTION | Section.TITLE_BAR | Section.COMPACT | Section.TWISTIE );
		
		gd = new GridData();
		gd.horizontalAlignment = GridData.FILL;
		gd.grabExcessHorizontalSpace = true;
		
		sectionChildren.setLayoutData(gd);
		sectionChildren.addExpansionListener(new ExpansionAdapter() {
			public void expansionStateChanged(ExpansionEvent e) {
				form.reflow(true);
			}
		});
		sectionChildren.setText("Callers");
		sectionClientChildren = toolkit.createComposite(sectionChildren);
		sectionClientChildren.setLayout(new GridLayout());
		
		listChildren = new List(sectionClientChildren, SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL );
		gd = new GridData(GridData.FILL, GridData.FILL, true, true);
		gd.verticalSpan = 4;
		listHeight = listChildren.getItemHeight() * 12;
		trim = listChildren.computeTrim(0, 0, 0, listHeight);
		gd.heightHint = trim.height;
		listChildren.setLayoutData(gd);
		
		btnGraph = toolkit.createButton(form.getBody(), "Visualize chart", SWT.PUSH);
		label = toolkit.createLabel(this.form.getBody(), "");
	}


	@Override
	public void setFocus() {
		form.setFocus();
	}

	public FanInController getFanInController() {
		return fanInController;
	}

	public void setFanInController(FanInController fanInController) {
		this.fanInController = fanInController;
		
	}

	/**
	 * Updates View
	 */
	public void update(){
		TreeObject[] parents = new TreeObject[this.getFanInController().getFanInAnalysis().getFanInsFiltered().size()];
		
		for(int i=0; i<parents.length; i++){
			TreeObject parent = new TreeObject(this.getFanInController().getFanInAnalysis().getFanInsFiltered().get(i).getInvokedMethod());
			Iterator<IMethod> it = this.getFanInController().getFanInAnalysis().getFanInsFiltered().get(i).getCallers().keySet().iterator(); 
			
			while(it.hasNext()){
				TreeObject child = new TreeObject((IMethod)it.next());
				parent.addChild(child);
			}
			parents[i] = parent;
		}
		
		MethodsView.stringToTreeObjectParents = new HashMap<String, TreeObject>();
		String[] itens = new String[parents.length];
		for(int i=0; i<parents.length; i++){
			String label = parents[i].getMethod().getDeclaringType().getElementName() + "."+parents[i].getMethod().getElementName();
			itens[i] = label;
			MethodsView.stringToTreeObjectParents.put(label, parents[i]);
		}
		
		listParents.setItems(itens);
		listParents.addListener(SWT.Selection, new ClickListener(this, listParents));
		sectionParents.setClient(sectionClientParents);
		btnGraph.addListener(SWT.Selection, new GraphListener(this.fanInController, listParents));
	
		String desc = "Methods set containing methods with biggest callers number." +
					  "\nTotal: " + itens.length;
		sectionParents.setDescription(desc);
		
	}

	/**
	 * Updates part of view containing methods caller.
	 * @param methodName Method to get callers.
	 */
	public void updateChildrenList(String methodName){
		TreeObject parent = MethodsView.stringToTreeObjectParents.get(methodName);
		String desc = "Method: " + methodName + 
					   "\nTotal callers: " + parent.getChildren().size();
		this.sectionChildren.setDescription(desc);
		
		String[] itens = new String[parent.getChildren().size()];
		for(int i=0; i<parent.getChildren().size(); i++){
			String label = parent.getChildren().get(i).getMethod().getDeclaringType().getElementName() + "." + parent.getChildren().get(i).getMethod().getElementName();
			itens[i] = label;
		}
		Arrays.sort(itens);
		listChildren.setItems(itens);
		String s = "Distinct classes: " + this.countNumberDiffClasses(methodName);
		label.setText(s);
		sectionChildren.setClient(sectionClientChildren);
	}

	/**
	 * Count number of distinct classes.
	 */
	private int countNumberDiffClasses(String methodName){
		TreeObject parent = MethodsView.stringToTreeObjectParents.get(methodName);
		Set<String> set = new HashSet<String>();
		for(int i=0; i<parent.getChildren().size(); i++){
			set.add(parent.getChildren().get(i).getMethod().getDeclaringType().getFullyQualifiedName());
		}
		return set.size();
	}

	public void updateToolBar(String message, int type){
		form.getToolBarManager().update(true);
		form.setMessage(message, type);
	}
	
	/**
	 * Inner class to implements listener of chart
	 * @author jlucasps
	 */
	class GraphListener implements Listener{

		FanInController fanInController;
		List list;
		
		public GraphListener(FanInController fanInController, List list){
			this.fanInController = fanInController;
			this.list = list;
		}
		
		public void handleEvent(Event event) {
			if(event.type == SWT.Selection){
				System.out.println( list.getItem(list.getSelectionIndex()) );
				
				TreeObject treeObject = MethodsView.stringToTreeObjectParents.get(list.getItem(list.getSelectionIndex()));
				
				// Start of other view manipulation
				if(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findViewReference(GraphView.ID_FANIN_GRAPH_VIEW) != null){
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().hideView(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findViewReference(GraphView.ID_FANIN_GRAPH_VIEW));
				}
				try {
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(GraphView.ID_FANIN_GRAPH_VIEW);
				} catch (PartInitException e) {
					log.error(e);
					e.printStackTrace();
				}
				GraphView graphView = (GraphView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(GraphView.ID_FANIN_GRAPH_VIEW);
				graphView.update(treeObject);
				// End of other view manipulation
				
				if(this.fanInController.getFanInAnalysis().getAllDeclaredMethods().keySet().contains(treeObject.getMethod())){
					if(this.fanInController.getMarkers().containsKey(treeObject.getMethod())){
						PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
						IMarker marker = this.fanInController.getMarkers().get(treeObject.getMethod());
						
						IFile file = (IFile) treeObject.getMethod().getDeclaringType().getResource();
						try {
							IDE.openEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(), file, true);
						} catch (PartInitException e) {
							log.error(e);
							e.printStackTrace();
						}
						IDE.gotoMarker(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor(), marker );
					}
					
				}else{
					String title = "FanIn Information!";
					String message = "The method " + treeObject.getMethod().getElementName() + " wasn't declared by you!";
					MessageDialog.openInformation(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), title, message);
				
				}
			}
		}
	}
	
	/**
	 * Inner class to implements listener of click in view
	 * @author jlucasps
	 */
	class ClickListener implements Listener{
		
		MethodsView methodsView;
		List listMethods;
		
		public ClickListener(MethodsView methodsView, List list){
			this.methodsView = methodsView;
			this.listMethods = list;
		}

		public void handleEvent(Event event) {	
			if(event.type == SWT.Selection){
				System.out.println(listMethods.getItem(listMethods.getSelectionIndex()));
				this.methodsView.updateChildrenList(listMethods.getItem(listMethods.getSelectionIndex()));
			}
		}
	}
	
	/**
	 * Disposes the toolkit
	 */
	public void dispose() {
		toolkit.dispose();
		super.dispose();
	}
}
