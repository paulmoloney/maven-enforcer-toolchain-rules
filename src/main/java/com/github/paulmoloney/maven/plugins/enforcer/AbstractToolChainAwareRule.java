package com.github.paulmoloney.maven.plugins.enforcer;

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
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.apache.maven.plugins.enforcer.AbstractVersionEnforcer;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;

import org.codehaus.plexus.compiler.CompilerError;
import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.StringUtils;

/**
 * Helper utility methods that toolchain aware rules can build upon
 * @author <a href="mailto:">Paul Moloney</a>
 * @version $Id: AbstractToolChainAwareRule.java $
 */
public abstract class AbstractToolChainAwareRule extends AbstractVersionEnforcer {
    private ToolchainManager toolchainManager;

    private MavenSession session;

    protected String executable;

    //@Parameter( defaultValue = "${project.build.outputDirectory}", required = true, readonly = true )
    private File outputDirectory;

    /**
     * The directory to run the compiler from if fork is true.
     */
    //@Parameter( defaultValue = "${basedir}", required = true, readonly = true )
    private File basedir; 

    /**
     * The target directory of the compiler if fork is true.
     */
    @Parameter( defaultValue = "${project.build.directory}", required = true, readonly = true )
    private File buildDirectory;

    protected void init(EnforcerRuleHelper helper) throws EnforcerRuleException, MojoExecutionException
    {
    	final String version = getVersion(); 
    	if (null == version || "".equals(version.trim()))
    	{
    		throw new MojoExecutionException("Version parameter was not supplied for rule usage");
    	}
    	try
    	{
    	    VersionRange.createFromVersionSpec( version );
    	}
    	catch (InvalidVersionSpecificationException e)
    	{
    		throw new MojoExecutionException("Invalid version parameter was supplied for rule usage", e);
    	}
    	try
	    {
	        session = (MavenSession) helper.evaluate("${session}");
	        toolchainManager = (ToolchainManager) helper.getComponent(ToolchainManager.class);
	        outputDirectory = new File((String) helper.evaluate("${project.build.outputDirectory}"));
	        basedir = new File((String) helper.evaluate("${basedir}"));
	    }
	    catch (ExpressionEvaluationException e)
	    {
	        throw new MojoExecutionException ("Unable to evaluate maven environment", e);
	    }
	    catch (ComponentLookupException e)
	    {
	        throw new MojoExecutionException ("Unable to retrieve component", e);
	    }
    }

    //TODO remove the part with ToolchainManager lookup once we depend on
    //3.0.9 (have it as prerequisite). Define as regular component field then.
    protected Toolchain findToolChain(String type, EnforcerRuleHelper helper, MavenProject project) throws MojoExecutionException
    {
    	
	    Toolchain tc = null;
	    if ( toolchainManager != null )
	    {
	        tc = toolchainManager.getToolchainFromBuildContext( type, session );
	    }
	    else
	    {
	    	helper.getLog().warn("Toolchain manager could not be found, toolchain plugin must run before the enforcer plugin");
	    	//tc = createToolChainManager(type, log, project);
	    }
	    if ( tc != null )
	    {
	    	helper.getLog().debug("Toolchain found for type: " + type);
	    }
	    else
	    {
	    	helper.getLog().debug("No toolchain found for type " + type);
	    }
	    return tc;
    }

    protected File getOutputDirectory()
    {
        return outputDirectory;
    }

    protected File getBaseDirectory()
    {
        return basedir;
    }

    protected File getBuildDirectory()
    {
        return buildDirectory;
    }

    private static final String LS = System.getProperty( "line.separator" );

    /**
     * Long message will contain all the errors form running the compiler
     * @param messages
     * @return the long or full error message
     */
    public String longMessage( List<CompilerError> messages )
    {
        StringBuilder sb = new StringBuilder();

        if ( messages != null )
        {
            for ( CompilerError compilerError : messages )
            {
                sb.append( compilerError ).append( LS );
            }
        }
        return sb.toString();
    }

    /**
     * Short message will have the error message if there's only one, useful for errors forking the compiler
     *
     * @param messages
     * @return the short error message 
     */
    public String shortMessage( List<CompilerError> messages )
    {
        StringBuffer sb = new StringBuffer();

        sb.append( "Compilation failure" );

        if ( messages.size() == 1 )
        {
            sb.append( LS );

            CompilerError compilerError = (CompilerError) messages.get( 0 );

            sb.append( compilerError ).append( LS );
        }

        return sb.toString();
    }

    protected String getExecutableExtension()
    {
    	return ( Os.isFamily( Os.FAMILY_WINDOWS ) ? ".exe" : "" );
    }

    protected MavenSession getSession()
    {
    	return session;
    }

    /**
     * Determines a path to a tool based on environment variables and subdirectory searches
     * @param tool
     * @param log
     * @param sysProperty
     * @param subDirs1
     * @param envArgs
     * @param subDirs2
     * @return The path to a tool or null if one is not found
     */
    protected String findToolExecutable(String tool, Log log, String sysProperty, String[] subDirs1, String [] envArgs, String [] subDirs2)
    {
    	log.warn("Falling back to env lookup for specified tool");
        Properties env = new Properties();
        env.putAll(getSession().getSystemProperties());
        env.putAll(getSession().getUserProperties());

        String command = tool;
        try {
        	command = findExecutable( tool, env.getProperty( sysProperty ), subDirs1 );
	
	        if ( command == null )
	        {
	            if (null != envArgs)
	            {
	                for ( int i = 0; i < envArgs.length && executable == null; i++ )
	                {
	                	command =
	                        findExecutable( tool, env.getProperty( envArgs[i] ), subDirs2 );
	                	if (command != null) {
	                		break;
	                	}
	                }
	            }
	        }
        }
        catch (IOException e) {
        	log.error("Unable to find executable", e);
        }

        log.warn("Using executable: " + command);
        return command;
    }

    /**
     * Attempt to locate a specified command line tool
     * @param command
     * @param homeDir
     * @param subDirs
     * @return
     * @throws IOException if a valid path to the specified command can not be determined or null if no tool found
     */
    protected String findExecutable( String command, String homeDir, String[] subDirs ) throws IOException
    {
        if ( StringUtils.isNotEmpty( homeDir ) )
        {
            for ( int i = 0; i < subDirs.length; i++ )
            {
                File file = new File( new File( homeDir, subDirs[i] ), command );

                if ( file.isFile() && file.exists())
                {
                    return new File(file.getAbsolutePath()).getCanonicalPath();
                }
            }
        }

        return null;
    }

	protected ToolchainManager getToolchainManager() {
		return toolchainManager;
	}

	protected String getExecutable() {
		return executable;
	}

	protected File getBasedir() {
		return basedir;
	}

    /**
     * Construct a new see {ToolchainManager}
     * @param type the toolchain type
     * @param log a logger
     * @param project the maven project
     * @return ToolchainManager or null
     * @throws MogoExecutionException
     *
    private ToolchainManager createToolChainManager(String type, Log log, MavenProject project)
    {
    	//this breaks later with a life-cycle exception
    	File toolchainsFile = null;
    	try {
    		//Maven 3.0-betaX?
    	    MavenExecutionRequest req = session.getRequest();
    	    toolchainsFile = req.getUserToolchainsFile();
    	} catch (Exception e)
    	{
    		//ignore and try to support older clients
    	}
    	if (null != toolchainsFile && !toolchainsFile.exists())
    	{
	        toolchainsFile = new File( new File( System.getProperty("user.home" ), ".m2" ), "toolchains.xml" );
	    }
	    //todo specified toolchains rather than user default
	    if ( !toolchainsFile.exists() )
	    {
  	      throw new MojoExecutionException("No toolchains.xml file found at " + toolchainsFile);
	    }
	
	    PersistedToolchains toolchainModels = null;
	    Reader in = null;
	    try
	    {
	        in = ReaderFactory.newXmlReader( toolchainsFile );
	        toolchainModels = new MavenToolchainsXpp3Reader().read( in);
	    }
	    catch ( Exception e )
	    {
	        throw new MojoExecutionException("Malformed toolchains.xml", e );
	    }
	    finally
	    {
	        IOUtil.close( in );
	    }
	
	    List<ToolchainModel> toolChains = toolchainModels.getToolchains();
	    if (null != toolChains)
	    {
	        for (ToolchainModel tcModel : toolChains)
	        {
	            if (type.equals(tcModel.getType()))
	            {
	                log.info("Found ToolChain Model: " + tcModel );
	                tc = new DefaultJavaToolChain(tcModel, null);
	                break;
	            }
	        }
	    }
    }*/

}
