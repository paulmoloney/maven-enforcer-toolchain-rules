package org.apache.maven.plugins.enforcer;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.toolchain.Toolchain;
import org.codehaus.plexus.compiler.manager.CompilerManager;
import org.codehaus.plexus.compiler.manager.NoSuchCompilerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.StringUtils;

import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;

/** This rule checks that the Java compiler version matched in toolchains.xml is allowed.
 * @author <a href="mailto:">Paul Moloney</a>
 * @version $Id: RuleJavaVersionToolchainAware.java $
 */
public class RuleJavaVersionToolchainAware extends AbstractToolChainAwareRule {
    @Parameter( property = "maven.compiler.compilerId", defaultValue = "javac" )
    private String compilerId;
    
    /*
     * From plexus
     */
    private CompilerManager compilerManager;
    
    @Parameter(property = "maven.compiler.compilerArgument", defaultValue = "-version" )
    private String compilerArgument;

    /**
     * If a suitable compiler from toolchains.xml can not be found, then try to match based on typical environmental variables
     */
    @Parameter (defaultValue = "true")
    private boolean isFallBackAllowed;
    
	/** 
	* This particular rule determines if the specified Java compiler version referenced in the toolchains.xml is an appropriate version
	* @see org.apache.maven.enforcer.rule.api.EnforcerRule&#execute(org.apache.maven.enforcer.rule.api.EnforcerRuleHelper)
	*/
    public void execute( EnforcerRuleHelper helper ) throws EnforcerRuleException {
    	try
    	{
    	    super.init(helper);
    	    try 
    	    {
	            compilerManager = (CompilerManager) helper.getComponent(CompilerManager.class);
    	    }
    	    catch (ComponentLookupException e)
    	    {
    	        throw new MojoExecutionException ("Unable to retrieve component", e);
    	    }
    	    if (null == compilerId || "".equals(compilerId))
    	    {
    	    	compilerId = "javac";
    	    }
    	    if (null == compilerArgument || "".equals(compilerArgument))
    	    {
    	    	compilerArgument = "-version";
    	    }    	    
    	    /*try
    	    {
    	        compilerId = (String) helper.evaluate("maven.compiler.compilerId");
    	    }
    	    catch (ExpressionEvaluationException e)
    	    {
    	        throw new MojoExecutionException ("Unable to determine compiler id", e);
    	    }*/
    	}
    	catch (MojoExecutionException e)
    	{
    		throw new EnforcerRuleException("Error initialising mojo", e);
    	}
	    String java_version;
	    final Log log = helper.getLog();

        log.debug( "Using compiler id'" + getCompilerId() + "'." );
        
        try
        {
            getCompilerManager().getCompiler( getCompilerId() );
        }
        catch ( NoSuchCompilerException e )
        {
            throw new EnforcerRuleException( "No compiler with id: '" + e.getCompilerId() + "'." );
        }
        
        try
        {
            Toolchain tc = findToolChain("jdk", helper, null);
            if (tc != null)
            {
        	    executable = tc.findTool( getCompilerId() );
        	    if (null != executable)
        	    {
        	    	executable = executable + getExecutableExtension();
        	    }        	    
            }
        }
        catch (MojoExecutionException e)
        {
        	throw new EnforcerRuleException("", e);
        }
        
        if (null == executable && isFallBackAllowed())
        {
        	executable = findToolExecutable(getCompilerId() + getExecutableExtension(), log, "java.home", 
        			new String [] { "../bin", "bin", "../sh" },
        			new String [] { "JDK_HOME", "JAVA_HOME" }, new String[] { "bin", "sh" }
        	    );
        }
        
        if (null == executable || "".equals(executable.trim()))
        {
            throw new EnforcerRuleException("No valid executable found, aborting");	
        } else {
        	File toolCmd = new File(executable);        	
        	if (!(toolCmd.isFile() && toolCmd.exists()))
        	{
        		throw new EnforcerRuleException("Tool '" + executable + "' is not a valid executable command");
        	}
        }
        java_version = runToolAndGetVersion(executable, log);
                
	    java_version = normalizeJDKVersion( java_version );
	    log.debug( "Normalized Java Version: " + java_version );
	
	    ArtifactVersion detectedJdkVersion = new DefaultArtifactVersion(java_version );
	    log.debug( "Parsed Version: Major: " + detectedJdkVersion.getMajorVersion() + " Minor: "
	        + detectedJdkVersion.getMinorVersion() + " Incremental: " + detectedJdkVersion.getIncrementalVersion()
	        + " Build: " + detectedJdkVersion.getBuildNumber() + "Qualifier: " + detectedJdkVersion.getQualifier() );
	
	    log.debug("Tool " + executable + ", provides: " + detectedJdkVersion + ", require: " + version);
	    enforceVersion( helper.getLog(), "JDK", version, detectedJdkVersion );
    } 

	/**
	* Converts a jdk string from 1.5.0-11b12 to a single 3 digitversion like 1.5.0-11
	*
	* @param theJdkVersion to be converted.
	* @return the converted string.
	*/
    private String normalizeJDKVersion( String theJdkVersion ) {
	    theJdkVersion = theJdkVersion.replaceAll( "_|-", "." );
	    String tokenArray[] = StringUtils.split( theJdkVersion, "." );
	    List<String> tokens = Arrays.asList( tokenArray );
	    StringBuffer buffer = new StringBuffer( theJdkVersion.length());
	    Iterator<String> iter = tokens.iterator();
	    
		for ( int i = 0; i < tokens.size() && i < 4; i++ ) {
	        String section = (String) iter.next();
	        section = section.replaceAll( "[^0-9]", "" );
	        if ( StringUtils.isNotEmpty( section ) ) {
	            buffer.append( Integer.parseInt( section ) );
	            if ( i != 2 ) {
	                buffer.append( '.' );
	            } else {
	                buffer.append( '-' );
	            }
	       }
	    }
	
	    String version = buffer.toString();
	    version = StringUtils.stripEnd( version, "-" );
	    return StringUtils.stripEnd( version, "." );
    }

    /**
     * Runs the specified java compiler to find out its version
     * @param executable to run
     * @param log to write to
     * @return the version of specified executable
     * @throws EnforcerRuleException if version can not be determined
     */
    private String runToolAndGetVersion(String executable, final Log log) throws EnforcerRuleException
    {
        Commandline cmdLine = new Commandline();
        cmdLine.setExecutable(executable);
        
        cmdLine.createArg().setValue(getCompilerArgument());
               
        InputStream in = new InputStream() {
        	public int read() {
        		return -1;
        	}
        };
        
        StreamConsumer out = new StreamConsumer() {
			
			public void consumeLine(String line) {
				//log.info(line);
			}
		};

		final StringBuilder firstLine = new StringBuilder();
		
        StreamConsumer err = new StreamConsumer() {
		
        	boolean foundFirstLine = true;
			public void consumeLine(String line) {
				if (foundFirstLine) {				
				    firstLine.append(line);
					foundFirstLine = !foundFirstLine;
				}
			}
		};
		
        try
        {
            CommandLineUtils.executeCommandLine(cmdLine, in, out, err);
        }
        catch (CommandLineException e)
        {
        	throw new EnforcerRuleException("Error executing: " + cmdLine, e);
        }
        
        String [] output = firstLine.toString().split("\\s");
        if (output.length > 1)
        {
        	log.debug(executable + " version: " + output[1]);
        	return output[1];
        }
        throw new EnforcerRuleException("No valid version could be determined for " + cmdLine);
    }

    private String getCompilerArgument()
    {
        return compilerArgument;
    }
       
    private CompilerManager getCompilerManager()
    {
    	return compilerManager;
    }
    
    private String getCompilerId()
    {
    	return compilerId;
    }
    
    private boolean isFallBackAllowed() {
    	return isFallBackAllowed;
    }    
}
