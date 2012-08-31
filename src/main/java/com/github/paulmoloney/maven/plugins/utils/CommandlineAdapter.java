package com.github.paulmoloney.maven.plugins.utils;

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

import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;

/**
 * @author Paul Moloney
 *
 */
public class CommandlineAdapter implements ProcessExecutor {

	private Commandline cmdLine = new Commandline();

	public CommandlineAdapter(String executable, String argument) {
        cmdLine.setExecutable(executable);
        cmdLine.createArg().setValue(argument);
	}
	
	public String runApplication() throws ProcessExecutorException
	{
		String executable = cmdLine.getExecutable();
    	File toolCmd = new File(executable);
    	if (!(toolCmd.isFile() && toolCmd.exists()))
    	{
    		throw new ProcessExecutorException("Tool '" + executable + "' is not a valid executable command");
    	}
    	
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
            return firstLine.toString();
        }
        catch (CommandLineException e)
        {
        	throw new ProcessExecutorException("Error executing: " + cmdLine, e);
        } 
	}
	
	public String getCommandLine()
	{
	    return cmdLine.toString();	
	}
}
