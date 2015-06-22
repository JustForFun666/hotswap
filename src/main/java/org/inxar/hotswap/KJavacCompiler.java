/**
 * $Id$
 *
 * Copyright (C) 2001 Paul Cody Johnston - pcj@inxar.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 */
package org.inxar.hotswap;

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.io.*;
import java.util.StringTokenizer;
import java.util.Iterator;

/**
 * Concrete <code>ProxyCompiler</code> implementation that interfaces
 * with <code>sun.tools.javac.Main</code> (requires
 * <code>tools.jar</code> in the classpath).
**/
public class KJavacCompiler extends ProxyCompiler
{
    protected static final String COMMAND_NAME = "javac";

    // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    // Implementation Note: This is all done using reflection in order
    // to not have to worry if user has tools.jar in the classpath.
    //
    // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

    // ================================
    // Constructors
    // ================================

    /**
     * Constructs a new <code>ProxyCompiler</code> having the given
     * parent <code>ClassLoader</code>.
     **/
    public KJavacCompiler(ClassLoader parent)
    {
	super(parent);
    }

    /**
     * Constructs a new <code>ProxyCompiler</code> having a default
     * <code>ClassLoader</code>.
     **/
    public KJavacCompiler()
    {
	super();
    }

    protected void init()
    {
	super.init();

	if (ctor == null)
	    throw new IllegalStateException
		("Cannot instantiate: cannot find sun.tools.javac.Main.  "+
		 "Please check your classpath for $JAVA_HOME/lib/tools.jar");
    }

    // ================================
    // TEHC Methods
    // ================================

    // ================================
    // ProxyCompiler Methods
    // ================================

    synchronized int compile(String className,
			     ProxyCompiler.Resource sourceFile,
			     ProxyCompiler.Resource classFile)
    {
	// Check to see if recompilation is advised.
	switch (filestat(sourceFile, classFile)) {

	    /* if it cannot be determined, propogate this uncertainty */
	case RC_FILESTAT_UNKNOWN:
	    return RC_COMPILE_UNKNOWN;

	    /* if the sourcefile does not exist, propogate this. */
	case RC_FILESTAT_SOURCE_MISSING:
	    return RC_COMPILE_SOURCE_MISSING;

	    /* if the classfile is current there is no need run the
               compilation; return trivial */
	case RC_FILESTAT_CLASS_CURRENT:
	    return RC_COMPILE_TRIVIAL;

	    /* if the classfile is expired then continue processing */
	case RC_FILESTAT_CLASS_EXPIRED:
	    break;

	default:
	    throw new InternalError(); // bad coding
	}

	// In this implementation we trust the compiler to tell us if
	// the compilation succeeded rather than doing file
	// modification tracking.

	boolean wasSuccessful = false;
	String out = "";
	String err = "";

	// Prepare the command line.
	String cmd = getCommand()
	    .append(' ')
	    .append(sourceFile.toString())
	    .toString();
	String[] argv = split(cmd, " ");

	//ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StringWriter sout = new StringWriter();
        PrintWriter pout = new PrintWriter(sout);

	int rc = RC_COMPILE_UNKNOWN;

	try {

	    // Create a new Javac compiler instance.
	    //OutputStream os = new BufferedOutputStream(baos);
	    //Object[] args = new Object[] { os, COMMAND_NAME };
	    //Object javac = ctor.newInstance(args);

            // TODO: can I re-use this instance?
	    Object javac = ctor.newInstance();

	    //wasSuccessful = new sun.tools.javac.Main(os, COMMAND_NAME).compile(argv);
	    Integer resultCode = (Integer)compile.invoke(javac, new Object[]{argv, pout});
            System.out.println("javac resultCode: " + resultCode);
	    rc = resultCode.intValue();
            wasSuccessful = rc == RC_COMPILE_SUCCESS;

	} catch (Exception ioex) {
	    ioex.printStackTrace();
	    StringWriter sw = new StringWriter();
	    PrintWriter pw = new PrintWriter(sw);
	    ioex.printStackTrace(pw);
	    err += sw.toString();
	    wasSuccessful = false;
	}

	if (!wasSuccessful)
	    out = "Compilation did not complete successfully.";

	//err = baos.toString();

	if (hasListeners())
	    fire(new ProxyCompileEvent(this,
				       className,
				       "javac " + cmd.toString(),
				       sout.toString(),
				       null,
				       rc));

	return rc;
    }

    // ================================
    // Protected Methods
    // ================================

    protected StringBuffer getCommand()
    {
	StringBuffer cmd = new StringBuffer();

	if (opts != null) {
	    int count = 0;
	    Iterator i = opts.iterator();
	    while (i.hasNext()) {
		cmd.append(' ').append(i.next());
	    }
	}

	if (dst != null)
	    cmd.append(" -d ").append(dst);

	if (sourcepath != null)
	    cmd.append(" -sourcepath ").append(sourcepath);

	if (cps != null) {
	    cmd.append(" -classpath ");
	    int count = 0;
	    Iterator i = cps.iterator();
	    while (i.hasNext()) {
		if (count++ > 0)
		    cmd.append(PATHSEP);
		cmd.append(i.next());
	    }
	}

	return cmd;
    }

    protected String[] split(String s, String delim)
    {
	StringTokenizer st = new StringTokenizer(s, delim);
	String[] array = new String[st.countTokens()];
	int i=0;

	while (st.hasMoreTokens())
	    array[i++] = st.nextToken();

	return array;
    }

    // ================================
    // Static Fields
    // ================================

    protected static Method compile;
    protected static Constructor ctor;

    static {
 	try {

	    // Fetch the compiler class

            // This was the version in 2001
	    //Class main = Class.forName("sun.tools.javac.Main");

	    Class main = Class.forName("com.sun.tools.javac.Main");

	    // Setup the compile method
	    //Class[] params = new Class[]{ String[].class };
	    Class[] params = new Class[]{ String[].class, java.io.PrintWriter.class };
	    compile = main.getMethod("compile", params);

	    // Setup the constructor

            // 2001 sun.tools.javac.Main
	    //params = new Class[]{ OutputStream.class, String.class };

            // newer one, no-arg constructor
	    params = new Class[]{  };
	    ctor = main.getConstructor(params);

	} catch (Exception ex) {
            ex.printStackTrace();
	    compile = null;
	    ctor = null;
	}
    }
}
