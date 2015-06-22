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

/**
 * <code>ProxyEvent</code> message sent to listeners when a
 * <code>ProxyCompiler</code> attempts a compile.
**/
public class ProxyCompileEvent extends ProxyEvent
{
    /**
     * Flexible subclass constructor.
    **/
    protected ProxyCompileEvent(Object source)
    {
	super(source);
    }

    /**
     * Standard constructor originates from the given
     * <code>ProxyCompiler</code> source and carries the command
     * string that was executed as well as any standard output and any
     * standard error.
    **/
    public ProxyCompileEvent(ProxyCompiler source, 
			     String className, 
			     String cmd, 
			     String out, 
			     String err,
			     int rc)
    {
	super(source);
	this.className = className;
	this.cmd = cmd;
	this.out = out;
	this.err = err;
	this.rc = rc;
    }

    /**
     * Prints a summary of the compilation.
    **/
    public String toString()
    {
	StringBuffer b = new StringBuffer();

	b.append("-- begin compilation summary-------------").append(NL).append(NL)
	    .append("name of class: ")
	    .append(className).append(NL)
	    .append("return code: ");
	switch (rc) {
	case ProxyCompiler.RC_COMPILE_UNKNOWN:
	    b.append("UNKNOWN");
	    break;
	    
	case ProxyCompiler.RC_COMPILE_SOURCE_MISSING:
	    b.append("MISSING SOURCEFILE");
	    break;
	    
	case ProxyCompiler.RC_COMPILE_FAILURE:
	    b.append("COMPILATION FAILURE");
	    break;
	    
	case ProxyCompiler.RC_COMPILE_TRIVIAL:
	    b.append("COMPILATION UNNECESSARY");
	    break;
	    
	case ProxyCompiler.RC_COMPILE_SUCCESS:
	    b.append("COMPILATION SUCCESS");
	    break;
	    
	    /* we missed a case */
	default:
	    throw new InternalError(); // bad coding
	    
	}
	b.append(NL).append(NL);

	switch (rc) {
	}

	if (cmd != null && cmd.length() > 0)
	    b.append(">>>>>>>> COMMAND:").append(NL)
		.append(cmd).append(NL)
		.append("<<<<<<<<").append(NL)
		.append(NL);
	if (out != null && out.length() > 0)
	    b.append(">>>>>>>> STDOUT:").append(NL)
		.append(out).append(NL)
		.append("<<<<<<<<").append(NL)
		.append(NL);
	if (err != null && err.length() > 0)
	    b.append(">>>>>>>> STDERR:").append(NL)
		.append(err).append(NL)
		.append("<<<<<<<<").append(NL)
		.append(NL);

	b.append("-- end compilation summary --------------").append(NL);
	return b.toString();
    }

    /**
     * Returns the command used to run the compilation.
    **/
    public String getCommand()
    {
	return cmd;
    }

    /**
     * Returns the name of the class that was recompiled.
    **/
    public String getClassName()
    {
	return className;
    }

    /**
     * Returns any standard output or the empty string.
    **/
    public String getOut()
    {
	return out;
    }

    /**
     * Returns any standard error or the empty string.
    **/
    public String getErr()
    {
	return err;
    }

    /**
     * Returns the return code from the
     * <code>ProxyCompiler_compile()</code> method.
    **/
    public int getReturnCode()
    {
	return rc;
    }

    protected String cmd;
    protected String out;
    protected String err;
    protected String className;
    protected int rc;
}
