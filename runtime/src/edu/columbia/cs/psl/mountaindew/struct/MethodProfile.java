package edu.columbia.cs.psl.mountaindew.struct;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import edu.columbia.cs.psl.invivo.struct.MethodInvocation;
import edu.columbia.cs.psl.mountaindew.absprop.MetamorphicProperty;
import edu.columbia.cs.psl.mountaindew.absprop.MetamorphicProperty.PropertyResult;
import edu.columbia.cs.psl.mountaindew.util.MetaSerializer;

public class MethodProfile {
	
	private MethodInvocation ori;
	
	private MethodInvocation trans;
	
	private PropertyResult result;
	
	private boolean isValidCase;
	
	public static String interpretParams(Object params) {
		StringBuilder sBuilder = new StringBuilder();
		if (params.getClass().isArray()) {
			sBuilder.append("[");
			for (int i = 0; i < Array.getLength(params); i++) {
				//sBuilder.append(((Double)Array.get(params, i)).toString());
				//sBuilder.append(String.valueOf((Number)Array.get(params, i)));
				sBuilder.append(interpretParams(Array.get(params, i)));
				sBuilder.append(" ");
			}
			sBuilder.append("]");
		} else if (Collection.class.isAssignableFrom(params.getClass())) {
			Iterator paramIT = ((Collection)params).iterator();
			sBuilder.append("[");
			Object tmp;
			while(paramIT.hasNext()) {
				//sBuilder.append(((Double)paramIT.next()).toString());
				tmp = paramIT.next();
				
				if (Number.class.isAssignableFrom(tmp.getClass())) {
					sBuilder.append(String.valueOf((Number)tmp));
				} else {
					sBuilder.append(tmp.toString());
				}
				sBuilder.append(" ");
			}
			sBuilder.append("]");
		} else if (Number.class.isAssignableFrom(params.getClass())) {
			sBuilder.append("[");
			sBuilder.append(String.valueOf((Number)params));
			sBuilder.append("]");
		} else if (Map.class.isAssignableFrom(params.getClass())) {
			Map tmpMap = (Map)params;
			sBuilder.append("[");
			for (Object key: tmpMap.keySet()) {
				sBuilder.append("Key: " + interpretParams(key));
				sBuilder.append("Value: " + interpretParams(tmpMap.get(key)));
			}
			sBuilder.append("]");
		} else {
			sBuilder.append("[");
			sBuilder.append(params.toString());
			sBuilder.append("]");
		};
		
		return sBuilder.toString();
	}
	
	public static String delim = ",";
	
	public static String newLine = "\n";
	
	public MethodProfile(MethodInvocation ori, MethodInvocation trans, PropertyResult result) {
		this.ori = ori;
		this.trans = trans;
		this.result = result;
	}
	
	public void setIsValidCase() {
		try {
			if (Modifier.isStatic(trans.method.getModifiers())) {
				Map<String, Boolean> boolMap = (Map<String, Boolean>)trans.callee.getClass().getField("__meta_static_bool_map").get(trans.callee);
				String methodThreadName = MetaSerializer.describeMethod(trans.method) + trans.getThread().getId();
				this.isValidCase = boolMap.get(methodThreadName);
			} else {
				this.isValidCase = trans.callee.getClass().getField("__meta_valid_case").getBoolean(trans.callee);
			}
		} catch (Exception e) {
			System.err.println("Fail to set up validity in MethodProfile " + e);
		}
	}
	
	public MethodInvocation getOri() {
		return this.ori;
	}
	
	public MethodInvocation getTrans() {
		return this.trans;
	}
	
	public PropertyResult getResult() {
		return this.result;
	}
	
	/**
	 * Only transformed invocation has frontend transformer
	 * @return
	 */
	public String getFrontend() {
		return trans.getFrontend();
	}
	
	/**
	 * Only transformed invocation has backend checker
	 * @return
	 */
	public String getBackend() {
		return trans.getBackend();
	}
	
	public String getClassName() {
		return ori.callee.getClass().getName();
	}
	
	public ArrayList<String> getTransformedField() {
		return trans.getFieldRecord();
	}
	
	public boolean isValidCase() {
		return this.isValidCase;
	}
	
	public String toString() {
		StringBuilder sBuilder = new StringBuilder();
		sBuilder.append(this.ori.getMethod().getName() + delim);
		//Target on single input first
		if (ori.getParams().length > 0) {
			sBuilder.append(interpretParams(ori.getParams()[0]) + delim);
		} else {
			sBuilder.append("" + delim); 
		}

		if (ori.getReturnValue() != null) {
			sBuilder.append(interpretParams(ori.getReturnValue()) + delim);
		} else {
			sBuilder.append(interpretParams("") + delim);
		}
		
		if (trans.getParams().length > 0) {
			sBuilder.append(interpretParams(trans.getParams()[0]) + delim);
		} else {
			sBuilder.append("" + delim); 
		}

		if (trans.getReturnValue() != null) {
			sBuilder.append(interpretParams(trans.getReturnValue()) + delim);
		} else {
			sBuilder.append(interpretParams("") + delim);
		}

		sBuilder.append(trans.getFrontend() + delim);
		sBuilder.append(trans.getBackend() + delim);
		sBuilder.append(this.result.holds + newLine);
		return sBuilder.toString();
	}

}
