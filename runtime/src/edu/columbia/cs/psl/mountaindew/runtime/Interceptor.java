package edu.columbia.cs.psl.mountaindew.runtime;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import com.rits.cloning.Cloner;

import edu.columbia.cs.psl.invivo.runtime.AbstractInterceptor;
import edu.columbia.cs.psl.invivo.struct.MethodInvocation;
import edu.columbia.cs.psl.mountaindew.property.MetamorphicProperty;
import edu.columbia.cs.psl.mountaindew.property.MetamorphicProperty.PropertyResult;


/**
 * Each intercepted object will have its _own_ Interceptor instance.
 * That instance will stick around for the lifetime of the intercepted object.
 * 
 * NB if you want to keep a list of these Interceptors somewhere statically,
 * you probably want to use a WeakHashMap so as to not create memory leaks
 * 
 * @author jon
 *
 */
public class Interceptor extends AbstractInterceptor {
	private HashMap<Method, HashSet<MetamorphicProperty>> properties = new HashMap<Method, HashSet<MetamorphicProperty>>();
	private HashSet<Class<? extends MetamorphicProperty>> propertyPrototypes;
	private HashMap<Integer, MethodInvocation> invocations = new HashMap<Integer, MethodInvocation>();
	private Integer invocationId = 0;
	private Cloner cloner = new Cloner();
	
	public Interceptor(Object intercepted) {
		super(intercepted);
		System.out.println("Interceptor created");
		propertyPrototypes = MetamorphicObserver.getInstance().registerInterceptor(this);
	}
	
	public int onEnter(Object callee, Method method, Object[] params)
	{
		if(isChild(callee))
			return -1;
		int retId = 0;
		synchronized(invocationId)
		{
			invocationId++;
			retId = invocationId;
		}
		if(!properties.containsKey(method))
		{
			properties.put(method, new HashSet<MetamorphicProperty>());
			for(Class<? extends MetamorphicProperty> c : propertyPrototypes)
			{
				try {
					MetamorphicProperty p = c.newInstance();
					p.setMethod(method);
					properties.get(method).add(p);
				} catch (InstantiationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}
		
		MethodInvocation inv = new MethodInvocation();
		//inv.params = params;
		inv.params = cloner.deepClone(params);
		inv.method = method;
		inv.callee = getInterceptedObject();
		invocations.put(retId, inv);
		ArrayList<MethodInvocation> children = new ArrayList<MethodInvocation>();
		for(MetamorphicProperty p : properties.get(inv.method))
		{
			for(MethodInvocation child : p.createChildren(inv))
			{
				child.callee = deepClone(inv.callee);
				child.method = inv.method;
				children.add(child);
				child.thread = createChildThread(child);
				child.thread.start();
			}
		}
		inv.children = new MethodInvocation[children.size()];
		inv.children = children.toArray(inv.children);
		return retId;
	}
	
	public void onExit(Object val, int op, int id)
	{
		if(id < 0)
			return;
		MethodInvocation inv = invocations.remove(id);
		inv.returnValue = val;
		
		/*for (int i = 0; i < Array.getLength(val); i++) {
			System.out.println("On Exit check return value: " + (Number)Array.get(val, i));
		}*/
				
		for(MethodInvocation inv2 : inv.children)
		{
			try {
//				System.out.println("Children goes first");
				inv2.thread.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			for(MetamorphicProperty p : properties.get(inv2.method))
			{
				p.logExecution(inv2);
			}
		}
		
		for(MetamorphicProperty p : properties.get(inv.method))
		{
//			System.out.println("Then parents");
			p.logExecution(inv);
		}
	}
	
	public void reportPropertyResults()
	{
		for(Method m : properties.keySet())
		{
			System.out.println(m);
			for(MetamorphicProperty p : properties.get(m))
			{
				PropertyResult r = p.propertyHolds();
				System.out.println(r);
			}

		}
	}

} 

