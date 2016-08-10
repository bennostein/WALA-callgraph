package com.ibm.wala.examples.drivers;

import com.ibm.wala.dataflow.IFDS.ISupergraph;
import com.ibm.wala.dataflow.IFDS.TabulationResult;
import com.ibm.wala.examples.analysis.dataflow.ContextSensitiveReachingDefs;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.AnalysisOptions.ReflectionOptions;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cfg.BasicBlockInContext;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.config.FileOfClasses;
import com.ibm.wala.util.io.CommandLine;
import com.ibm.wala.util.warnings.Warnings;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Properties;


/**
 * Driver for running {@link ContextSensitiveReachingDefs}
 *
 */
public class CSReachingDefsDriver {

	  // more aggressive exclusions to avoid library blowup
	  // in interprocedural tests
	  private static final String EXCLUSIONS = "java\\/awt\\/.*\n" + 
	  		"javax\\/swing\\/.*\n" + 
	  		"sun\\/awt\\/.*\n" + 
	  		"sun\\/swing\\/.*\n" + 
	  		"com\\/sun\\/.*\n" + 
	  		"sun\\/.*\n" + 
	  		"org\\/netbeans\\/.*\n" + 
	  		"org\\/openide\\/.*\n" + 
	  		"com\\/ibm\\/crypto\\/.*\n" + 
	  		"com\\/ibm\\/security\\/.*\n" + 
	  		"org\\/apache\\/xerces\\/.*\n" + 
	  		"java\\/security\\/.*\n" + 
	  		"";
	  
	public static void main(String[] args) throws IOException, ClassHierarchyException, IllegalArgumentException, CallGraphBuilderCancelException {
	    long start = System.currentTimeMillis();		
	    Properties p = CommandLine.parse(args);
	    String scopeFile = p.getProperty("scopeFile");
	    if (scopeFile == null) {
	    	throw new IllegalArgumentException("must specify scope file");
	    }
	    String mainClass = p.getProperty("mainClass");
	    if (mainClass == null) {
	      throw new IllegalArgumentException("must specify main class");
	    }
	    AnalysisScope scope = AnalysisScopeReader.readJavaScope(scopeFile, null, CSReachingDefsDriver.class.getClassLoader());
	    scope.setExclusions(new FileOfClasses(new ByteArrayInputStream(EXCLUSIONS.getBytes("UTF-8"))));
	    IClassHierarchy cha = ClassHierarchy.make(scope);
	    System.out.println(cha.getNumberOfClasses() + " classes");
	    System.out.println(Warnings.asString());
	    Warnings.clear();
	    AnalysisOptions options = new AnalysisOptions();
	    Iterable<Entrypoint> entrypoints = Util.makeMainEntrypoints(scope, cha, mainClass);
	    options.setEntrypoints(entrypoints);
	    // you can dial down reflection handling if you like
	    options.setReflectionOptions(ReflectionOptions.NONE);
	    AnalysisCache cache = new AnalysisCache();
	    // other builders can be constructed with different Util methods
	    CallGraphBuilder builder = Util.makeZeroOneContainerCFABuilder(options, cache, cha, scope);
//	    CallGraphBuilder builder = Util.makeNCFABuilder(2, options, cache, cha, scope);
//	    CallGraphBuilder builder = Util.makeVanillaNCFABuilder(2, options, cache, cha, scope);
	    System.out.println("building call graph...");
	    CallGraph cg = builder.makeCallGraph(options, null);
//	    System.out.println(cg);
	    long end = System.currentTimeMillis();
	    System.out.println("done");
	    System.out.println("took " + (end-start) + "ms");
	    System.out.println(CallGraphStats.getStats(cg));
	    
	    ContextSensitiveReachingDefs reachingDefs = new ContextSensitiveReachingDefs(cg, cache);
	    TabulationResult<BasicBlockInContext<IExplodedBasicBlock>, CGNode, Pair<CGNode, Integer>> result = reachingDefs.analyze();
	    ISupergraph<BasicBlockInContext<IExplodedBasicBlock>, CGNode> supergraph = reachingDefs.getSupergraph();

	    // TODO print out some analysis results
	}

}
