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

import java.io.File;
import java.io.IOException;
import java.util.Stack;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;

/**
 * Concrete <code>ProxyClass</code> implementation that supports
 * dynamic proxy classes.
 **/
class KJDK13ProxyClass extends ProxyClass
{
    // ================================
    // Constructors
    // ================================

    KJDK13ProxyClass(ProxyCompiler compiler, Class cls)
    {
	this(compiler, cls, cls.getInterfaces());
    }

    KJDK13ProxyClass(ProxyCompiler compiler, Class cls, Class[] interfaces)
    {
	super(compiler, cls);

        // log("jdk13 class impl: " + cls.getName());
        // log("classloader: " + cls.getClassLoader().getClass().getName());
        // log("interfaces: " + interfaces);

        //for (Class i : interfaces) {
          //log("- " + i.getName());
        //}

	if (newProxyInstance == null)
	    throw new ProxyException
		("Cannot instantiate: cannot find java.lang.reflect.Proxy.  "+
		 "Are you sure you are in a JDK1.3 environment?");

	if (interfaces == null || interfaces.length == 0)
	    throw new ProxyException
		(cls.toString()+" must implement at least one interface.");

	// Make a new array such that we *also* implement Proxy
	Class[] dst = new Class[interfaces.length + 1];
	System.arraycopy(interfaces, 0, dst, 0, interfaces.length);
	dst[interfaces.length] = Proxy.class;
	this.interfaces = dst;
    }

    // ================================
    // ProxyClass Methods
    // ================================

    public boolean isJDK13()
    {
	return true;
    }

    public Proxy newInstance()
    {
        if (dirty)
            hotswap();

	//return newJDK13Proxy(new KProxy(this, isAutoEnqueue), null);
	return newJDK13Proxy(new KProxy(this, true), null);
    }

    public Proxy newInstance(Object[] args)
    {
        if (dirty)
            hotswap();
	return newJDK13Proxy(new KProxy(this, isAutoEnqueue, args), null);
    }

    public Proxy newInstance(Class[] params, Object[] args)
    {
        if (dirty)
            hotswap();
	return newJDK13Proxy(new KProxy(this, isAutoEnqueue, params, args), null);
    }

    public Proxy newInstanceH(Object h)
    {
	if (h == null)
	    throw new NullPointerException
		("A non-null ProxyInvocationHandler argument is required.");

        if (dirty)
            hotswap();
	return newJDK13Proxy(new KProxy(this, isAutoEnqueue), h);
    }

    public Proxy newInstanceH(Object h, Object[] args)
    {
	if (h == null)
	    throw new NullPointerException
		("A non-null ProxyInvocationHandler argument is required.");

        if (dirty)
            hotswap();

	return newJDK13Proxy(new KProxy(this, isAutoEnqueue, args), h);
    }

    public Proxy newInstanceH(Object h, Class[] params, Object[] args)
    {
	if (h == null)
	    throw new NullPointerException
		("A non-null ProxyInvocationHandler argument is required.");

        if (dirty)
            hotswap();

	return newJDK13Proxy(new KProxy(this, isAutoEnqueue, params, args), h);
    }

    // ================================
    // Other Methods
    // ================================

    protected Proxy newJDK13Proxy(Proxy proxy, Object h)
    {
	try {

	    // Instantiate a new default invocation handler if one has
	    // not been provided.
	    if (h == null)
		h = dh.newInstance();

	    // Call ProxyInvocationHandler.setProxy(proxy).
	    try {
		setProxy.invoke(h, new Object[]{ proxy });
	    } catch (IllegalArgumentException iaex) {
		throw new ProxyException
		    ("InvocationHandler argument MUST "+
		     "extend ProxyInvocationHandler", iaex);
	    }

	    // Finally, create a new hotswap Proxy using java proxy
	    // reflection.
	    return (Proxy)newProxyInstance.invoke(null, new Object[] {
		compiler.parent,
		interfaces,
		h
	    });

	} catch (Exception ex) {
	    throw new ProxyException(ex);
	}
    }

    // ================================
    // Fields Methods
    // ================================

    protected Class[] interfaces;

    // ================================
    // Class Fields and Methods
    // ================================

    // org.inxar.hotswap.ProxyInvocationHandler
    static Class dh = null;

    // org.inxar.hotswap.ProxyInvocationHandler.setProxy
    static Method setProxy = null;

    // java.lang.reflect.Proxy.newProxyInstance
    static Method newProxyInstance = null;

    static {
	try {

	    dh = Class.forName("org.inxar.hotswap.ProxyInvocationHandler");
	    setProxy = dh.getMethod("setProxy", new Class[]{ Proxy.class });

	    Class h_class =
		Class.forName("java.lang.reflect.InvocationHandler");

	    newProxyInstance = Class.forName("java.lang.reflect.Proxy")
		.getMethod("newProxyInstance", new Class[] {
		    ClassLoader.class,
		    Class[].class,
		    h_class
		});

	} catch (Exception ex) {
	    ex.printStackTrace();
	}
    }
}
