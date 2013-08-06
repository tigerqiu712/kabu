package edu.columbia.cs.psl.mountaindew.runtime;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;

import com.rits.cloning.Cloner;

import edu.columbia.cs.psl.invivo.runtime.AbstractInterceptor;
import edu.columbia.cs.psl.invivo.struct.MethodInvocation;
import edu.columbia.cs.psl.metamorphic.inputProcessor.MetamorphicInputProcessor;
import edu.columbia.cs.psl.metamorphic.runtime.MetamorphicInputProcessorGroup;
import edu.columbia.cs.psl.mountaindew.absprop.MetamorphicProperty;
import edu.columbia.cs.psl.mountaindew.absprop.MetamorphicProperty.PropertyResult;
import edu.columbia.cs.psl.mountaindew.adapter.AbstractAdapter;
import edu.columbia.cs.psl.mountaindew.adapter.AdapterLoader;
import edu.columbia.cs.psl.mountaindew.stats.Correlationer;
import edu.columbia.cs.psl.mountaindew.struct.MethodProfile;
import edu.columbia.cs.psl.mountaindew.util.MetamorphicConfigurer;


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
	private static String header = 
			"Method name,ori_input,ori_output,trans_input,trans_output,frontend_transformer,backend_checker,Holds\n";
	private static String holdHeader =
			"Method name, frontend_transformer,backend_checker\n";
	//private static String profileRoot = "/Users/mike/Documents/metamorphic-projects/mountaindew/tester/profiles/";
	private static String profileRoot = "profiles/";
	private static String configString = "config/mutant.property";
	private static String metaConfigString = "config/metamorphic.property";
	private static SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
	private MetamorphicConfigurer mConfigurer = new MetamorphicConfigurer(metaConfigString);
	private HashMap<Method, HashSet<MetamorphicProperty>> properties = new HashMap<Method, HashSet<MetamorphicProperty>>();
	private HashSet<Class<? extends MetamorphicProperty>> propertyPrototypes;
	private HashSet<Class<? extends MetamorphicInputProcessor>> processorPrototypes;
	private HashSet<Class<? extends MetamorphicInputProcessor>> nonValueChangeProcessorPrototypes;
	private Class<? extends AbstractAdapter> targetAdapter;
	private HashMap<Integer, MethodInvocation> invocations = new HashMap<Integer, MethodInvocation>();
	private Integer invocationId = 0;
	private List<MethodProfiler> profilerList = new ArrayList<MethodProfiler>();
//	private Cloner cloner = new Cloner();
	private String calleeName;
	private String timeTag = "default";
	private String holdTag = "hold";
	
//	private static int fileCount = 0;
	
	public Interceptor(Object intercepted) {
		super(intercepted);
		System.out.println("Interceptor created");
		//propertyPrototypes = MetamorphicObserver.getInstance().registerInterceptor(this);
		//processorPrototypes = MetamorphicInputProcessorGroup.getInstance().getProcessors();
		propertyPrototypes = this.filterCheckers(MetamorphicObserver.getInstance().registerInterceptor(this));
		processorPrototypes = this.filterTransformers(MetamorphicInputProcessorGroup.getInstance().getProcessors());
		nonValueChangeProcessorPrototypes = MetamorphicInputProcessorGroup.getInstance().getNonValueChangeProcessors();
		targetAdapter = this.getAdapter();
		this.getTimeTag();
	}
	
	private HashSet<Class<? extends MetamorphicProperty>> filterCheckers(HashSet<Class<? extends MetamorphicProperty>> allCheckers) {
		List<String> selectedClasses = this.mConfigurer.getCheckerNames();
		HashSet<Class<? extends MetamorphicProperty>> ret = new HashSet<Class<? extends MetamorphicProperty>>();
		
		for (Class<? extends MetamorphicProperty> tmpChecker: allCheckers) {
			if (selectedClasses.contains(tmpChecker.getName())) {
				ret.add(tmpChecker);
			}
		}
		
		return ret;
	}
	
	private HashSet<Class<? extends MetamorphicInputProcessor>> filterTransformers(HashSet<Class<? extends MetamorphicInputProcessor>> allTransformers) {
		List<String> selectedClasses = this.mConfigurer.getTransformerNames();
		HashSet<Class<? extends MetamorphicInputProcessor>> ret = new HashSet<Class<? extends MetamorphicInputProcessor>>();
		
		for (Class<? extends MetamorphicInputProcessor> tmpProcessor: allTransformers) {
			if (selectedClasses.contains(tmpProcessor.getName())) {
				ret.add(tmpProcessor);
			}
		}
		
		return ret;
	}
	
	private Class<? extends AbstractAdapter> getAdapter() {
		String targetClass = mConfigurer.getAdapterClassName();
		Class<? extends AbstractAdapter> targetAdapter = AdapterLoader.loadClass(targetClass);
		return targetAdapter;
	}
	
	private void getTimeTag() {
		File mutantConfig = new File(configString);
		
		if (!mutantConfig.exists()) {
			System.err.println("Mutant configuration file does not exist");
		}
		
		FileInputStream fs;
		try {
			fs = new FileInputStream(mutantConfig);
			
			Properties mutantProperty = new Properties();
			mutantProperty.load(fs);
			
			String tmpTag = mutantProperty.getProperty("timetag");
			
			if (tmpTag == null || tmpTag.isEmpty()) {
				System.err.println("Time tag in mutant configuration file is empty");
			} else {
				this.timeTag = tmpTag;
			}
			
			fs.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
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
					p.setInputProcessors(this.processorPrototypes);
					p.setTargetAdapter(this.targetAdapter);
					p.setNonValueChangeInputProcessors(this.nonValueChangeProcessorPrototypes);
					p.loadInputProcessors();
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
		//In case the input param is also the output of the method
		inv.params = deepClone(params);
		inv.method = method;
		inv.callee = getInterceptedObject();
		invocations.put(retId, inv);
		
		this.calleeName = callee.getClass().getName();
		int namePos = this.calleeName.lastIndexOf(".");
		this.calleeName = this.calleeName.substring(namePos+1, calleeName.length());
		
		/*ArrayList<MethodInvocation> children = new ArrayList<MethodInvocation>();
		for(MetamorphicProperty p : properties.get(inv.method))
		{
			for(MethodInvocation child : p.createChildren(inv))
			{
				//System.out.println("Check children frontend backend: " + child.getFrontend() + " " + child.getBackend());
				child.callee = deepClone(inv.callee);
				child.method = inv.method;
				children.add(child);
				child.thread = createChildThread(child);
				child.thread.start();
				try {
					child.thread.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		inv.children = new MethodInvocation[children.size()];
		inv.children = children.toArray(inv.children);

		this.calleeName = callee.getClass().getName();
		int namePos = this.calleeName.lastIndexOf(".");
		this.calleeName = this.calleeName.substring(namePos+1, calleeName.length());
		
		System.out.println("Method name " + inv.getMethod().getName());
		System.out.println("Children size: " + inv.children.length);
		System.out.println("Callee: " + callee.getClass().getName());
		
		this.reportTransformerChecker();*/
		return retId;
	}
	
	public void onExit(Object val, int op, int id)
	{
		if(id < 0)
			return;
		MethodInvocation inv = invocations.remove(id);
		inv.returnValue = val;
		
		ArrayList<MethodInvocation> children = new ArrayList<MethodInvocation>();
		for(MetamorphicProperty p : properties.get(inv.method))
		{
			for(MethodInvocation child : p.createChildren(inv))
			{
				//System.out.println("Check children frontend backend: " + child.getFrontend() + " " + child.getBackend());
				child.callee = deepClone(inv.callee);
				child.method = inv.method;
				children.add(child);
				child.thread = createChildThread(child);
				child.thread.start();
				try {
					child.thread.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		inv.children = new MethodInvocation[children.size()];
		inv.children = children.toArray(inv.children);
		
		System.out.println("Method name " + inv.getMethod().getName());
		System.out.println("Children size: " + inv.children.length);
		System.out.println("Callee: " + this.calleeName);
		
		this.reportTransformerChecker();
				
		for(MethodInvocation inv2 : inv.children)
		{
			try {
				inv2.thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			System.out.println("Log children");
			for(MetamorphicProperty p : properties.get(inv2.method))
			{
				p.logExecution(inv2);
			}
		}
		
		for(MetamorphicProperty p : properties.get(inv.method))
		{
//			System.out.println("Parent go next");
			p.logExecution(inv);
		}
		
		//Calculate correlation coefficient between ori_input and output
		//The length of correlation array depends on how many input in inv.getParams()
		
		//Try to alleviate heap space issue
		System.gc();
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
				profilerList.add(p.getMethodProfiler());
			}
		}
	}
	
	public void reportPropertyResultList() {
		for (Method m: properties.keySet()) {
			System.out.println(m);
			
			for (MetamorphicProperty p: properties.get(m)) {
				List<PropertyResult> resultList = p.propertiesHolds();
				
				for (PropertyResult result: resultList) {
					System.out.println(result + "\n");
				}
				profilerList.add(p.getMethodProfiler());
			}
		}
	}
	
	public void exportMethodProfile() {
		
		StringBuilder sBuilder = new StringBuilder();
		
		sBuilder.append(header);
		
		MethodProfiler tmpProfiler;
		/*while (!this.profilerList.isEmpty()) {
			tmpProfiler = this.profilerList.remove(0);
			for (MethodProfile mProfile: tmpProfiler.getMethodProfiles()) {
				sBuilder.append(mProfile.toString());
			}
			System.gc();
		}*/
		
		for (MethodProfiler mProfiler: this.profilerList) {
			for (MethodProfile mProfile: mProfiler.getMethodProfiles()) {
				sBuilder.append(mProfile.toString());
			}
		}
		//System.out.println("Test export string: " + sBuilder.toString());
		
		File rootDir = new File(profileRoot + this.timeTag);
		
		String absPath = "";
		if (rootDir.exists() && rootDir.isDirectory()) {
			absPath = rootDir.getAbsolutePath() + "/";
			System.out.println("Confirm root directory for exporting: " + absPath);
		} else if (!rootDir.exists()) {
			System.out.println("Profile directory does not exists. Create one...");
			boolean success = rootDir.mkdir();
			
			if (success) {
				System.out.println("Profile directory creation succeeds.");
				absPath = rootDir.getAbsolutePath() + "/";
				System.out.println("Confirm root direcotry for exporting: " + absPath);
			} else {
				System.out.println("Profile directory creation fails");
				return ;
			}
		} else {
			System.out.println("For some reason, profile directory creation fails.");
			return ;
		}
		
		try {
			FileWriter fWriter = new FileWriter(absPath + this.calleeName + "_" + (new Date()).toString().replaceAll(" ", "") + ".csv");
			BufferedWriter bWriter = new BufferedWriter(fWriter);
			bWriter.write(sBuilder.toString());
			bWriter.close();
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	public void exportHoldMethodProfile() {
		
		StringBuilder sBuilder = new StringBuilder();
		
		sBuilder.append(holdHeader);
		
		MethodProfiler tmpProfiler;
		for (MethodProfiler mProfiler: this.profilerList) {
			for (MethodProfile mProfile: mProfiler.getHoldMethodProfiles()) {
				sBuilder.append(mProfile.getOri().getMethod().getName() + ",");
				sBuilder.append(mProfile.getFrontend() + ",");
				sBuilder.append(mProfile.getBackend() + "\n");
				//sBuilder.append(mProfile.getResult().holds + "\n");
			}
		}
		//System.out.println("Test export string: " + sBuilder.toString());
		
		//File rootDir = new File(profileRoot + this.timeTag);
		File rootDir = new File(profileRoot + this.holdTag);
		
		String absPath = "";
		if (rootDir.exists() && rootDir.isDirectory()) {
			absPath = rootDir.getAbsolutePath() + "/";
			System.out.println("Confirm root directory for exporting: " + absPath);
		} else if (!rootDir.exists()) {
			System.out.println("Profile directory does not exists. Create one...");
			boolean success = rootDir.mkdir();
			
			if (success) {
				System.out.println("Profile directory creation succeeds.");
				absPath = rootDir.getAbsolutePath() + "/";
				System.out.println("Confirm root direcotry for exporting: " + absPath);
			} else {
				System.out.println("Profile directory creation fails");
				return ;
			}
		} else {
			System.out.println("For some reason, profile directory creation fails.");
			return ;
		}
		
		try {
			File holdFile = new File(absPath + this.calleeName + ".csv");
			
			if (holdFile.exists()) {
				holdFile.delete();
			}
			
			//FileWriter fWriter = new FileWriter(absPath + this.calleeName + "_" + (new Date()).toString().replaceAll(" ", "") + "_holds.csv");
			FileWriter fWriter = new FileWriter(holdFile);
			BufferedWriter bWriter = new BufferedWriter(fWriter);
			bWriter.write(sBuilder.toString());
			bWriter.close();
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	public void exportHoldMethodProperty() {
		
		Properties prop = new Properties();
		HashSet<String> remainedT = new HashSet<String>();
		HashSet<String> remainedC = new HashSet<String>();
		
		String methodKey = "Method";
		String tKey = "Transformers";
		String cKey = "Checkers";
		
		StringBuilder sBuilder = new StringBuilder();
		
		sBuilder.append(holdHeader);
		
		MethodProfiler tmpProfiler;
		for (MethodProfiler mProfiler: this.profilerList) {
			for (MethodProfile mProfile: mProfiler.getHoldMethodProfiles()) {
				sBuilder.append(mProfile.getOri().getMethod().getName() + ",");
				sBuilder.append(mProfile.getFrontend() + ",");
				sBuilder.append(mProfile.getBackend() + "\n");
				//sBuilder.append(mProfile.getResult().holds + "\n");
			}
		}
		//System.out.println("Test export string: " + sBuilder.toString());
		
		//File rootDir = new File(profileRoot + this.timeTag);
		File rootDir = new File(profileRoot + this.holdTag);
		
		String absPath = "";
		if (rootDir.exists() && rootDir.isDirectory()) {
			absPath = rootDir.getAbsolutePath() + "/";
			System.out.println("Confirm root directory for exporting: " + absPath);
		} else if (!rootDir.exists()) {
			System.out.println("Profile directory does not exists. Create one...");
			boolean success = rootDir.mkdir();
			
			if (success) {
				System.out.println("Profile directory creation succeeds.");
				absPath = rootDir.getAbsolutePath() + "/";
				System.out.println("Confirm root direcotry for exporting: " + absPath);
			} else {
				System.out.println("Profile directory creation fails");
				return ;
			}
		} else {
			System.out.println("For some reason, profile directory creation fails.");
			return ;
		}
		
		try {
			File holdFile = new File(absPath + this.calleeName + ".csv");
			
			if (holdFile.exists()) {
				holdFile.delete();
			}
			
			//FileWriter fWriter = new FileWriter(absPath + this.calleeName + "_" + (new Date()).toString().replaceAll(" ", "") + "_holds.csv");
			FileWriter fWriter = new FileWriter(holdFile);
			BufferedWriter bWriter = new BufferedWriter(fWriter);
			bWriter.write(sBuilder.toString());
			bWriter.close();
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	private void reportTransformerChecker() {
		System.out.println("Registered Transformers: ");
		
		for (Class<? extends MetamorphicInputProcessor> processorPrototype: this.processorPrototypes) {
			System.out.println(processorPrototype.getName());
		}
		
		System.out.println("");
		
		System.out.println("Registered Adapter: ");
		System.out.println(this.targetAdapter.getName());
		
		System.out.println("");
		
		System.out.println("Registered Checkers: ");
		
		for (Class<? extends MetamorphicProperty> checkerPrototype: this.propertyPrototypes) {
			System.out.println(checkerPrototype.getName());
		}
	}
} 

