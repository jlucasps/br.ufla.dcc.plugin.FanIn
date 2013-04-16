package br.ufla.dcc.plugin.model.analysis.faninanalysis;

import java.util.ArrayList;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodInvocation;
/**
 * Pattern Visitor provided by Eclipse API
 * @author jlucasps
 *
 */
public class InvocationVisitor extends ASTVisitor {

	ArrayList<MethodInvocation> invocations = new ArrayList<MethodInvocation>();
	
	@Override
	public boolean visit(MethodInvocation node){
		this.invocations.add(node);
		return super.visit(node);
	}

	public ArrayList<MethodInvocation> getInvocations() {
		return invocations;
	}

	public void setInvocations(ArrayList<MethodInvocation> invocations) {
		this.invocations = invocations;
	}
	
}
