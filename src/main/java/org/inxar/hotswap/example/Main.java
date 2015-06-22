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
package org.inxar.hotswap.example;

import java.util.Date;
import org.inxar.hotswap.*;

/**
 * Main entry method takes the name of a class that implements
 * <code>Component</code> to create a <code>ProxyClass</code> for that
 * is assumed to reside in the <code>examples</code> directory; a
 * Proxy is then created and a hotswap thread is started.
**/
public class Main
{
    // ================================
    // Class Methods
    // ================================

    public static void main(String[] argv)
    {
	if (argv.length != 4) {
	    System.err.println("Usage: java org.inxar.hotswap.example.Main <destinationpath> <sourcepath> "+
			       "<classpath> <classname of Component implementation>");
            System.err.println(" --> expected 4 arguments, not " + argv.length);

	    System.exit(1);
	}

	String destinationpath = argv[0];
	String sourcepath = argv[1];
	String classpath = argv[2];
	String className = argv[3];
	String version = System.getProperty("java.version");
	if (version.startsWith("1.3"))
	    isJDK13 = true;

	trace("Welcome to hotswap, you are running Java version: "+version);

	// Setup an event listener
	ProxyEventListener l = new ProxyEventListener() {
		public void notify(ProxyEvent evt) {
		    System.out.println
			("[Main]["+new Date()+"] "+ evt);
		}
	    };

	// Setup compiler
	ProxyCompiler compiler = null;
	{
	    try {
		compiler = new KJavacCompiler();
	    } catch (IllegalStateException isex) {
		isex.printStackTrace();
		trace("Falling back to system javac interface (slow)...");
		compiler = new KSystemCompiler("javac");

		/* if you have jikes, use it instead */
		//compiler = new KSystemCompiler("jikes");
	    }

	    compiler.setDestinationpath(destinationpath);
	    compiler.setSourcepath(sourcepath);
	    compiler.getClasspath().add(classpath);
	    compiler.addCompileListener(l);
	}

	// Setup proxyclass
	ProxyClass cls =  (isJDK13)
	    ? compiler.load(className)
	    : compiler.loadJDK12(className);
	{
	    cls.addClassSwapListener(l);
	}

	// Setup the Proxy
	Proxy proxy = cls.newInstance();
	{
	    proxy.hotswap_addObjectSwapListener(l);
	}

	// Start the hotswap thread
	Thread t = new Thread(new Hotswapper(proxy));
	{
	    trace("Starting hotswap thread on "+className+"...");
	    t.start();
	}
    }

    public static void trace(String msg)
    {
	System.out.println("[Main]["+new Date()+"] "+msg);
    }

    // ================================
    // Class Fields
    // ================================

    static boolean isJDK13 = false;

    // ================================
    // Inner Classes
    // ================================

    static class Hotswapper implements Runnable
    {
	Hotswapper(Proxy proxy)
	{
	    this.proxy = proxy;
	}

	public void run()
	{
	    try {

		while (true) {
		    trace("----------------------------------------");

		    Component component = null;
		    if (proxy.hotswap_getProxyClass().isJDK13()) {
			component = (Component)proxy;
		    } else {
			component = (Component)proxy.hotswap();
		    }

		    System.out.println();
		    System.out.println(">>>>>>>>");
		    component.execute();
		    System.out.println("<<<<<<<<");
		    System.out.println();

		    long sleep = component.getSleepMillis();

		    if (sleep < 0)
			throw new InterruptedException();

		    trace("Going to sleep for "+sleep+" ms [if < 0 then stop]");

		    Thread.currentThread().sleep(sleep);;
		}

	    } catch (InterruptedException iex) {
		trace("Interrupted! Goodbye...");
	    }
	}

	Proxy proxy;
    }
}
