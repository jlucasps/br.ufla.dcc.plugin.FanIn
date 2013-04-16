package br.ufla.dcc.plugin.view.action;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ListSelectionDialog;

import br.ufla.dcc.plugin.controller.FanInController;
import br.ufla.dcc.plugin.view.views.MethodsView;

/**
 * This Class must be implemented to run plugin into Eclipse.
 * @author jlucasps
 *
 */
public class FanInActionDelegate implements IWorkbenchWindowActionDelegate {

	/** Logger for this class. */
	private static Logger log = Logger.getLogger(FanInActionDelegate.class);
	/** Workbench window. */
	private IWorkbenchWindow window;
	/** FanIn Controller. */
	private FanInController fanInController;
	/** Dialog to show messages. */
	ListSelectionDialog dialog;

	@Override
	public void run(IAction action) {
		try{
			// Get selected project
			IStructuredSelection selection = (IStructuredSelection)this.getWindow().getActivePage().getSelection("org.eclipse.jdt.ui.PackageExplorer");
			
			Object firstElement = selection.getFirstElement();
			// If it is a Java Project
			if (firstElement instanceof IJavaProject) {
			
				IJavaProject project = (IJavaProject) firstElement;

				// Show view MethodsView
				if(window.getActivePage().findViewReference(MethodsView.ID_FANIN_METHODS_VIEW) != null){
					window.getActivePage().hideView(window.getActivePage().findViewReference(MethodsView.ID_FANIN_METHODS_VIEW));
				}
				try {
					window.getActivePage().showView(MethodsView.ID_FANIN_METHODS_VIEW);
				} catch (PartInitException e) {
					log.error(e);
				}
				MethodsView methodsView = (MethodsView) window.getActivePage().findView(MethodsView.ID_FANIN_METHODS_VIEW);

				// Instantiate controller and execute FanIn analysis.
				this.fanInController = new FanInController();
				methodsView.setFanInController(this.fanInController);
				this.getFanInController().runFanIn(project);

				// Updates the view.
				methodsView.update();
				String message = "Analysis of " + project.getElementName() + " project finished successfully.";
				methodsView.updateToolBar(message , IMessageProvider.INFORMATION);
				
			} else {
				String title = "FanIn Analysis";
				String message = "Please, select one project on Package Explorer!";
				
				MessageDialog.openInformation(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), title, message);
				
			}
		}catch(Exception e){
			log.error(e);
			String title = "A fatal error occurred!";
			String message = "Cannot executes plugin in your environment.";
			MessageDialog.openInformation(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), title, message);
		}
		
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		// TODO Auto-generated method stub
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
	}

	@Override
	public void init(IWorkbenchWindow window) {
		// TODO Auto-generated method stub
		this.window = window;
		
	}

	public IWorkbenchWindow getWindow() {
		return window;
	}

	public void setWindow(IWorkbenchWindow window) {
		this.window = window;
		BasicConfigurator.configure();
		
	}

	public FanInController getFanInController() {
		return fanInController;
	}

	public void setFanInController(FanInController fanInController) {
		this.fanInController = fanInController;
	}
	
}
