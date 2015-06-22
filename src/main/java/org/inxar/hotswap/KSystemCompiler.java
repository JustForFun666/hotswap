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

import java.io.BufferedInputStream;
import java.io.StringWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.File;
import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;

/**
 * Concrete <code>ProxyCompiler</code> implementation that runs a
 * system command.
 *
 * <P>
 * <table border=0 cellpadding="0" cellspacing="0" bgcolor="#999999" width="100%">
 * <tr><td>
 * <table cellpadding="0" cellspacing="1"><tr><td><font face="helvetica" color="#eeeeee">
 * Constructor usage of KSystemCompiler
 * </font></td></tr></table>
 * </td></tr>
 * <tr><td>
 * <table border=0 cellpadding="7" cellspacing="1" bgcolor="#eeeeff" width="100%"><tr><td>
 * <pre>
 * ProxyCompiler compiler = new KSystemCompiler("javac");
 * ProxyCompiler compiler = new KSystemCompiler("/usr/local/jdk1.2.2/javac");
 * ProxyCompiler compiler = new KSystemCompiler("jikes");
 * ProxyCompiler compiler = new KSystemCompiler("/usr/bin/jikes");
 * </td></tr></table>
 * </td></tr>
 * </table>
 * <P>
**/
public class KSystemCompiler extends ProxyCompiler
{
    // ================================
    // Constructors 
    // ================================

    /**
     * Constructs a new <code>ProxyCompiler</code> having the given
     * parent <code>ClassLoader</code> and command name (for example
     * <code>/usr/bin/jikes</code>).
     **/
    public KSystemCompiler(ClassLoader parent, String commandName)
    {
	super(parent);
	this.commandName = commandName;
	initSystem();
    }
    
    /**
     * Constructs a new <code>ProxyCompiler</code> having a default
     * <code>ClassLoader</code> and command name (for example
     * <code>/usr/bin/jikes</code>).
     **/
    public KSystemCompiler(String commandName)
    {
	super();
	this.commandName = commandName;
	initSystem();
    }
    
    protected void initSystem()
    {
	if (commandName.endsWith("jikes")) {
	    String home = System.getProperty("java.home");
	    if (home != null) {
		if (home.endsWith("/jre")) {
		    int len = home.length() - 4;
		    home = home.substring(0, len);
		}			
		
		getClasspath().add(home + "/jre/lib/rt.jar");
		getClasspath().add(home + "/lib/tools.jar");
	    }
	}
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

	boolean wasSuccessful = false;
	String out = "";
	String err = "";

	StringBuffer cmd = getCommand()
	    .append(' ')
	    .append(sourceFile.toString());

	KSystemCommand c = null;

	try {

	    if (classFile.exists()) {

		long lastMod = classFile.lastModified();
		
		c = new KSystemCommand(cmd.toString());
		c.run();
		
		wasSuccessful = lastMod < classFile.lastModified();
		
	    } else {
		
		c = new KSystemCommand(cmd.toString());
		c.run();
		
		wasSuccessful = classFile.exists();
		
	    }

	} catch (IOException ioex) {

	    StringWriter sw = new StringWriter();
	    PrintWriter pw = new PrintWriter(sw);
	    ioex.printStackTrace(pw);
	    err = sw.toString();
	    wasSuccessful = false;

	}

	if (c != null) {
	    out += c.out;
	    err += c.err;
	}

	int rc = wasSuccessful ? RC_COMPILE_SUCCESS : RC_COMPILE_FAILURE;

	if (hasListeners()) 
	    fire(new ProxyCompileEvent(this, 
				       className,
				       cmd.toString(), 
				       out, 
				       err, 
				       rc));
	return rc;
    }

    // ================================
    // Other Methods 
    // ================================

    protected StringBuffer getCommand() 
    {
	StringBuffer cmd = new StringBuffer(commandName);
	
	if (opts != null) {
	    int count = 0;
	    Iterator i = opts.iterator();
	    while (i.hasNext()) {
		cmd.append(' ').append(i.next());
	    }
	}

	if (dst != null)
	    cmd.append(" -d ").append(dst);
	
//  	if (sourcepath != null)
//  	    cmd.append(" -sourcepath ").append(sourcepath);
	
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

    // ================================
    // Fields 
    // ================================

    // compiler command name (jikes, javac)
    protected String commandName;	
}
