package br.ufla.dcc.plugin.controller;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import br.ufla.dcc.plugin.model.analysis.faninanalysis.FanIn;
import br.ufla.dcc.plugin.model.analysis.faninanalysis.InvocationVisitor;

/**
 * This class executes Fan In Analysis.
 * @author jlucasps
 */
public class FanInAnalysis extends Thread{

	/** Logger for this class. */
	private static Logger log = Logger.getLogger(FanInAnalysis.class);
	
	/** FanIn of called methods. */
	private ArrayList<FanIn> fanInsLevelOne;
	/** FanIn calculated by hierarchy. */
	private ArrayList<FanIn> fanInsLevelTwo;
	/** FanIn of all methods. */
	private ArrayList<FanIn> fanInsTotalized;
	/** Filtered FanIns. */
	private ArrayList<FanIn> fanInsFiltered;

	/** All declared methods. */
	private HashMap<IMethod, IMethodBinding> allDeclaredMethods;
	/** Java project been analyzed.*/
	private IJavaProject project;
	/** Percentage value to be used on filtering stage.*/
	private float percent;
	/** System classes set. */
	private Set<TypeDeclaration> systemClasses;

	/**
	 * Constructor.
	 * @param project Project to analyze.
	 * @param percent Percentage of filtered methods.
	 */
	public FanInAnalysis(IJavaProject project, float percent) {
		this.percent = percent;
		this.project = project;
		this.fanInsLevelOne = new ArrayList<FanIn>();
		this.fanInsLevelTwo = new ArrayList<FanIn>();
		this.fanInsTotalized = new ArrayList<FanIn>();
		this.allDeclaredMethods = new HashMap<IMethod, IMethodBinding>();
	}

	/**
	 * Start fanIn.
	 */
	@Override
	public void run(){
		try{
			this.startFanIn();
		}catch(JavaModelException modelException){
			log.error(modelException.getMessage());
		}catch(CoreException coreException){
			log.error(coreException.getMessage());
		}
	}

	/**
	 * Start FanIn Analysis.
	 * @throws CoreException In case of CoreException.
	 * @throws JavaModelException In case of JavaModelException.
	 */
	public void startFanIn() throws CoreException,
			JavaModelException {
		this.systemClasses = new HashSet<TypeDeclaration>();
		log.info("Analysis started: " + Calendar.getInstance().getTime());
		
		long begin = System.currentTimeMillis();
		this.readProjectInfo(this.project);
		long end = System.currentTimeMillis();
		
		log.info("Number of classes: " + systemClasses.size());
		log.info("Number of methods: " + this.getAllDeclaredMethods().size());
		log.info("Execution time: " + (end - begin) / 1000);
		
		log.info("Analysis finished successfully: " + Calendar.getInstance().getTime());
	}

	/**
	 * Read source code looking for called methods.
	 * @param javaProject Java project to analyze.
	 * @throws JavaModelException In case of JavaModelException.
	 */
	private void readProjectInfo(IJavaProject javaProject)
			throws JavaModelException {
		IPackageFragment[] packages = javaProject.getPackageFragments();

		log.info("Analysing called methods...");
		for (IPackageFragment mypackage : packages) {
			// Analyzes only Java source code, not considering binaries
			if (mypackage.getKind() == IPackageFragmentRoot.K_SOURCE) {
				log.info("Package: "+ mypackage.getElementName());
				//Analyzes CompilationUnits of current package
				this.readIPackageInfo(mypackage);
			}
		}
		this.printMethods(FanIn.LEVEL_ONE);
		
		log.info("Analysing overridden methods...");
		this.checkOverriddens();
		this.printMethods(FanIn.LEVEL_TWO);
		
		log.info("Totalizing FanIn values...");
		this.totalizeFanIns();
		this.printMethods(FanIn.LEVEL_TOTALIZED);
		
		log.info("Sorting FanIn...");
		this.sortFanIns();
		log.info("Filtering FanIn...");
		this.filterFanIns();
	}

	/**
	 * Analyzes CompilationUnits
	 * @param mypackage Package to read CompilationUnits from.
	 * @throws JavaModelException
	 */
	private void readIPackageInfo(IPackageFragment mypackage)
			throws JavaModelException {

		// For each CompilationUnit of package
		for (ICompilationUnit unit : mypackage.getCompilationUnits()) {
			log.info("CompilationUnit: " + unit.getElementName());
			// Parsing
			CompilationUnit compUnitParsed = this.parseCompilationUnit(unit);
			// For each Class of CompilationUnit
			for (Object obj : compUnitParsed.types()) {
				// Since compUnitParsed.types() is called, it can return objects of
				// type AnnotationTypeDeclaration, EnumDeclaration and TypeDeclaration.
				// The if() below consider only type TypeDeclaration
				if(obj instanceof TypeDeclaration ){
					TypeDeclaration type = (TypeDeclaration) obj;
					log.info("\tType: " + type.getName());
					this.systemClasses.add(type);
					this.readTypeInfo(type);
				}
			}
		}
	}

	/**
	 * Read informations of a TypeDeclaration object
	 * @param type Type to analyze
	 */
	private void readTypeInfo(TypeDeclaration type){
		// For each method and constructor declared in Type <code>type</code>
		for (Object ob : type.getMethods()) {
			MethodDeclaration callerDeclaration = (MethodDeclaration) ob;
			log.info("\t\tMethod body: " + callerDeclaration.getName() + " { ");
			IMethodBinding callerBinding = callerDeclaration.resolveBinding();
			if(callerBinding != null){
				IMethod callerMethod = (IMethod) callerBinding.getJavaElement();
				// Store every method and its bidings in a hashmap
				this.getAllDeclaredMethods().put(callerMethod, callerBinding);
				// Visitor Pattern implemented by Eclipse API
				InvocationVisitor visitor = new InvocationVisitor();
				// Visitor executed on body method do recovery methods invocations.
				callerDeclaration.accept(visitor);
				// For each method in callerDeclaraion
				for (MethodInvocation invoked : visitor.getInvocations()) {
					this.addCallerToMethodInvocation(invoked, callerMethod, callerBinding);
				}
			}
			log.info(" } ");
		}
	}
	
	/**
	 * Add a caller method to a method invocation. I.e, when a method is called,
	 * the method caller must be stored in its caller set.
	 * @param invoked Called method.
	 * @param callerMethod Caller methods
	 * @param callerBinding Caller binding.
	 */
	private void addCallerToMethodInvocation(MethodInvocation invoked, IMethod callerMethod, IMethodBinding callerBinding){
		// Resolve binding o called method.
		IMethodBinding invokedBinding = invoked.resolveMethodBinding();
		if(invokedBinding != null){
			if(invokedBinding.getJavaElement() instanceof IMethod){
				IMethod invokedMethod = (IMethod) invokedBinding.getJavaElement();
				log.info(invokedMethod.getParent().getElementName() + "." + invokedMethod.getElementName() + " ");
				this.addCallerToMethod(invokedMethod, invokedBinding, callerMethod, callerBinding, FanIn.LEVEL_ONE);
			}
		}
	}

	/**
	 * Add a caller to method.
	 * @param invokedMethod Called method.
	 * @param invokedBinding Called method binding.
	 * @param callerMethod Caller method.
	 * @param callerBinding Caller method binding.
	 * @param fanInLevel Adda caller in a FanIn level
	 */
	private void addCallerToMethod(IMethod invokedMethod, IMethodBinding invokedBinding, IMethod callerMethod, IMethodBinding callerBinding, int fanInLevel){
		// Check if FanIn list contains called method.
		if (this.fanInListcontains(invokedMethod, fanInLevel)) {
			// Get callers of a called method.
			HashMap<IMethod, IMethodBinding> callers = this.getCallerMethodsOf(invokedMethod, fanInLevel);
			// Method which body it is being analyzed is set into map
			callers.put(callerMethod, callerBinding);
		} else {
			// If this is the first time a method it is being called,
			// a new FanIn object is instantiated and its attributes is set.
			// Furthermore, the method which body it is being analyzed is set
			// into set of caller method of called method.
			FanIn fanIn = new FanIn();
			fanIn.setInvokedMethod(invokedMethod);
			fanIn.setInvokedBinding(invokedBinding);
			fanIn.getCallers().put(callerMethod, callerBinding);
			this.getFanInsByLevel(fanInLevel).add(fanIn);
		}
	}

	/**
	 * Print methods of a level
	 * @param fanInLevel Level to print methods.
	 */
	private void printMethods(int fanInLevel){
		log.info("\n=================================================================================");
		log.info("LEVEL " + fanInLevel);
		for(FanIn fanIn : this.getFanInsByLevel(fanInLevel)){
			log.info("\nInvocated:" + fanIn.getInvokedMethod().getDeclaringType().getElementName() + 
												"." +fanIn.getInvokedMethod().getElementName());
			if(this.fanInListcontains(fanIn.getInvokedMethod(), fanInLevel)){
				HashMap<IMethod, IMethodBinding> callers = this.getCallerMethodsOf(fanIn.getInvokedMethod(), fanInLevel);
				Iterator<IMethod> it = callers.keySet().iterator();
				log.info("\t " + callers.size()+ " Callers: ");
				while(it.hasNext()){
					IMethod method = (IMethod) it.next();
					log.info( method.getDeclaringType().getElementName() +"."+ method.getElementName() + " ");
				}
			}
		}
		log.info("\n=================================================================================");
	}
	
	/**
	 * Check if a calling method exists in FanIn list
	 * @param method Method to check
	 * @param fanInLevel FanIn level.
	 * @return <code>true</code> case exists, false otherwise.
	 */
	private boolean fanInListcontains(IMethod method, int fanInLevel){
		for(FanIn fanIn : this.getFanInsByLevel(fanInLevel)){
			if(fanIn.getInvokedMethod().isSimilar(method) && 
					fanIn.getInvokedMethod().getDeclaringType().equals(method.getDeclaringType())){
				return true;
			}
		}
		return false;
	}

	/**
	 * Return the caller methods of a method
	 * @param invokedMethod methods to return callers
	 * @param fanInLevel FanIn level.
	 * @return Map of caller methods. key: method caller; value: binding of a method caller.
	 */
	private HashMap<IMethod, IMethodBinding> getCallerMethodsOf( IMethod invokedMethod, int fanInLevel ){
		for(FanIn fanIn : this.getFanInsByLevel(fanInLevel)){
			if(fanIn.getInvokedMethod().isSimilar(invokedMethod) && 
					fanIn.getInvokedMethod().getDeclaringType().equals(invokedMethod.getDeclaringType())){
				return fanIn.getCallers();
			}
		}
		return null;
	}
	
	/**
	 * Return methods which overrides <code>method</code> or are overridden by it.
	 * @param method A method
	 * @return HashMap of Method and MethodBinding of that methods that are in hierarchy of <code>method</code>
	 */
	private HashMap<IMethod, IMethodBinding> getOverriddensOf(IMethod method){
		HashMap<IMethod, IMethodBinding> allOverriddens = new HashMap<IMethod, IMethodBinding>();
		// Select only methods written by developer
		if(this.getAllDeclaredMethods().containsKey(method)){

			// Get binding of a method
			IMethodBinding methodBinding = this.getAllDeclaredMethods().get(method);
			try{
				// Get the class where method was declared.
				IType methodType = (IType) method.getParent();
				// Get class hierarchy
				ITypeHierarchy methodHierarchy = methodType.newSupertypeHierarchy(new NullProgressMonitor());
				
				// For each declared method
				for(IMethod m : this.getAllDeclaredMethods().keySet()){
					IMethodBinding mb = this.getAllDeclaredMethods().get(m);

					// Check if method is a subsignature of other
					// See Section 8.4.2 of The Java Language Specification Third Edition (JLS3)
					// for further informations.
					boolean isSubsignature = mb.isSubsignature(methodBinding) || methodBinding.isSubsignature(mb);
					// Get the class where method was declared.
					IType mType = (IType) m.getParent();
					// Get class hierarchy
					ITypeHierarchy mHierarchy = mType.newSupertypeHierarchy(new NullProgressMonitor());
				
					// Check:
					// if a method is a subsignature of other,
					// if a class of a method it is in the same hierarchy of other method,
					// if it is not the current method.
					if( ( isSubsignature ) && 
						( methodHierarchy.contains(mType) || mHierarchy.contains(methodType) ) && 
						( !methodType.equals(mType) ) ){
						// If conditions are satisfied, the method is stored on map.
						allOverriddens.put(m, mb);
					}
				}
			}catch(JavaModelException modelException){
				log.error(modelException);
				return null;
			}
		}
		return allOverriddens;
	}

	/**
	 * Check for overridden methods
	 */
	private void checkOverriddens(){
		
		Iterator<IMethod> it = this.getAllDeclaredMethods().keySet().iterator();
		// Iterate over all declared methods.
		while(it.hasNext()){
			IMethod method = it.next();
			log.info("\nMethod: " + method.getDeclaringType().getElementName() + "." + method.getElementName() + ": ");
			
			if(this.fanInListcontains(method, FanIn.LEVEL_ONE)){
				HashMap<IMethod, IMethodBinding> methodCallers = this.getCallerMethodsOf(method, FanIn.LEVEL_ONE);
				HashMap<IMethod, IMethodBinding> methodOverriddens = this.getOverriddensOf(method);
				
				for (IMethod overridden : methodOverriddens.keySet()) {
					Iterator<IMethod> mIt = methodCallers.keySet().iterator();
					while(mIt.hasNext()){
						IMethod m = mIt.next();
						// Add a caller to a method.
						this.addCallerToMethod(overridden, methodOverriddens.get(overridden), m, methodCallers.get(m), FanIn.LEVEL_TWO);
					}
				}
			}
		}
	}

	/**
	 * Totalize the FanIn number.
	 */
	private void totalizeFanIns(){
		// Sum level one.
		for(FanIn fanIn : this.getFanInsByLevel(FanIn.LEVEL_ONE)){
			Iterator<IMethod> it = fanIn.getCallers().keySet().iterator();
			while(it.hasNext()){
				IMethod callerMethod = (IMethod) it.next();
				IMethodBinding callerBinding = fanIn.getCallers().get(callerMethod);
				
				this.addCallerToMethod(fanIn.getInvokedMethod(), fanIn.getInvokedBinding(), callerMethod, callerBinding, FanIn.LEVEL_TOTALIZED);
			}
			
		}
		// sum level two
		for(FanIn fanIn : this.getFanInsByLevel(FanIn.LEVEL_TWO)){
			Iterator<IMethod> it = fanIn.getCallers().keySet().iterator();
			while(it.hasNext()){
				IMethod callerMethod = (IMethod) it.next();
				IMethodBinding callerBinding = fanIn.getCallers().get(callerMethod);
				
				this.addCallerToMethod(fanIn.getInvokedMethod(), fanIn.getInvokedBinding(), callerMethod, callerBinding, FanIn.LEVEL_TOTALIZED);
			}	
		}
		
	}

	/**
	 * Sort FanIns
	 */
	private void sortFanIns(){
		for(FanIn fanIn : this.getFanInsByLevel(FanIn.LEVEL_TOTALIZED)){
			fanIn.setCount(fanIn.getCallers().size());
		}
		Collections.sort(this.getFanInsByLevel(FanIn.LEVEL_TOTALIZED));
	}

	/**
	 * Filter FanIns considering percentage value.
	 */
	public void filterFanIns(){
		float limit = this.getFanInsByLevel(FanIn.LEVEL_TOTALIZED).size() * (this.percent / 100);
		if(limit < 5){
			limit++;
		}
		this.fanInsFiltered = new ArrayList<FanIn>( (int) limit );
		for(int i=0; i<limit; i++){
			this.fanInsFiltered.add(i, this.getFanInsByLevel(FanIn.LEVEL_TOTALIZED).get(this.getFanInsByLevel(FanIn.LEVEL_TOTALIZED).size() -i -1));
		}
		for(int i=0; i<this.getFanInsFiltered().size(); i++){
			log.info(this.getFanInsFiltered().get(i).getCount() + " " +
					   this.getFanInsFiltered().get(i).getInvokedMethod().getDeclaringType().getElementName() + 
					   "."+this.getFanInsFiltered().get(i).getInvokedMethod().getElementName());
		}
	}
	
	/**
	 * Executes parsing.
	 * @param unit CompilationUnit to parse.
	 * @return Parsed CompilationUnit.
	 */
	public CompilationUnit parseCompilationUnit(ICompilationUnit unit) {
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(unit);
		parser.setResolveBindings(true);
		return (CompilationUnit) parser.createAST(null); // parse
	}

	/**
	 * Return a method declaration of a method.
	 * @param method Method to get declaration.
	 * @param unit CompilationUnit of method.
	 * @return Method declaration
	 */
	public MethodDeclaration getMethodDeclaration(IMethod method, CompilationUnit unit){
		
		for (Object obj : unit.types()) {
			// Get only TypeDeclaration, not considering AnnotationTypeDeclaration and EnumDeclaration 
			if(obj instanceof TypeDeclaration ){
				TypeDeclaration type = (TypeDeclaration) obj;
				if(type.getName().toString().equals(method.getDeclaringType().getElementName())){

					for (Object ob : type.getMethods()) {
						MethodDeclaration methodDeclaration = (MethodDeclaration) ob;
						if(method.getElementName().equals(methodDeclaration.getName().toString())){
							return methodDeclaration;
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * Return FanIns by level.
	 * @param level Desired level.
	 * @return List of FanIns
	 */
	private ArrayList<FanIn> getFanInsByLevel(int level) {
		if(level == FanIn.LEVEL_ONE){
			return this.fanInsLevelOne;
		}else if (level == FanIn.LEVEL_TWO){
			return this.fanInsLevelTwo;
		}else if (level == FanIn.LEVEL_TOTALIZED){
			return this.fanInsTotalized;
		}else {
			log.info("Level " + level + " not supported.");
			return null;
		}
	}

	// Getters and Setters.
	public ArrayList<FanIn> getFanInsFiltered() {
		return fanInsFiltered;
	}

	public void setFanInsFiltered(ArrayList<FanIn> fanInsFiltered) {
		this.fanInsFiltered = fanInsFiltered;
	}

	public HashMap<IMethod, IMethodBinding> getAllDeclaredMethods() {
		return allDeclaredMethods;
	}

	public void setAllDeclaredMethods(HashMap<IMethod, IMethodBinding> allMethods) {
		this.allDeclaredMethods = allMethods;
	}

	public IJavaProject getProject() {
		return project;
	}

	public void setProject(IJavaProject project) {
		this.project = project;
	}
	
}
