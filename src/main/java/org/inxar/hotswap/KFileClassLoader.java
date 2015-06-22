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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Set;
import java.util.Map;
import java.util.TreeMap;

/**
 * Concrete <code>ClassLoader</code> implementation that loads classes from files.
**/
class KFileClassLoader extends ClassLoader
{
    protected static boolean DEBUG = true;

    // ================================
    // Constructors
    // ================================

    KFileClassLoader(ClassLoader parent, ProxyCompiler compiler)
    {
	super(parent);
	this.compiler = compiler;
	this.classes = new TreeMap();
    }

    KFileClassLoader(ProxyCompiler compiler)
    {
	super();
	this.compiler = compiler;
	this.classes = new TreeMap();
    }

    // ================================
    // TEHC Methods
    // ================================

    /* remove underscore to enable */
    protected void _finalize() throws Throwable
    {
	Class cls = this.getClass();
	Class loader = cls.getClassLoader().getClass();

	System.out.println
	    ("Finalizing Object "+System.identityHashCode(this)+
	     " of Class "+cls.getName()+" "+System.identityHashCode(cls)+
	     " of ClassLoader "+loader.getName()+" "+System.identityHashCode(loader)+
	     ")");
    }

    // ================================
    // ClassLoader Methods
    // ================================

    protected synchronized Class findClass(String className) throws ClassNotFoundException
    {
        System.out.println("[KFileClassLoader] findClass " + className);
	FileInputStream in = null;

	ProxyCompiler.Resource sourceFile = compiler.getSourceFile(className);
	ProxyCompiler.Resource classFile = compiler.getClassFile(className);

        // Compile the class and see what happened.
	int rc = compiler.compile(className, sourceFile, classFile);

	Class oldClass = null;

	try {

            // Grab the old class out of the table and store it
            // temporarily.
	    oldClass = (Class)classes.remove(className);

	    switch (rc) {

		/* if it was unknown whether the compilation was
		   successful we shall tentatively continue, expecting to
		   probably throw a ClassNotFoundException later. */
	    case ProxyCompiler.RC_COMPILE_UNKNOWN:
		if (!classFile.exists())
		    throw new ClassNotFoundException
			("Missing classfile ("+classFile+')');
		break;

		/* if the source file is missing, its unlikely we'll
		   be able to load the class, but who knows, perhaps
		   the classfile exists but the sourcefile does not.
		   (generated from bytcode, perhaps) */
	    case ProxyCompiler.RC_COMPILE_SOURCE_MISSING:
		if (!classFile.exists())
		    throw new ClassNotFoundException
			("Missing classfile ("+classFile+
			 ") AND missing sourcefile ("+sourceFile+')');
		break;

		/* if the compilation failed, we are expecting that
		   reloading the class would be a waste.  But this
		   could be the case that (*) sourcefile exists but
		   has bad syntax AND (2) classfile exists and is okay
		   AND (3) this is the first time loading the class.
		   So we'll let things continue. */
	    case ProxyCompiler.RC_COMPILE_FAILURE:
		if (!classFile.exists())
		    throw new ClassNotFoundException
			("Missing classfile ("+classFile+
			 ") AND compilation failure "+
			 "of sourcefile ("+sourceFile+')');
		break;

		/* if the compilation was not attempted because it was
		   up to date, return the class immediately if we
		   already have it.  Otherwise continue with the
		   reload. */
	    case ProxyCompiler.RC_COMPILE_TRIVIAL:
                System.out.println("[KFileClassLoader] skipped compilation of " + className);
		if (oldClass != null)
		    return oldClass;
		else
		    break;

		/* if the compilation succeeded, go ahead as planned */
	    case ProxyCompiler.RC_COMPILE_SUCCESS:
                System.out.println("[KFileClassLoader] successfully compiled " + className);
		break;

		/* we missed a case */
	    default:
		throw new InternalError(); // bad coding

	    }

	    // If we are here it means that we definitely want to try
	    // and reload the class whether or not we already have it.
	    // Do this now.
	    long len = classFile.length();
	    byte[] buf = new byte[(int)len];

	    in = new FileInputStream(classFile.toString());
	    in.read(buf);
	    in.close();
	    in = null;

	    Class newClass = defineClass(className, buf, 0, buf.length);
	    resolveClass(newClass);

            System.out.println("[KFileClassLoader] defined & resolved " + className);

	    /* okay, we have a new class. Store it and the return it
               to the caller. */
	    classes.put(className, newClass);

	    return getClassInstance(newClass);

	} catch (Exception ex) {

	    /* rollback the oldClass into the table if it exists. */
	    if (oldClass != null)
		classes.put(className, oldClass);

	    throw new ClassNotFoundException
		("Unable to load class "+className+": "+ex.getMessage());

	} finally {
	    if (in != null)
		try { in.close(); in = null; }
		catch (Exception ex) {}
	}
    }

    // ================================
    // Overrideable Methods
    // ================================

    protected Class getClassInstance(Class newClass)
    {
	return newClass;
    }

    // ================================
    // Instance Fields
    // ================================

    protected Map classes;	// table of Class objects keyed by className.
    protected ProxyCompiler compiler;
}
