package br.ufla.dcc.plugin.model.analysis.faninanalysis;

import java.util.HashMap;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.dom.IMethodBinding;

/**
 * FanIn Object.
 * @author jlucasps
 */
public class FanIn implements Comparable<FanIn>{

	/** Number of callers methods. */
	private int count;
	/** IMethod instance for the current method. */
	private IMethod invokedMethod;
	/** IMethodBinding instance for the current method. */
	private IMethodBinding invokedBinding;
	/** Callers of current Method. */
	private HashMap<IMethod, IMethodBinding> callers;
	/** Constant to represent FanIn Level 1 (invoked) */
	public static final int LEVEL_ONE = 1;
	/** Constant to represent FanIn Level 2 (hierarchical) */
	public static final int LEVEL_TWO = 2;
	/** Constant to represent all FanIns*/
	public static final int LEVEL_TOTALIZED = 3;
	
	public FanIn(){
		this.callers = new HashMap<IMethod, IMethodBinding>();
	}
	
	public HashMap<IMethod, IMethodBinding> getCallers() {
		return callers;
	}
	
	public void setCallers(HashMap<IMethod, IMethodBinding> callers) {
		this.callers = callers;
	}
	
	public IMethod getInvokedMethod() {
		return invokedMethod;
	}
	
	public void setInvokedMethod(IMethod invokedMethod) {
		this.invokedMethod = invokedMethod;
	}
	
	public IMethodBinding getInvokedBinding() {
		return invokedBinding;
	}

	public void setInvokedBinding(IMethodBinding binding) {
		this.invokedBinding = binding;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	@Override
	public int compareTo(FanIn o) {
		FanIn other = (FanIn) o;
		return this.getCount() - other.getCount();
	}
}
