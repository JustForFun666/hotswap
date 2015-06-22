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

/**
 * Base <code>ProxyClass</code> implementation.  The
 * <code>ProxyClass</code> holds a reference to a <code>Class</code>
 * whose implementation may change during the life of an application;
 * it functions as a factory for <code>Proxy</code> instances.  Each
 * <code>ProxyClass</code> monitors the timestamps of the sourcefile
 * and the classfile of the corresponding <code>Class</code> object.
 * It will reload a new <code>Class</code> implementation if the
 * sourcefile becomes newer then the classfile.
**/
public abstract class ProxyClass
{
    // ================================
    // Constructors
    // ================================

    ProxyClass(ProxyCompiler compiler, Class cls)
    {
	this.compiler = compiler;
	this.cls = cls;
	this.sourceFile = compiler.getSourceFile(cls.getName());
	this.classFile = compiler.getClassFile(cls.getName());
	this.proxies = new Stack();
    }

    // ================================
    // TEHC Methods
    // ================================

    public int hashCode()
    {
	return this.cls.hashCode();
    }

    public boolean equals(Object other)
    {
	if (other == this)
	    return true;

	if (!(other instanceof KProxy))
	    return false;

	KProxy that = (KProxy)other;

	// Compare class equality
	return this.cls.equals(that.cls);
    }

    // ================================
    // Abstract Methods
    // ================================

    /**
     * JDK1.2 or JDK1.3 Factory method; Creates a new
     * <code>Proxy</code> for this <code>ProxyClass</code> having no
     * constructor arguments (though arguments can be set later on the
     * <code>Proxy</code> itself).
    **/
    public abstract Proxy newInstance();

    /**
     * JDK1.2 or JDK1.3 Factory method; Creates a new
     * <code>Proxy</code> having the given constructor arguments.  The
     * parameters are gathered by fetching the class for each
     * argument.
    **/
    public abstract Proxy newInstance(Object[] args);

    /**
     * JDK1.2 or JDK1.3 Factory method; Creates a new
     * <code>Proxy</code> having the given constructor parameters and
     * arguments.
    **/
    public abstract Proxy newInstance(Class[] params, Object[] args);

    /**
     * JDK1.3 Factory method; Creates a new <code>Proxy</code> for
     * this <code>ProxyClass</code> having no constructor arguments
     * using the given <code>ProxyInvocationHandler</code> (H) instance.
     *
     * @exception <code>UnsupportedOperationException</code> if this
     * is not a JDK1.3 proxy class.
    **/
    public abstract Proxy newInstanceH(Object h);

    /**
     * JDK1.3 Factory method; Creates a new <code>Proxy</code> having
     * the given constructor arguments using the given
     * <code>ProxyInvocationHandler</code> (H) instance.  The parameters are
     * gathered by fetching the class for each argument.
     *
     * @exception <code>UnsupportedOperationException</code> if this
     * is not a JDK1.3 proxy class.
    **/
    public abstract Proxy newInstanceH(Object h, Object[] args);

    /**
     * JDK1.3 Factory method; Creates a new <code>Proxy</code> having
     * the given constructor parameters and arguments using the given
     * <code>ProxyInvocationHandler</code> (H) instance. This invocation
     * handler will be passed to the
     * <code>java.lang.reflect.Proxy.newProxyInstance</code> method
     * and MUST extend
     * <code>org.inxar.hotswap.ProxyInvocationHandler</code>.  <P>The
     * parameter type is given here as <code>Object</code> rather than
     * <code>ProxyInvocationHandler</code> in order to avoid classloading
     * of <code>JDK1.3</code> dependent classes in a
     * <code>JDK1.2</code> environment.
     *
     * @exception <code>UnsupportedOperationException</code> if this
     * is not a JDK1.3 proxy class.
    **/
    public abstract Proxy newInstanceH(Object h, Class[] params, Object[] args);

    // ================================
    // Public Methods
    // ================================

    /**
     * Returns <code>true</code> if this method is a
     * <code>JDK1.3</code> proxy class.
    **/
    public boolean isJDK13()
    {
	return false;
    }

    /**
     * Returns the parent <code>ProxyCompiler</code>.
     **/
    public ProxyCompiler getCompiler()
    {
	return compiler;
    }

    /**
     * Returns the name of the class being proxied. Equivalent to
     * <code>ProxyClass.getInstance().getName()</code>.
    **/
    public String getName()
    {
	return cls.getName();
    }

    /**
     * Returns <code>true</code> if the sourcefile is newer than the
     * classfile, <code>false</code> if the sourcefile is not never
     * than the classfile, or <code>null</code> if it cannot be
     * determined.
    **/
    synchronized public Boolean hasChanged()
    {
        return dirty;
    }

    public void setChanged() {
        dirty = true;
        fire(new ProxyClassDirtyEvent(this));
    }

    /**
     * Returns <code>true</code> if the sourcefile is newer than the
     * classfile, <code>false</code> if the sourcefile is not newer
     * than the classfile, or <code>null</code> if it cannot be
     * determined.
    **/
    synchronized public Boolean isSourceNewer()
    {
	// Check the status of the file.
	switch (compiler.filestat(sourceFile, classFile)) {

	    /* if it cannot be determined, propogate this uncertainty */
	case ProxyCompiler.RC_FILESTAT_UNKNOWN:
	    return null;

	    /* if the sourcefile does not exist. */
	case ProxyCompiler.RC_FILESTAT_SOURCE_MISSING:
	    throw new ProxyException("Unknown source file "+sourceFile.toString());

	    /* if the classfile is current there is no need run the
               compilation; return trivial */
	case ProxyCompiler.RC_FILESTAT_CLASS_CURRENT:
	    return Boolean.FALSE;

	    /* if the classfile is expired then continue processing */
	case ProxyCompiler.RC_FILESTAT_CLASS_EXPIRED:
	    return Boolean.TRUE;

	default:
	    throw new InternalError(); // bad coding
	}
    }

    /**
     * Returns the default <code>isAutoEnqueue</code> value for
     * <code>Proxy</code> instances constructed from this
     * <code>ProxyClass</code>.
    **/
    synchronized public boolean isAutoEnqueue()
    {
	return isAutoEnqueue;
    }

    /**
     * Sets the default <code>isAutoEnqueue</code> value for
     * <code>Proxy</code> instances constructed from this
     * <code>ProxyClass</code>.  If set to <code>true</code>, all
     * subsequent Proxy instances returned by any of the
     * <code>newProxy()</code> methods will be configured to
     * automatically enqueue in hotswap transactions.  If a Proxy
     * instance automatically enqueues itself it cannot be garbage
     * collected and therefore requires the
     * <code>Proxy.hotswap_release()</code> method to be called
     * explicitly by end-user code.
    **/
    synchronized public void isAutoEnqueue(boolean isAutoEnqueue)
    {
	this.isAutoEnqueue = isAutoEnqueue;
    }

    /**
     * Returns the class instance that this <code>ProxyClass</code>
     * contains.
    **/
    public Class getClassInstance()
    {
	return cls;
    }

    /**
     * Adds the given listener of <code>ProxyClassSwapEvent</code>s.
    **/
    synchronized public void addClassSwapListener(ProxyEventListener l)
    {
	if (listeners == null)
	    listeners = new LinkedList();
	listeners.add(l);
    }

    /**
     * Removes the given listener of
     * <code>ProxyClassSwapEvent</code>s.
    **/
    synchronized public void removeClassSwapListener(ProxyEventListener l)
    {
	if (listeners != null)
	    listeners.remove(l);
    }

    public ProxyCompiler.Resource getSourceFile() {
        return sourceFile;
    }

    public ProxyCompiler.Resource getClassFile() {
        return classFile;
    }

    // ================================
    // Package Methods
    // ================================

    /**
     * Attempts to recompile and reload the class.  If successful,
     * return <code>true</code>.  If unsuccessful, the current
     * <code>Class</code> instance is retained.
     *
     * <P>
     *
     * If classloading is successful, any waiting
     * <code>ProxyEventListeners</code> will be notified of a
     * <code>ProxyClassSwapEvent</code>.
    **/
    synchronized boolean hotswap()
    {
	// If the source has NOT changed we return true immediately.
	if (!dirty)
	    return false;

	// If there are no proxies that will be involved in the
	// hotswap, then dont bother. Only hotswap if it will make a
	// difference to the end user.
	if (!hasProxies())
	    return false;

	// Recompile the class.
	int rc = compiler.compile(cls.getName(), sourceFile, classFile);

	// If it did NOT recompile, abort.
	if (rc != ProxyCompiler.RC_COMPILE_SUCCESS)
	    return false;

	// Ok, the class has been successfully recompiled.  Next step
	// is to reload the class.

	Class oldClass = this.cls;
	Class newClass = null;

	try {

	    // If the load succeeds, the classloader will cache the
	    // object automatically.
	    newClass = newClassInstance();

	} catch (Exception ex) {

	    return false;

	}

	// Create a new list that will contain all successfully
	// prepared proxies.
	List prepared = new ArrayList(proxies.size());
	Iterator i;
	KProxy p;
	boolean commit = true;
	try {

	    // First prepare all proxies.
	    while (!proxies.isEmpty()) {
		p = (KProxy)proxies.pop();
		if (p.hotswap_prepare(newClass))
		    prepared.add(p);
		else
		    commit = false;
	    }

	} catch (Exception ex) {
	    /* might be nice to know the error */
	    System.out.println("Exception caught during hotswap transaction preparation:");
	    ex.printStackTrace();
	    commit = false;
	}

	if (commit) {

	    // Do the commit
	    i = prepared.iterator();
	    while (i.hasNext())
		((KProxy)i.next()).hotswap_commit();

	    // The transaction has succeeded.  Drop the old class and
	    // replace it with the new.
	    this.cls = newClass;

	    // Finally, notify the listeners.
	    if (hasListeners())
		fire( new ProxyClassSwapEvent(this, oldClass, newClass) );

	} else {

	    // Rollback any proxies that successfully prepared.
	    if (prepared.size() > 0) {
		i = prepared.iterator();
		while (i.hasNext())
		    ((KProxy)i.next()).hotswap_rollback();
	    }

	}

	return commit;
    }

    synchronized boolean hasListeners()
    {
	return listeners != null && listeners.size() > 0;
    }

    synchronized boolean hasProxies()
    {
	return proxies.size() > 0;
    }

    synchronized void enqueue(KProxy proxy)
    {
	proxies.add(proxy);
    }

    synchronized void dequeue(KProxy proxy)
    {
	proxies.remove(proxy);
    }

    synchronized boolean isEnqueued(KProxy proxy)
    {
	return proxies.contains(proxy);
    }

    // ================================
    // Protected Methods
    // ================================

    /**
     * Returns a new intance of this <code>Class</code>.
    **/
    synchronized protected Class newClassInstance()
    {
	try {

	    return compiler.loadClass(cls.getName());

	} catch (ClassNotFoundException ex) {
	    throw new ProxyException(ex);
	}
    }

    synchronized protected void fire(ProxyEvent evt)
    {
	if (hasListeners()) {
	    Iterator i = listeners.iterator();
	    while (i.hasNext())
		((ProxyEventListener)i.next()).notify(evt);
	}
    }

    // ================================
    // Fields
    // ================================

    protected ProxyCompiler.Resource sourceFile;
    protected ProxyCompiler.Resource classFile;
    protected Stack stack;
    protected Class cls;
    protected List listeners;
    protected Stack proxies;
    protected ProxyCompiler compiler;
    protected boolean isAutoEnqueue;

    // Boolean field to be set to true when hotswap should occur.
    protected boolean dirty;

}
