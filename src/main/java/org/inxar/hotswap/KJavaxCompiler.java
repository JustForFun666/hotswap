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
import java.util.*;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileManager;
import javax.tools.StandardJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject.Kind;

/**
 * Concrete <code>ProxyCompiler</code> implementation that uses the
 * JDK1.7 javax.tools.JavaCompiler object.
 *
 * @see http://www.informit.com/articles/article.aspx?p=2027052&seqNum=2
 * @see http://docs.oracle.com/javase/7/docs/api/javax/tools/JavaCompiler.html
 * @see http://stackoverflow.com/questions/12173294/compiling-fully-in-memory-with-javax-tools-javacompiler
 * @see http://www.java2s.com/Code/Java/JDK-6/CompilingfromMemory.htm
 * @see https://github.com/OpenHFT/Java-Runtime-Compiler
 * @see http://stackoverflow.com/questions/1563909/how-to-set-classpath-when-i-use-javax-tools-javacompiler-compile-the-source

**/
public class KJavaxCompiler extends ProxyCompiler
{

    // ================================
    // Constructors
    // ================================

    /**
     * Constructs a new <code>ProxyCompiler</code> having the given
     * parent <code>ClassLoader</code>.
     **/
    public KJavaxCompiler(ClassLoader parent)
    {
	super(parent);
    }

    /**
     * Constructs a new <code>ProxyCompiler</code> having a default
     * <code>ClassLoader</code>.
     **/
    public KJavaxCompiler()
    {
	super();
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
	int rc = RC_COMPILE_UNKNOWN;
        String err = null;
        String out = null;

	try {

            // Get compiler instance
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

            // arg0: Writer.  Using null means use System.err; Most
            // stuff goes to diagnostics, this catches everything
            // else.
            Writer errWriter = new StringWriter();

            // arg2: diagnostics, but arg1 is dependent upon it.  Null
            DiagnosticCollector diagnostics = getCompilationDiagnostics();

            // arg1: fileManager. Optional locale and charset.
            StandardJavaFileManager fileManager = compiler
                .getStandardFileManager(diagnostics, null, null);

            // arg3: options
            List<String> options = getCompilationOptions();

            // arg4: classes.  Not really clear what this is for.
            List<String> classes = getCompilationClasses();

            // arg5: compilation units
            Iterable <? extends JavaFileObject> units = getCompilationUnits(fileManager, sourceFile);

            //log("[KJavaxCompiler] compiling " + units);

            // Create the compilation task
            JavaCompiler.CompilationTask task = compiler.getTask(errWriter,
                                                                 fileManager,
                                                                 diagnostics,
                                                                 options,
                                                                 classes,
                                                                 units);

            // can attach annotation processors to the task.

            wasSuccessful = task.call().booleanValue();

            rc = wasSuccessful
                ? ProxyCompiler.RC_COMPILE_SUCCESS
                : ProxyCompiler.RC_COMPILE_FAILURE;

            //log("[KJavaxCompiler] success " + wasSuccessful + ": " + rc);

            err = errWriter.toString();

            //log("[KJavaxCompiler] err " + err);

            out = formatDiagnostics(diagnostics);

            //log("[KJavaxCompiler] out " + out);

	} catch (Exception ioex) {
	    ioex.printStackTrace();
	    StringWriter sw = new StringWriter();
	    PrintWriter pw = new PrintWriter(sw);
	    ioex.printStackTrace(pw);
	    err += sw.toString();
	    wasSuccessful = false;

            //log("[KJavaxCompiler] exception " + err);
	}

	if (!wasSuccessful && (err == null || err.toString().length() == 0))
	    err = "Compilation did not complete successfully.";

	if (hasListeners())
	    fire(new ProxyCompileEvent(this, className,
				       "<compiler api invoked>", // TODO: some command
				       out, err, rc));

	return rc;
    }

    public DiagnosticCollector<JavaFileObject> getCompilationDiagnostics() {
        return new DiagnosticCollector();
    }

    public String formatDiagnostic(Diagnostic diagnostic) {
        return diagnostic.toString();
        //StringBuilder b = new StringBuilder();
        // System.out.println(diagnostic.getCode());
        // System.out.println(diagnostic.getKind());
        // System.out.println(diagnostic.getPosition());
        // System.out.println(diagnostic.getStartPosition());
        // System.out.println(diagnostic.getEndPosition());
        // System.out.println(diagnostic.getSource());
        // System.out.println(diagnostic.getMessage(null));
    }

    public String formatDiagnostics(DiagnosticCollector<JavaFileObject> diagnostics) {
        String nl = System.getProperty("line.separator");
        nl += nl;
        StringBuilder b = new StringBuilder();
        for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
            b.append(formatDiagnostic(diagnostic))
                .append(nl);
        }
        return b.toString();
    }


    public List<String> getCompilationOptions() {

        List<String> options = new ArrayList();
        options.add("-cp");
        options.add(formatClasspath());
        options.add("-sourcepath");
        options.add(getSourcepath().toString());
        options.add("-d");
        options.add(getDestinationpath());
        return options;
    }

    public List<String> getCompilationClasses() {
        return null;
        // List<String> classes = new ArrayList(1);
        // classes.add(className);
        // return classes;
    }

    public Iterable<? extends JavaFileObject>
                              getCompilationUnits(StandardJavaFileManager m,
                                                  ProxyCompiler.Resource src) {
        List<File> files = new ArrayList();
        files.add(src.toFile());
        return m.getJavaFileObjectsFromFiles(files);
    }


}
