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

import java.util.Properties;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.compiler.manager.CompilerManager;
import org.codehaus.plexus.compiler.Compiler;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.github.paulmoloney.maven.plugins.utils.ProcessExecutor;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.*;

/**
 * @author Paul Moloney
 * Unit test for RuleJavaVersionToolchainAware.
 */
public class RuleJavaVersionToolchainAwareTest 
{
	private EnforcerRuleHelper helper;
	private MavenSession session;
	private ToolchainManager tcManager;
	private CompilerManager manager;
	private Log log;
	private Toolchain toolChain;
	private ProcessExecutor process;
	private Compiler compiler;
	
	@Before
	public void setUp() throws Exception
	{
		helper = mock(EnforcerRuleHelper.class);
		session = mock(MavenSession.class);
		tcManager = mock(ToolchainManager.class);
		manager = mock(CompilerManager.class);
		log = mock(Log.class);		
		toolChain = mock(Toolchain.class);
	    process = mock(ProcessExecutor.class);
	    compiler = mock(Compiler.class);
	}

	@After
	public void tearDown() throws Exception
	{		
		verifyNoMoreInteractions(helper, session, tcManager, manager, toolChain, compiler);
        validateMockitoUsage();		
	}
	
	@Test
	public void testFoundExactMatchToolChain() throws Exception
	{		
		when(helper.getComponent(ToolchainManager.class)).thenReturn(tcManager);
		when(helper.evaluate("${project.build.outputDirectory}")).thenReturn("");
		when(helper.evaluate("${basedir}")).thenReturn("");
		when(helper.getComponent(CompilerManager.class)).thenReturn(manager);
		when(helper.evaluate("${session}")).thenReturn(session);
		when(helper.getLog()).thenReturn(log);
		when(tcManager.getToolchainFromBuildContext("jdk", session)).thenReturn(toolChain);
		String executable = "/opt/javac";
		String compilerId = "javac";
		when(toolChain.findTool("javac")).thenReturn(executable);
		when(process.runApplication()).thenReturn("javac 1.5.0_01");
		when(manager.getCompiler(compilerId)).thenReturn(compiler);
				
		RuleJavaVersionToolchainAware rule = new RuleJavaVersionToolchainAware();
		rule.setCompilerId(compilerId);
		rule.setVersion("1.5.0-1");
		rule.setProcess(process);
		rule.execute(helper);
		
		verify(helper).getComponent(ToolchainManager.class);
		verify(helper).evaluate("${project.build.outputDirectory}");
		verify(helper).evaluate("${basedir}");		
		verify(helper).getComponent(CompilerManager.class);
		verify(helper).evaluate("${session}");
		verify(tcManager).getToolchainFromBuildContext("jdk", session);
		verify(manager).getCompiler(compilerId);
		
		verify(helper, times(2)).getLog();
		verify(toolChain).findTool("javac");
	}
	
	@Test
	public void testFoundOutOfDateToolChain() throws Exception
	{		
		when(helper.getComponent(ToolchainManager.class)).thenReturn(tcManager);
		when(helper.evaluate("${project.build.outputDirectory}")).thenReturn("");
		when(helper.evaluate("${basedir}")).thenReturn("");
		when(helper.getComponent(CompilerManager.class)).thenReturn(manager);
		when(helper.evaluate("${session}")).thenReturn(session);
		when(helper.getLog()).thenReturn(log);
		when(tcManager.getToolchainFromBuildContext("jdk", session)).thenReturn(toolChain);
		String executable = "/opt/javac";
		String compilerId = "javac";
		when(toolChain.findTool(compilerId)).thenReturn(executable);
		when(process.runApplication()).thenReturn("javac 1.5.0_01");
		when(manager.getCompiler(compilerId)).thenReturn(compiler);
				
		RuleJavaVersionToolchainAware rule = new RuleJavaVersionToolchainAware();
		rule.setCompilerId(compilerId);
		rule.setVersion("1.5.0-22");
		rule.setProcess(process);
		String ruleFailureMessage = null;
		try
		{
		    rule.execute(helper);
		}
		catch (EnforcerRuleException e) {
			ruleFailureMessage = e.getMessage();
			assertThat(ruleFailureMessage, is("Detected JDK Version: 1.5.0-1 is not in the allowed range 1.5.0-22."));
		}
		
		verify(helper).getComponent(ToolchainManager.class);
		verify(helper).evaluate("${project.build.outputDirectory}");
		verify(helper).evaluate("${basedir}");		
		verify(helper).getComponent(CompilerManager.class);
		verify(helper).evaluate("${session}");
		verify(tcManager).getToolchainFromBuildContext("jdk", session);
		verify(manager).getCompiler(compilerId);
		
		verify(helper, times(2)).getLog();
		verify(toolChain).findTool("javac");
	}

	@Test
	public void testFoundNewerToolChain() throws Exception
	{		
		when(helper.getComponent(ToolchainManager.class)).thenReturn(tcManager);
		when(helper.evaluate("${project.build.outputDirectory}")).thenReturn("");
		when(helper.evaluate("${basedir}")).thenReturn("");
		when(helper.getComponent(CompilerManager.class)).thenReturn(manager);
		when(helper.evaluate("${session}")).thenReturn(session);
		when(helper.getLog()).thenReturn(log);
		when(tcManager.getToolchainFromBuildContext("jdk", session)).thenReturn(toolChain);
		String executable = "/opt/javac";
		String compilerId = "javac";
		when(toolChain.findTool(compilerId)).thenReturn(executable);
		when(process.runApplication()).thenReturn("javac 1.6.0_01");
		when(manager.getCompiler(compilerId)).thenReturn(compiler);
				
		RuleJavaVersionToolchainAware rule = new RuleJavaVersionToolchainAware();
		rule.setCompilerId(compilerId);
		rule.setVersion("1.5.0-22");
		rule.setProcess(process);
		String ruleFailureMessage = null;
		try
		{
		    rule.execute(helper);
		}
		catch (EnforcerRuleException e) {
			ruleFailureMessage = e.getMessage();
			assertThat(ruleFailureMessage, is("Detected JDK Version: 1.5.0-1 is not in the allowed range 1.5.0-22."));
		}
		
		verify(helper).getComponent(ToolchainManager.class);
		verify(helper).evaluate("${project.build.outputDirectory}");
		verify(helper).evaluate("${basedir}");		
		verify(helper).getComponent(CompilerManager.class);
		verify(helper).evaluate("${session}");
		verify(tcManager).getToolchainFromBuildContext("jdk", session);
		verify(manager).getCompiler(compilerId);
		
		verify(helper, times(2)).getLog();
		verify(toolChain).findTool("javac");
	}
	
	@Test
	public void testNoValidExecutableFound() throws Exception
	{		
		when(helper.getComponent(ToolchainManager.class)).thenReturn(tcManager);
		when(helper.evaluate("${project.build.outputDirectory}")).thenReturn("");
		when(helper.evaluate("${basedir}")).thenReturn("");
		when(helper.getComponent(CompilerManager.class)).thenReturn(manager);
		when(helper.evaluate("${session}")).thenReturn(session);
		when(helper.getLog()).thenReturn(log);
		when(tcManager.getToolchainFromBuildContext("jdk", session)).thenReturn(null);
		String compilerId = "javac";
		when(manager.getCompiler(compilerId)).thenReturn(compiler);
		when(session.getSystemProperties()).thenReturn(new Properties());
		when(session.getUserProperties()).thenReturn(new Properties());
		
		RuleJavaVersionToolchainAware rule = new RuleJavaVersionToolchainAware();
		rule.setCompilerId(compilerId);
		rule.setVersion("1.5.0-22");
		rule.setProcess(process);
		rule.setFallback(true);
		String ruleFailureMessage = null;
		try
		{
		    rule.execute(helper);
		}
		catch (EnforcerRuleException e) {
			ruleFailureMessage = e.getMessage();
			assertThat(ruleFailureMessage, is("No valid executable found, aborting"));
		}
		
		verify(helper).getComponent(ToolchainManager.class);
		verify(helper).evaluate("${project.build.outputDirectory}");
		verify(helper).evaluate("${basedir}");		
		verify(helper).getComponent(CompilerManager.class);
		verify(helper).evaluate("${session}");
		verify(tcManager).getToolchainFromBuildContext("jdk", session);
		verify(manager).getCompiler(compilerId);
		
		verify(helper, times(2)).getLog();
		verify(session).getSystemProperties();
		verify(session).getUserProperties();
	}	
}
