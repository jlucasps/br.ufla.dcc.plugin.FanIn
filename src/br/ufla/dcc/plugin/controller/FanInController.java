package br.ufla.dcc.plugin.controller;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import br.ufla.dcc.plugin.model.analysis.faninanalysis.FanIn;

public class FanInController{

	/** Logger for this class. */
	private static Logger log = Logger.getLogger(FanInController.class);
	
	/** Id of High FanIn marker. */
	private static String ID_HIGH_FANIN_MARKER = "br.ufla.dcc.plugin.view.highFanInMarker";

	/** FanIn Analysis instance. */
	private FanInAnalysis fanInAnalysis;
	/** Markers of Java Editor. */
	private HashMap<IMethod, IMarker> markers;

	public FanInController(){
		this.markers = new HashMap<IMethod, IMarker>();
	}

	/**
	 * Execute FanIn analysis.
	 * @param javaProject
	 */
	public void runFanIn(IJavaProject javaProject){
		this.fanInAnalysis = new FanInAnalysis(javaProject, 5f);
		
		this.fanInAnalysis.start();
		try {
			this.fanInAnalysis.join();
		} catch (InterruptedException e) {
			System.out.println(e);
			e.printStackTrace();
		}
		//Create markes on Java Editor.
		this.createMarkers();
	}

	/**
	 * Create Markers on Java Editor.
	 */
	public void createMarkers(){
		for(FanIn fanIn: this.getFanInAnalysis().getFanInsFiltered()){
			
			IMethod method = fanIn.getInvokedMethod();
			if(this.getFanInAnalysis().getAllDeclaredMethods().keySet().contains(method)){
				try {
					IFile file = (IFile) method.getCompilationUnit().getCorrespondingResource();
				
					IMarker marker = file.createMarker(FanInController.ID_HIGH_FANIN_MARKER);
				
					marker.setAttribute(IMarker.MESSAGE, "Method with high FanIn value: "+ fanIn.getCount() );
					marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);

					CompilationUnit unit = this.getFanInAnalysis().parseCompilationUnit(method.getCompilationUnit());
					MethodDeclaration methodDeclaration = this.getFanInAnalysis().getMethodDeclaration(method, unit);
					
					ASTNode node = (ASTNode) methodDeclaration;

					marker.setAttribute(IMarker.CHAR_START, node.getStartPosition());
					marker.setAttribute(IMarker.CHAR_END, node.getStartPosition() + node.getLength());
					
					marker.setAttribute(IMarker.LINE_NUMBER, unit.getLineNumber(node.getStartPosition()));
					
					this.markers.put(method, marker);
					
				} catch (CoreException e) {
					log.error(e);
					e.printStackTrace();
				}
			}
		}
	}
	
	// Getters and Setters.
	public FanInAnalysis getFanInAnalysis() {
		return fanInAnalysis;
	}

	public void setFanInAnalysis(FanInAnalysis fanInAnalysis) {
		this.fanInAnalysis = fanInAnalysis;
	}

	public HashMap<IMethod, IMarker> getMarkers() {
		return markers;
	}

	public void setMarkers(HashMap<IMethod, IMarker> markers) {
		this.markers = markers;
	}
	
	
}
