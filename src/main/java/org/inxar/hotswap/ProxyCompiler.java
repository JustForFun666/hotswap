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
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.Collection;
import java.util.Collections;

/**
 * Base <code>ProxyCompiler</code> implementation; it is responsible
 * for mediating recompilation.  Subclasses need implement the
 * <code>compile</code> methods.
 **/
public abstract class ProxyCompiler
{
    public static final String SEMVER = "0.8.1";

    /**
     * Compilation Return Code meaning that a compilation was NOT
     * attempted because it was deemed unnecessary (ie the problem
     * was trivial).
     **/
    public static final int RC_COMPILE_TRIVIAL = -1;

    /**
     * Compilation Return Code meaning that a compilation was
     * attempted and completed successfully.
     **/
    public static final int RC_COMPILE_SUCCESS = 0;

    /**
     * Compilation Return Code meaning that a compilation was
     * attempted and NOT completed successfully.
     **/
    public static final int RC_COMPILE_FAILURE = 1;

    /**
     * Compilation Return Code meaning that a compilation was NOT
     * attempted because an the sourcefile does not exist.
     **/
    public static final int RC_COMPILE_SOURCE_MISSING = 2;

    /**
     * Compilation Return Code meaning complete lack of knowledge of
     * the outcome.
     **/
    public static final int RC_COMPILE_UNKNOWN = 4;

    /**
     * Filestat Return Code meaning the classfile is current; no
     * compilation is necessary.
     **/
    public static final int RC_FILESTAT_CLASS_CURRENT = 0;

    /**
     * Filestat Return Code meaning the classfile is NOT current; a
     * compilation is recommended.
     **/
    public static final int RC_FILESTAT_CLASS_EXPIRED = 1;

    /**
     * Filestat Return Code meaning the sourcefile does not exist.
     **/
    public static final int RC_FILESTAT_SOURCE_MISSING = 2;

    /**
     * Filestat Return Code meaning the outcome of the filestat could
     * not be determined.
     **/
    public static final int RC_FILESTAT_UNKNOWN = 3;

    /**
     * Cached instance of
     * <code>System.getProperty("path.separator")</code> (unix = ':',
     * dos = ';').
     **/
    static String PATHSEP = System.getProperty("path.separator");

    // ================================
    // Constructors
    // ================================

    protected ProxyCompiler()
    {
	this(new ProxyClassWatcher());
    }

    protected ProxyCompiler(ProxyClassMonitor monitor)
    {
	this(ProxyCompiler.class.getClassLoader(), monitor);
    }

    protected ProxyCompiler(ClassLoader parent)
    {
	this(parent, new ProxyClassWatcher());
    }

    protected ProxyCompiler(ClassLoader parent, ProxyClassMonitor monitor)
    {
	this.parent = parent;
        this.monitor = monitor;
        monitor.setCompiler(this);
	init();
    }

    protected void init()
    {
	this.proxyClasses = new HashMap();
    }

    // ================================
    // Public Methods
    // ================================

    /**
     * Returns the Object where the source files are rooted.  The
     * run-time type of this <code>Object</code> may be a
     * <code>String</code>, <code>File</code>, or <code>URL</code>.
     **/
    synchronized public void setSourcepath(Object sourcepath)
    {
	if (sourcepath == null ||
	    sourcepath instanceof String ||
	    sourcepath instanceof File)
	    this.sourcepath = sourcepath;

	else
	    throw new IllegalArgumentException
		("Sourcepath must be a String, File, or null.");
    }

    /**
     * Sets the location where the source files are rooted (for
     * example <code>/home/joeuser/myproject/src</code>).  The
     * argument is an object, but implementations should accept a
     * String (meaning a filename or URI), a <code>File</code>, or a
     * <code>URL</code>.
     **/
    public Object getSourcepath()
    {
	return this.sourcepath;
    }

    /**
     * Returns the local pathname where the compiled classes will be
     * written.  If it does not exist, the path will be created.
     **/
    synchronized public void setDestinationpath(String dst)
    {
	this.dst = dst;
        File dir = new File(dst);
        if (dir.exists()) {
            if (!dir.isDirectory()) {
                throw new IllegalArgumentException
                    ("Illegal destination directory (not a directory): " + dst);
            }
        } else {
            dir.mkdirs();
        }
    }

    /**
     * Sets the pathname where the class files should be written (for
     * example <code>/home/joeuser/myproject/classes</code>).
     **/
    public String getDestinationpath()
    {
	return this.dst;
    }

    /**
     * Returns a <code>List</code> of the option entries.  Users
     * should interact with the <code>List</code> directly to add
     * Object instances whose <code>toString()</code> method will
     * render appropriately.  Each entry is printed as-is, no dashes
     * are added by the compiler.
     **/
    synchronized public List getOptions()
    {
	if (opts == null)
	    opts = new LinkedList();
	return opts;
    }

    /**
     * Returns a <code>List</code> of the classpath entries.  Users
     * should interact with the <code>List</code> directly to add
     * Object instances whose <code>toString()</code> method will
     * render appropriately.
     **/
    synchronized public List getClasspath()
    {
	if (cps == null)
	    cps = new LinkedList();
	return cps;
    }

    /**
     * Adds the given listener of <code>ProxyCompileEvent</code>s.
     **/
    synchronized public void addCompileListener(ProxyEventListener l)
    {
	if (listeners == null)
	    listeners = new LinkedList();
	listeners.add(l);
    }

    /**
     * Removes the given listener of <code>ProxyCompileEvent</code>s.
     **/
    synchronized public void removeCompileListener(ProxyEventListener l)
    {
	if (listeners != null)
	    listeners.remove(l);
    }

    /**
     * Returns the <code>Collection</code> of <code>ProxyClass</code>
     * instances in this compiler instance.
     **/
    synchronized public Collection getLoadedClasses()
    {
	return Collections.synchronizedCollection(proxyClasses.values());
    }

    /**
     * JDK1.2 Factory method; Creates a new <code>ProxyClass</code>
     * for the given className.  If a <code>ProxyClass</code> having
     * the given name already exists it will be returned instead.
     **/
    synchronized public ProxyClass loadJDK12(String className)
    {
	ProxyClass pc = getJDK12(className);

	if (pc != null)
	    return pc;

	try {

  	    pc = new KJDK12ProxyClass(this, loadClass(className));
  	    add(className, pc);
  	    return pc;

	} catch (ClassNotFoundException cnfex) {
	    throw new ProxyException(cnfex);
	}
    }

    /**
     * JDK1.3 Factory method; Creates a new <code>ProxyClass</code>
     * for the given className that will use the <code>JDK1.3</code>
     * dynamic proxy class mechanism to create a dynamic proxy class
     * that will be a factory for <code>Proxy</code> instances having
     * the given interfaces IN ADDITION TO the <code>Proxy</code>
     * interface.  If a <code>ProxyClass</code> having the given name
     * already exists it will be returned instead.
     *
     * <P>If this method is chosen to construct the
     * <code>ProxyClass</code>, new <code>Proxy</code> instances will
     * be assignable to the given interfaces.  Therefore, not only
     * will the return argument of proxyClass.newProxy() be assignable
     * to class <code>Proxy</code>, it will <i>also</i> be assignable
     * to <code>Foo</code>.  Therefore, the following idiom works:
     *
     * <pre>
     * ProxyClass cls = compiler.load("FooImpl");
     * Foo foo = (Foo)cls.newProxy();
     * </pre>
     *
     * This <code>Foo</code> implementation will transparently monitor
     * implementation changes and hotswap itself as necessary.
     **/
    synchronized public ProxyClass load(String className, Class[] interfaces)
    {
	ProxyClass pc = getJDK13(className);

	if (pc != null)
	    return pc;

	try {

  	    pc = new KJDK13ProxyClass(this, loadClass(className), interfaces);
  	    add(className, pc);
  	    return pc;

	} catch (ClassNotFoundException cnfex) {
	    throw new ProxyException(cnfex);
	}
    }

    /**
     * JDK1.3 Factory method; Convenience method that uses the
     * published list of interfaces on the Class object.  This is
     * equivalent to <code>load(className,
     * Class.forName(className).getInterfaces())</code>.
     **/
    synchronized public ProxyClass load(String className)
    {
	ProxyClass pc = getJDK13(className);

	if (pc != null)
	    return pc;

	try {

  	    pc = new KJDK13ProxyClass(this, loadClass(className));
  	    add(className, pc);
  	    return pc;

	} catch (ClassNotFoundException cnfex) {
	    throw new ProxyException(cnfex);
	}
    }

    /**
     * Add a proxy class.
     */
    protected void add(String className, ProxyClass cls) {
        proxyClasses.put(className, cls);
        monitor.watch(cls);
    }

    /**
     * Removes the given <code>ProxyClass</code> instance from this
     * cache and returns the cached instance or <code>null</code> if
     * no such <code>ProxyClass</code> exists in this cache.
     **/
    synchronized public ProxyClass remove(ProxyClass cls) {
        monitor.unwatch(cls);
	return (ProxyClass)proxyClasses.remove(cls.getName());
    }

    /**
     * Returns the <code>ProxyClass</code> having the given name or
     * <code>null</code> if no such <code>ProxyClass</code> exists.
     * If the given classname is NOT a JDK13 <code>ProxyClass</code>,
     * a <code>ProxyException</code> will be thrown.
     **/
    synchronized public ProxyClass getJDK13(String className)
    {
	ProxyClass pc = (ProxyClass)proxyClasses.get(className);

	if (pc != null)
	    if (!pc.isJDK13())
		throw new ProxyException("Attempted fetch of non JDK13ProxyClass "+
					 "through wrong interface.  Use "+
					 "load() instead.");
	    else
		return pc;

	else
	    return null;
    }

    /**
     * Returns the <code>ProxyClass</code> having the given name or
     * <code>null</code> if no such <code>ProxyClass</code> exists.
     * If the given classname is a JDK13 <code>ProxyClass</code>, a
     * <code>ProxyException</code> will be thrown.
     **/
    synchronized public ProxyClass getJDK12(String className)
    {
	ProxyClass pc = (ProxyClass)proxyClasses.get(className);

	if (pc != null)
	    if (pc.isJDK13())
		throw new ProxyException("Attempted fetch of JDK13ProxyClass "+
					 "through wrong interface.  Use "+
					 "loadJDK12() instead.");
	    else
		return pc;

	else
	    return null;
    }

    // ================================
    // Package Methods
    // ================================

    /**
     * Compiles the given source file having the given name, firing a
     * <code>ProxyRecompileEvent</code> to any registered
     * listeners.The return value is one of the
     * <code>RC_COMPILE_XXXXXXX</code> constants in this class.
     **/
    synchronized public int compile(String className)
    {
	return compile(className,
		       getSourceFile(className),
		       getClassFile(className));
    }

    /**
     * Compiles the given source file having the given name and
     * resources, firing a <code>ProxyRecompileEvent</code> to any
     * registered listeners.  The return value is one of the
     * <code>RC_COMPILE_XXXXXXX</code> constants in this class.
     **/
    synchronized int compile(String className, Resource sourceFile, Resource classFile)
    {
	throw new UnsupportedOperationException();
    }

    /**
     * Returns the sourcefile <code>Resource</code> for the given
     * classname.  The location of the sourcecode is dependent upon
     * the run-time type of the sourcepath <code>Object</code> (see
     * <code>setSourcepath(Object)</code>).
     **/
    synchronized Resource getSourceFile(String className)
    {
	StringBuffer b = new StringBuffer();

	if (sourcepath != null) {
	    if (sourcepath instanceof String) {
		String s = (String)sourcepath;
		if (s.startsWith("file:"))
		    b.append(s.substring(5));
		else
		    b.append(s);
	    } else if (sourcepath instanceof File) {
		File f = (File)sourcepath;
		b.append(f.getAbsolutePath());
	    }
	}

	if (b.length() > 0 && b.charAt(b.length() - 1) != File.separatorChar)
	    b.append(File.separatorChar);

	b.append(className.replace('.' , File.separatorChar))
	    .append(".java");

	return new KFileResource(new File(b.toString()));
    }

    /**
     * Returns a File resource for the given classname.  It is always
     * assumed that the classfile exists on the filesystem.
     **/
    synchronized Resource getClassFile(String className)
    {
 	StringBuffer b = new StringBuffer();

	if (dst != null) {
	    b.append(dst);
	    if (dst.charAt(dst.length() - 1) != File.separatorChar)
		b.append(File.separatorChar);
	}

	b.append(className.replace('.' , File.separatorChar))
	    .append(".class");

	return new KFileResource(new File(b.toString()));
    }

    /**
     * Returns the status of the sourcefile and classfile as one of
     * the <code>RC_FILESTAT_XXX</code> constants in this class.
     **/
    synchronized int filestat(Resource sourceFile, Resource classFile)
    {
	try {

	    if (!sourceFile.exists())
		return RC_FILESTAT_SOURCE_MISSING;


	    if (!classFile.exists())
		return RC_FILESTAT_CLASS_EXPIRED;

	    return sourceFile.lastModified() > classFile.lastModified()
		? RC_FILESTAT_CLASS_EXPIRED
		: RC_FILESTAT_CLASS_CURRENT;

	} catch (IOException ioex) {

	    ioex.printStackTrace();
	    return RC_FILESTAT_UNKNOWN;

	}
    }

    /**
     * Tries to load the given classname using an internal
     * classloader.
     **/
    synchronized Class loadClass(String name) throws ClassNotFoundException
    {
	return new KFileClassLoader(parent, this).loadClass(name);
    }

    String formatClasspath() {
        List list = getClasspath();
	if (list == null && list.size() == 0) {
            return "";
        }
        StringBuilder cp = new StringBuilder();
        int count = 0;
        Iterator i = cps.iterator();
        while (i.hasNext()) {
            if (count++ > 0)
                cp.append(PATHSEP);
            cp.append(i.next().toString());
        }
        return cp.toString();
    }

    // ================================
    // Protected Methods
    // ================================

    /**
     * Sends <code>ProxyRecompileEvent</code> notification to all
     * listeners.
     **/
    synchronized protected void fire(ProxyCompileEvent evt)
    {
	if (hasListeners()) {
	    Iterator i = listeners.iterator();
	    while (i.hasNext())
		((ProxyEventListener)i.next()).notify(evt);
	}
    }

    /**
     * Returns <code>true</code> if there is at least one listener.
     **/
    synchronized protected boolean hasListeners()
    {
	return listeners != null && listeners.size() > 0;
    }

    protected void log(String msg) {
        System.out.println("["+this.getClass().getName()+"] " + msg);
    }

    // ================================
    // Instance Fields
    // ================================

    // A table of ProxyClass objects keyed by className.
    protected Map proxyClasses;

    protected List cps;		// classpaths
    protected List opts;	// options
    protected List listeners;	// event listeners
    protected String dst;	// code destination
    protected Object sourcepath; // the sourcepath
    protected ClassLoader parent; // the model classloader
    protected ProxyClassMonitor monitor; // checks if classes need recompiling

    /**
     * Abstraction of a <code>File</code>, <code>URL</code>, or other
     * entity that is used to hold bytes of data.
     **/
    public interface Resource
    {
	/**
	 * Returns the time of last modification.
         **/
	long lastModified() throws IOException;

	/**
	 * Returns the number of bytes in the
	 * <code>InputStream</code>.
         **/
	long length() throws IOException;

	/**
	 * Returns <code>true</code> if the resource exists.
         **/
	boolean exists() throws IOException;

	/**
	 * Returns an <code>InputStream</code> used to read the
	 * resource data.
         **/
	InputStream getInputStream() throws IOException;

	/**
	 * Returns a <code>String</code> representation of the
	 * <code>Resource</code>.
         **/
	String toString();

	/**
	 * Returns a <code>File</code> representation of the
	 * <code>Resource</code>.
         **/
	java.io.File toFile();
    }


}

/*

 *
 * <P><b>About the <code>InvocationHandler</code></b>:<br> Though
 * listed here as an <code>Object</code>, the argument type MUST
 * extend <code>AInvocationHandler</code>.  The reason for listing
 * it here as an <code>Object</code> is to avoid classloading of
 * <code>JDK1.3</code>-dependent classes and therefore useable in
 * a <code>JDK1.2</code> environment.  If the invocationHandler
 * does not extend AInvocationHandler, an
 * <code>IllegalArgumentException</code> will be thrown.

 * <P>Since this is supported only in <code>JDK1.3</code>
 * implementations, the <code>invocationHandler</code> parameter
 * is listed as an <code>Object</code> rather than the actual
 * required type <code>java.lang.reflect.InvocationHandler</code>.
 * It is necessary to dumb-down the type-system to prevent
 * <code>NoClassDefFoundException</code>s in a JDK1.2 virtual
 * machine.  Nevertheless, an
 * <code>IllegalArgumentException</code> will be throw of the
 * run-time-type of the invocationHandler object is insufficient.
 *

 */
