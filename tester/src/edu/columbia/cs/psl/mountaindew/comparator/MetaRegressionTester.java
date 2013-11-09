package edu.columbia.cs.psl.mountaindew.comparator;

import java.io.InputStream;
import java.io.File;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashMap;
import java.util.TreeMap;

public class MetaRegressionTester {
	
	private static String basicLibs = 
			"lib/mountaindew.jar:lib/columbus2.jar:lib/invivo.jar:" +
			"lib/cloning-1.7.9.jar:lib/javassist.jar:lib/JFlex.jar:" +
			"lib/asm-all-4.0.jar:lib/java-cup.jar:" +
			"lib/log4j-1.2.16.jar:lib/objenesis-1.2.jar:" +
			"lib/gson-2.2.4.jar:bin/:";
	
	private static String wekabase = "/Users/mikefhsu/Mike/Research/Wekas/";
	
	private static String injector = "edu.columbia.cs.psl.mountaindew.runtime.MetamorphicInjector";
	
	private static String configBase = "config/";
	
	private static String recordBase = "/Users/mikefhsu/Mike/Research/Wekas/Record/";
	
	public static void main(String args[]) {
		String libName = args[0];
		String driver = args[1];
		String methodName = args[2];
		HashSet<String> versions = new HashSet<String>();
		
		TreeMap<String, Set<StateObject>> stateMap = new TreeMap<String, Set<StateObject>>();
		
		for (int i = 3; i < args.length; i++) {
			//versions.add(libSpecificName(libName, args[i]));
			versions.add(libName + "-" + args[i]);
		}
		
		System.out.println("Confirm driver class: " + driver);
		System.out.println("Confirm versions for Metamorphic Regression Testing: " + versions);
		
		for (String version: versions) {
			executeMetaTesting(driver, version);
			stateMap.put(version, JSONComparator.getStateSet(configBase + methodName + ".json"));
			cleanJSONFile(methodName, version);
		}
		
		for (String key: stateMap.keySet()) {
			Set<StateObject> curSet = stateMap.get(key);
			String nextKey = stateMap.higherKey(key);
			
			if (nextKey == null) {
				break;
			}
			Set<StateObject> nextSet = stateMap.get(nextKey);
			
			System.out.println(key + " vs. " + nextKey);
			Set<StateObject> diffSet = JSONComparator.diffStates(curSet, nextSet);
			System.out.println(diffSet);
			
			System.out.println(nextKey + " vs. " + key);
			diffSet = JSONComparator.diffStates(nextSet, curSet);
			System.out.println(diffSet);
		}
	}
	
	public static void executeMetaTesting(String driver, String version) {
		ArrayList<String> commands = constructCommands(driver, version);
		
		try {
			ProcessBuilder pb = new ProcessBuilder(commands);
			Process process = pb.start();
			
			InputStream out = process.getInputStream();
			InputStream err = process.getErrorStream();
			
			ProcessInfoReader outReader = new ProcessInfoReader(out, "Out");
			ProcessInfoReader errReader = new ProcessInfoReader(err, "Err");
			
			outReader.start();
			errReader.start();
			
			int result = process.waitFor();
			
			if (result < 0) {
				System.err.println("Something wrong with the metamorphic testing process");
			}
			System.out.println("Process finishes: "+ process.exitValue());
			
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public static ArrayList<String> constructCommands(String driver, String version) {
		ArrayList<String> commands = new ArrayList<String>();
		commands.add("java");
		commands.add("-Xmx6000m");
		commands.add("-javaagent:lib/mountaindew.jar");
		commands.add("-cp");
		
		String basebin = wekabase + version + "/bin";
		String baselib = "\"" + wekabase + version + "/lib/*\""; 
		
		String libs = basicLibs + basebin + ":" + baselib + ":" + "/Users/mikefhsu/researchws/WekaTester35X/lib/libsvm.jar";
		
		commands.add(libs);
		commands.add(injector);
		commands.add(driver);
		
		System.out.println("Confirm commands: " + commands);
		
		return commands;
	}
	
	public static String libSpecificName(String libName, String version) {
		//Split weka version
		char[] chars = version.toCharArray();
		StringBuilder sb = new StringBuilder();
		sb.append(libName);
		sb.append("-");
		
		for (int i = 0; i < chars.length; i++) {
			sb.append(chars[i]);
			sb.append("-");
		}
		
		sb.deleteCharAt(sb.length() - 1);
		
		return sb.toString();
	}
	
	public static void cleanJSONFile(String methodName,String version) {
		File jsonFile = new File(configBase + methodName + ".json");
		
		try {
			if (!jsonFile.exists()) {
				System.err.println(jsonFile.getCanonicalPath() + " does not exist");
				return ;
			}
			
			//Move json to record base
			if (!moveJSONFile(jsonFile, methodName, version))
				System.err.println("The move of " + jsonFile.getCanonicalPath() + "fails");
			
			jsonFile.delete();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
	}
	
	public static boolean moveJSONFile(File oriFile, String methodName, String version) {
		File destFile = new File(recordBase + methodName + version + ".json");
		
		if (destFile.exists())
			destFile.delete();
		
		return oriFile.renameTo(destFile);
	}

}
