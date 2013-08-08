package edu.columbia.cs.psl.mountaindew.runtime;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
import java.util.Set;
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
	private static SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmssSS");
	private MetamorphicConfigurer mConfigurer = new MetamorphicConfigurer(metaConfigString);
	private HashMap<Method, HashSet<MetamorphicProperty>> properties = new HashMap<Method, HashSet<MetamorphicProperty>>();
	private HashSet<Class<? extends MetamorphicProperty>> propertyPrototypes;
	private HashSet<Class<? extends MetamorphicInputProcessor>> processorPrototypes;
	private HashSet<Class<? extends MetamorphicInputProcessor>> nonValueChangeProcessorPrototypes;
	private HashSet<Class<? extends MetamorphicProperty>> finalPropertyPrototype;
	private HashSet<Class<? extends MetamorphicInputProcessor>> finalProcessorPrototype;
	private Class<? extends AbstractAdapter> targetAdapter;
	private HashMap<Integer, MethodInvocation> invocations = new HashMap<Integer, MethodInvocation>();
	private Integer invocationId = 0;
	private List<MethodProfiler> profilerList = new ArrayList<MethodProfiler>();
//	private Cloner cloner = new Cloner();
	private String calleeName;
	private String timeTag = "default";
	private String configRoot = "config";
	private String transKey = "Transformers";
	private String checkKey = "Checkers";
	private String adapterKey = "Adapter";
	private String stopKey = "Stop";
	private String holdTag = "Holds";
	
//	private static int fileCount = 0;
	
	public Interceptor(Object intercepted) {
		super(intercepted);
		System.out.println("Interceptor created");
		//propertyPrototypes = MetamorphicObserver.getInstance().registerInterceptor(this);
		//processorPrototypes = MetamorphicInputProcessorGroup.getInstance().getProcessors();
		propertyPrototypes = this.filterCheckers(this.mConfigurer, MetamorphicObserver.getInstance().registerInterceptor(this));
		processorPrototypes = this.filterTransformers(this.mConfigurer, MetamorphicInputProcessorGroup.getInstance().getProcessors());
		nonValueChangeProcessorPrototypes = MetamorphicInputProcessorGroup.getInstance().getNonValueChangeProcessors();
		targetAdapter = this.getAdapter();
		this.getTimeTag();
	}
	
	private HashSet<Class<? extends MetamorphicProperty>> filterCheckers(MetamorphicConfigurer mConfigurer, HashSet<Class<? extends MetamorphicProperty>> allCheckers) {
		List<String> selectedClasses = mConfigurer.getCheckerNames();
		HashSet<Class<? extends MetamorphicProperty>> ret = new HashSet<Class<? extends MetamorphicProperty>>();
		
		for (Class<? extends MetamorphicProperty> tmpChecker: allCheckers) {
			if (selectedClasses.contains(tmpChecker.getName())) {
				ret.add(tmpChecker);
			}
		}
		
		return ret;
	}
	
	private HashSet<Class<? extends MetamorphicInputProcessor>> filterTransformers(MetamorphicConfigurer mConfigurer, HashSet<Class<? extends MetamorphicInputProcessor>> allTransformers) {
		List<String> selectedClasses = mConfigurer.getTransformerNames();
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
			
			String methodPFile = "config/" + method.getName() + ".property";
			File tmpFile = new File(methodPFile);
			if (!tmpFile.exists()) {
				this.finalPropertyPrototype = this.propertyPrototypes;
				this.finalProcessorPrototype = this.processorPrototypes;
			} else {
				MetamorphicConfigurer methodConfigurer = new MetamorphicConfigurer(methodPFile);
				this.finalPropertyPrototype = this.filterCheckers(methodConfigurer, this.propertyPrototypes);
				this.finalProcessorPrototype = this.filterTransformers(methodConfigurer, this.processorPrototypes);
			}
			
			//System.out.println("Check final property prototype: " + finalPropertyPrototype);
			//System.out.println("Check final processor prototype: " + finalProcessorPrototype);
			
			for(Class<? extends MetamorphicProperty> c : finalPropertyPrototype)
			{
				try {
					MetamorphicProperty p = c.newInstance();
					p.setMethod(method);
					p.setInputProcessors(finalProcessorPrototype);
					p.setTargetAdapter(this.targetAdapter);
					p.setNonValueChangeInputProcessors(this.nonValueChangeProcessorPrototypes);
					p.loadInputProcessors();
					properties.get(method).add(p);
				} catch (InstantiationException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}

			/*for(Class<? extends MetamorphicProperty> c : propertyPrototypes)
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
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}*/
			
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
			FileWriter fWriter = new FileWriter(absPath + this.calleeName + formatter.format(new Date()) + ".csv");
			BufferedWriter bWriter = new BufferedWriter(fWriter);
			bWriter.write(sBuilder.toString());
			bWriter.close();
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	public void exportHoldMethodProfile() {
		//Need to retrieve all methods first
		HashMap<String, Boolean> allMethodMap = new HashMap<String, Boolean>();
		List<MethodProp> methodPropList = new ArrayList<MethodProp>();
		List<MethodProfile> tmpHoldList;
		for (MethodProfiler mProfiler: this.profilerList) {
			for (MethodProfile mProfile: mProfiler.getMethodProfiles()) {
				allMethodMap.put(mProfile.getOri().getMethod().getName(), false);
			}
			
			String methodName;
			for (MethodProfile mProfile: mProfiler.getHoldMethodProfiles()) {
				methodName = mProfile.getOri().getMethod().getName();
				
				boolean found = false;
				for (MethodProp tmpProp: methodPropList) {
					if (tmpProp.getMethodName().equals(methodName)) {
						tmpProp.addTransformer(mProfile.getFrontend());
						tmpProp.addChecker(mProfile.getBackend());
						tmpProp.setAdapterString(this.mConfigurer.getAdapterName());
						found = true;
					}
				}
				
				if (!found) {
					MethodProp mProp = new MethodProp(methodName);
					mProp.addTransformer(mProfile.getFrontend());
					mProp.addChecker(mProfile.getBackend());
					mProp.setAdapterString(this.mConfigurer.getAdapterName());
					methodPropList.add(mProp);
				}
			}
		}
		
		try {
			Properties prop = new Properties();
			String propertyFileName;
			File propertyFile;
			for (MethodProp tmpProp: methodPropList) {
				System.out.println("Start to load property");
				propertyFileName = this.configRoot + "/" + tmpProp.getMethodName() + ".property";
				propertyFile = new File(propertyFileName);
				
				if (propertyFile.exists()) {
					propertyFile.delete();
				}
				
				//prop.load(new FileInputStream(propertyFileName));
				prop.setProperty(this.transKey, tmpProp.getTransformersString());
				prop.setProperty(this.checkKey, tmpProp.getCheckersString());
				prop.setProperty(this.adapterKey, tmpProp.getAdapterString());
				prop.setProperty(this.stopKey, "false");
				
				prop.store(new FileOutputStream(propertyFileName), null);
				allMethodMap.put(tmpProp.getMethodName(), true);
			}
			
			for (String methodName: allMethodMap.keySet()) {
				if (!allMethodMap.get(methodName)) {
					this.shutDownPropertyFile(methodName);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}		
	}
	
	private void shutDownPropertyFile(String methodName) {
		Properties prop = new Properties();
		File propFile = new File(this.configRoot + "/" + methodName + ".property");
		
		if (propFile.exists())
			propFile.delete();
		
		try {
			prop.setProperty(this.transKey, "");
			prop.setProperty(this.checkKey, "");
			prop.setProperty(this.adapterKey, "");
			prop.setProperty(this.stopKey, "true");
			
			prop.store(new FileOutputStream(propFile), null);
		} catch (Exception ex) {
			ex.printStackTrace();
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
			System.out.println("Confirm exporting file: " + holdFile.getAbsolutePath());
			
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
		
		for (Class<? extends MetamorphicInputProcessor> processorPrototype: this.finalProcessorPrototype) {
			System.out.println(processorPrototype.getName());
		}
		
		System.out.println("");
		
		System.out.println("Registered Adapter: ");
		System.out.println(this.targetAdapter.getName());
		
		System.out.println("");
		
		System.out.println("Registered Checkers: ");
		
		for (Class<? extends MetamorphicProperty> checkerPrototype: this.finalPropertyPrototype) {
			System.out.println(checkerPrototype.getName());
		}
	}
	
	private static class MethodProp {
		
		private String methodName;
		
		private HashSet<String> transformers = new HashSet<String>();
		
		private HashSet<String> checkers = new HashSet<String>();
		
		private String adapter;
		
		public MethodProp(String methodName) {
			this.methodName = methodName;
		}
		
		public void addTransformer(String transformer) {
			transformer = transformer.replace("T:", "");
			this.transformers.add(transformer);
		}
		
		public void addChecker(String checker) {
			checker = checker.replace("C:", "");
			this.checkers.add(checker);
		}
		
		public void setAdapterString(String adapter) {
			this.adapter = adapter;
		}
		
		public String getAdapterString() {
			return this.adapter;
		}
		
		public HashSet<String> getTransformers() {
			return this.transformers;
		}
		
		public HashSet<String> getCheckers() {
			return this.checkers;
		}
		
		public String getTransformersString() {
			StringBuilder sb = new StringBuilder();
			
			for (String tran: this.transformers) {
				sb.append(tran + ",");
			}
			sb.deleteCharAt(sb.length() - 1);
			return sb.toString();
		}
		
		public String getCheckersString() {
			StringBuilder sb = new StringBuilder();
			
			for (String check: this.checkers) {
				sb.append(check + ",");
			}
			sb.deleteCharAt(sb.length() - 1);
			return sb.toString();
		}
		
		public String getMethodName() {
			return this. methodName;
		}
			
		public boolean equals(Object tmp) {
			if (tmp == this)
				return true;
			
			if (tmp.getClass() != this.getClass())
				return false;
			
			MethodProp input = (MethodProp)tmp;
			
			if (input.getMethodName().equals(this.getMethodName()) && input.getTransformers() == this.getTransformers() && input.getCheckers() == this.getCheckers())
				return true;
			else
				return false;
		}
	}
} 

