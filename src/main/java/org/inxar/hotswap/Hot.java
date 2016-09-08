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
import java.util.Scanner;
import java.util.Arrays;

/**
 * Static convenience interface for the package.
 **/
public class Hot {

    protected static ProxyCompiler compiler;

    /**
     * Returns the default static instance.  If none has been setup, a
     * new one will be constructed with path assumptions for a
     * maven-type project and a file.  These can be reconfigured as needed.
     */
    public static ProxyCompiler getCompiler() {
        if (compiler == null) {

            try {
                compiler = new KJavaxCompiler();
                //compiler = new KJavacCompiler();
            } catch (IllegalStateException isex) {
                isex.printStackTrace();
                compiler = new KSystemCompiler("javac");
            }

            compiler.setDestinationpath("target/test-classes");
            compiler.setSourcepath("src/test/java");
            compiler.getClasspath().add("target/classes");
            compiler.getClasspath().addAll(getClasspathFromFile("target/deps.classpath"));
        }
        return compiler;
    }

    /**
     * Sets the static compiler instance to the given argument.
     */
    public static void setCompiler(ProxyCompiler c) {
        compiler = c;
    }

    /**
     * Using the given classname, attempt to load-compile the class.
     */
    public static ProxyClass load(String classname) {
        return getCompiler().load(classname);
    }

    /**
     * If the object argument in a Proxy instance, call hotswap() and
     * return the swapped instance. If not a proxy or swap not needed
     * (or failed), return the original argument.
     */
    public static Object swap(Object obj) {
        if (obj instanceof ProxyObject) {
            return ((Proxy)obj).hotswap();
        } else {
            return obj;
        }
    }

    /**
     * Utility method to load a file having the given name as a
     * string, split it by the colon character, and return a list.  To
     * generate such a file, use:
     *
     * `mvn dependency:build-classpath -Dmdep.outputFile=target/deps.classpath`
     */
    public static List<String> getClasspathFromFile(String filename) {
        File file = new File(filename);
        if (!file.exists()) {
            System.err.println("File does not exist: " + filename);
            return Collections.emptyList();
        }
        Scanner scanner = null;
        StringBuilder content = new StringBuilder((int)file.length());

        try {

            scanner = new Scanner(file);
            String lineSeparator = System.getProperty("line.separator");

            while(scanner.hasNextLine()) {
                content.append(scanner.nextLine() + lineSeparator);
            }
            String[] path = content.toString().split(":");
            return Arrays.asList(path);
        } catch (Exception ioex) {
            return Collections.emptyList();
        } finally {
            if (scanner != null)
                scanner.close();
        }

    }

    public static void inspect(Object obj) {
        Class cls = obj.getClass();
        //log("class: " + obj.getClass().getName());
        //log("classloader: " + cls.getClassLoader().getClass().getName());
        //log("interfaces: " + cls.getInterfaces());
        for (Class i : cls.getInterfaces()) {
            log("- " + i.getName());
        }

        if (obj instanceof Proxy) {
          //log("proxy: yes");
          Proxy proxy = (Proxy)obj;
          ProxyClass proxyClass = proxy.hotswap_getProxyClass();
          //log("proxy class: " + proxyClass.getClass().getName());
        }
    }

    static void log(String msg) {
        System.out.println("[Hotswap] " + msg);
    }

}
