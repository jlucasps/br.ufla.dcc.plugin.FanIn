package br.ufla.dcc.plugin.analysis.faninanalysis;

import java.util.HashMap;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.IMethodBinding;

public class OverriddenThread extends Thread {

	private IMethod method; 
	private IMethodBinding methodBinding;
	private HashMap<IMethod, IMethodBinding> chunk;
	HashMap<IMethod, IMethodBinding> overriddens;
	
	
	public OverriddenThread(HashMap<IMethod, IMethodBinding> chunck, IMethod method, IMethodBinding methodBinding){
		this.chunk = chunck;
		this.method = method;
		this.methodBinding = methodBinding;
		
		this.overriddens = new HashMap<IMethod, IMethodBinding>();
		
	}
	
	public void run(){
		/*Analisa todos os métodos do software*/
		for(IMethod m : this.getChunk().keySet()){
			/*Para cada método m do software, o seu binding 
			 * é armazenado em mb*/
			IMethodBinding mb = this.getChunk().get(m);
			
			/*Veririca se um método é uma sub assinatura do outro.
			 * Consultar seção 8.4.2 of The Java Language Specification, 
			 * Third Edition (JLS3) para mais informações */
			boolean isSubsignature = mb.isSubsignature(this.methodBinding) || this.methodBinding.isSubsignature(mb);
			
			/*Recupera as classes onde cada método está declarado*/
			IType methodType = (IType) this.method.getParent();
			IType mType = (IType) m.getParent();
			
			try{
				/*Recupera a hierarquia de cada uma das classes.*/
				ITypeHierarchy methodHierarchy = methodType.newSupertypeHierarchy(new NullProgressMonitor());
				ITypeHierarchy mHierarchy = mType.newSupertypeHierarchy(new NullProgressMonitor());
			
				/*Verifica se um método é uma sub assinatura do outro;
				 * se a classe de um dos métodos está na hierarquia de classes
				 *    do outro método e;
				 * se não está analisando para o mesmo método*/
				if( ( isSubsignature ) && 
					( methodHierarchy.contains(mType) || mHierarchy.contains(methodType) ) && 
					( !methodType.equals(mType) ) ){
					
					/*Caso estas condições sejam satisfeitas, o método m 
					 * e seu respectivo binding é armazenado no conjunto de 
					 * método que sobrescrevem ou são sobrescritos pelo
					 * parâmetro method*/
					this.overriddens.put(m, mb);
				}
				
			}catch(JavaModelException modelException){
				System.out.println(modelException);
			}
		}
	}

	public HashMap<IMethod, IMethodBinding> getChunk() {
		return chunk;
	}

	public void setChunk(HashMap<IMethod, IMethodBinding> chunck) {
		this.chunk = chunck;
	}

	public IMethod getMethod() {
		return method;
	}

	public void setMethod(IMethod method) {
		this.method = method;
	}

	public IMethodBinding getMethodBinding() {
		return methodBinding;
	}

	public void setMethodBinding(IMethodBinding methodBinding) {
		this.methodBinding = methodBinding;
	}

	public HashMap<IMethod, IMethodBinding> getOverriddens() {
		return overriddens;
	}

	public void setOverriddens(HashMap<IMethod, IMethodBinding> overriddens) {
		this.overriddens = overriddens;
	}
	
	
	
}
