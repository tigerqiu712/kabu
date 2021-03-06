package edu.columbia.cs.psl.mountaindew.example;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.sun.xml.internal.ws.org.objectweb.asm.Type;

import edu.columbia.cs.psl.metamorphic.inputProcessor.MetamorphicInputProcessor;
import edu.columbia.cs.psl.metamorphic.inputProcessor.impl.MultiplyByNumericConstant;
import edu.columbia.cs.psl.metamorphic.runtime.MetamorphicInputProcessorGroup;
import edu.columbia.cs.psl.mountaindew.absprop.MetamorphicProperty;
import edu.columbia.cs.psl.mountaindew.adapter.AbstractAdapter;
import edu.columbia.cs.psl.mountaindew.adapter.DefaultAdapter;
import edu.columbia.cs.psl.mountaindew.util.TransformPlugin;

public class MyObject2 {
	
	public static HashSet<Class<? extends MetamorphicInputProcessor>> nonValueChangeProcessors = 
			MetamorphicInputProcessorGroup.getInstance().getNonValueChangeProcessors();
	
	//Key method name + thread id, Val processor
	public static HashMap<String, MetamorphicInputProcessor> __meta_static_p_map = new HashMap<String, MetamorphicInputProcessor>();
	
	//Key method name + thread id, Val adapter
	public static HashMap<String, AbstractAdapter> __meta_static_a_map = new HashMap<String, AbstractAdapter>();
	
	//Key method name + thread id, Val Object param[]
	public static HashMap<String, Object[]> __meta_static_param_map = new HashMap<String, Object[]>();
	
	//Key method name + thread id, Val Map<Static field name, Boolean>
	public static HashMap<String, HashMap<String, Boolean>> __meta_static_bool_map = new HashMap<String, HashMap<String, Boolean>>();
	
	//Key method name + thread id, Val valid case
	public static HashMap<String, Boolean> __meta_static_valid_map = new HashMap<String, Boolean>();
	
	public static Map testStaticMap = new HashMap();
	
	public AbstractAdapter adapter;
	
	public MetamorphicInputProcessor processor;
	
	public Object[] params;
	
	private int[] a = new int[]{1, 2, 3};
	
	public boolean __meta_should_trans_a = true;
	
	public List b = new ArrayList();
	
	public boolean __meta_should_trans_b = false;
	
	private Map c = new HashMap();
	
	public boolean __meta_should_trans_c = false;
	
	public boolean __meta_valid_case = true;
	
	public List reflectTest = new ArrayList();
	
	public static List fakeData = new ArrayList();
	
	public int returnInt(int abc) {
		Iterator it = b.iterator();
		if(__meta_should_trans_b) {
			b = __metamorphic_process(b);
			it = b.iterator();
		}
		
		int sum = 0;
		while(it.hasNext()) {
			sum = sum + ((int)it.next());
		}
		
		System.out.println("Check b: " + b);
		
		return sum;
	}
	
	public Map returnMap(Map input) {
		if (this.__meta_should_trans_a) {
			this.c = __metamorphic_process(this.c);
			System.out.println("Check input after transformation: " + input);
			System.out.println("Check case validity: " + __meta_valid_case);
		}
		
		this.__meta_should_trans_b = true;
			
		Map newMap = new HashMap();
		for (Object key: input.keySet()) {
			Object val = new Integer(1);
			newMap.put(key, val);
		}
		
		return newMap;
	}
	
	public void testIfElse(boolean t) {
		if (t) {
			System.out.println("It's true");
		} else {
			System.out.println("It's false");
		}
	}
	
	public int returnInt() {
		int a = 3;
		return a;
	}
	
	public int returnIntAdd3(int a) {
		return a + 3;
	}
	
	public int[] returnIntArray() {
		return new int[3];
	}
	
	public Integer returnInteger(Integer a, Integer b) {
		return a;
	}
	
	public ArrayList returnList() {
		return new ArrayList();
	}
	
	public int[] returnArray() {
		return new int[3];
	}
	
	public Object[] toArray(Object[] array) {
		return new Object[3];
	}
	
	public Object[] toArray() {
		return new Object[3];
	}
	
	public Object[] testToArray(List items) {
		return new Object[3];
	}
	
	public List testToList(Object[] tmp) {
		return new ArrayList();
	}
	
	public void doNothing() {
		
	}
	
	public double returnDouble() {
		return 2.0;
	}
	
	public <T> T __metamorphic_process(T obj) {
		try {
			if(this.adapter != null && this.processor != null)
				return TransformPlugin.__meta_transform_basic(obj, this.adapter, this.processor, this.params);
		} catch (Exception ex) {
			System.out.println(ex);
			System.out.println("Change valid case to false: " + obj);
			this.__meta_valid_case = false;
			return obj;
		}
		System.out.println("Empty adapter or processor");
		return obj;
	}
	
	public void testTryCatch() {
		try {
			double a = 2/5;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public static void fakeStaticMethod() {
		try {
			fakeData = __metamorphic_static_process(fakeData, "fakeStaticMethod", "fakeData");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		fakeData.add(5);
	}
	
	public static <T> T __metamorphic_static_process(T obj, String methodName, String fieldName) {		
		long id = Thread.currentThread().getId();
		String key = methodName + id;
		
		Map<String, Boolean> fieldBoolMap = __meta_static_bool_map.get(key);
		
		if (fieldBoolMap.get(fieldName)) {
			AbstractAdapter adapter = __meta_static_a_map.get(key);
			MetamorphicInputProcessor processor = __meta_static_p_map.get(key);
			Object[] params = __meta_static_param_map.get(key);
			try {
				return TransformPlugin.__meta_transform_basic(obj, adapter, processor, params);
			} catch (Exception ex) {
				System.out.println(ex);
				System.out.println("Change valid case to false: " + obj);
				
				__meta_static_valid_map.put(key, false);
			}
		}
		return obj;
	}
	
	public static void main(String args[]) {
		MyObject2 mo2 = new MyObject2();
		mo2.processor = new MultiplyByNumericConstant();
		mo2.adapter = new DefaultAdapter();
		mo2.params = new Object[]{5};
		/*Map<String, Integer> input2 = new HashMap<String, Integer>();
		input2.put("a", 1);
		input2.put("b", 2);
		System.out.println("Check map2: " + mo2.returnMap(input2));*/
		System.out.println(mo2.__metamorphic_process(2));
		
		MyObject2 mo = new MyObject2();
		
		mo.b.add(1);
		mo.b.add(2);
		mo.b.add(3);
		
		mo.processor = new MultiplyByNumericConstant();
		mo.adapter = new DefaultAdapter();
		mo.params = new Object[]{5};
		
		Map<String, Object> input = new HashMap<String, Object>();
		input.put("a", new Object());
		input.put("b", new Object());
		System.out.println("Check map: " + mo.returnMap(input));
		
		try {
			List reflectList = (List)mo.getClass().getField("reflectTest").get(mo);
			reflectList.add(28);
			System.out.println("After reflect setting: " + mo.reflectTest);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
