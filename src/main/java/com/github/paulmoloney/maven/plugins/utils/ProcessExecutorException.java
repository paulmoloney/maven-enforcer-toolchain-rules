package com.github.paulmoloney.maven.plugins.utils;

public class ProcessExecutorException extends Exception {
	private static final long serialVersionUID = -1L;

	/**
	 * 
	 */
	public ProcessExecutorException() {
		super();
	}

	/**
	 * @param message
	 * @param cause
	 */
	public ProcessExecutorException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message
	 */
	public ProcessExecutorException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public ProcessExecutorException(Throwable cause) {
		super(cause);
	}

}
