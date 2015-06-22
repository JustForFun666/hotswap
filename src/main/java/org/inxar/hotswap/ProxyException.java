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
 * General <code>RuntimeException</code> used to wrap
 * reflection-related and other exception conditions.
**/
public class ProxyException extends RuntimeException
{
    /**
     * Constructs an <code>ProxyException</code> with no specified
     * detail message.
     **/
    public ProxyException() 
    {
	super();
    }

    /**
     * Constructs an <code>ProxyException</code> with detail
     * message, <code>s</code>.
     * @param s the detail message
     **/
    public ProxyException(String s) 
    {
	super(s);
    }

    /**
     * Constructs an <code>ProxyException</code> with detail message,
     * <code>s</code>, and detail exception <code>ex</code>.
     *
     * @param s detail message
     * @param ex detail exception
     **/
    public ProxyException(String s, Throwable ex) 
    {
	super(s);
	detail = ex;
    }

    /**
     * Constructs an <code>ProxyException</code> with detail
     * exception <code>ex</code>.
     *
     * @param ex detail exception
     **/
    public ProxyException(Throwable ex) 
    {
	super();
	detail = ex;
    }

    /**
     * Produces the message, include the message from the nested
     * exception if there is one.
     * @return the message
     **/
    public String getMessage() 
    {
	if (detail == null) 
	    return super.getMessage();
	else
	    return super.getMessage() + 
		"; nested exception is: \n\t" +
		detail.toString();
    }

    /**
     * Prints the composite message and the embedded stack trace to
     * the specified stream <code>ps</code>.
     * @param ps the print stream
     **/
    public void printStackTrace(java.io.PrintStream ps) 
    {
	if (detail == null) {
	    super.printStackTrace(ps);
	} else {
	    synchronized(ps) {
		ps.println(this);
		detail.printStackTrace(ps);
	    }
	}
    }

    /**
     * Prints the composite message to <code>System.err</code>.
     **/
    public void printStackTrace() 
    {
	printStackTrace(System.err);
    }

    /**
     * Prints the composite message and the embedded stack trace to
     * the specified print writer <code>pw</code>.
     * @param pw the print writer
     * @since 1.2
     **/
    public void printStackTrace(java.io.PrintWriter pw)
    {
	if (detail == null) {
	    super.printStackTrace(pw);
	} else {
	    synchronized(pw) {
		pw.println(this);
		detail.printStackTrace(pw);
	    }
	}
    }

    /**
     * Nested <code>Exception</code> to hold wrapped component
     * exceptions.
     **/
    public Throwable detail;
}
