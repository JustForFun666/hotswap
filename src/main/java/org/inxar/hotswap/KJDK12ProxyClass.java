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
 * Concrete <code>ProxyClass</code> implementation that does not
 * support dynamic proxy classes.
**/
class KJDK12ProxyClass extends ProxyClass
{
    // ================================
    // Constructors
    // ================================

    KJDK12ProxyClass(ProxyCompiler compiler, Class cls)
    {
	super(compiler, cls);

        //log("jdk13 class impl: " + cls.getName());
        //log("classloader: " + cls.getClassLoader().getClass().getName());
    }

    // ================================
    // ProxyClass Methods
    // ================================

    public boolean isJDK13()
    {
	return false;
    }

    public Proxy newInstance()
    {
	return new KProxy(this, isAutoEnqueue);
    }

    public Proxy newInstance(Object[] args)
    {
	return new KProxy(this, isAutoEnqueue, null, args);
    }

    public Proxy newInstance(Class[] params, Object[] args)
    {
	return new KProxy(this, isAutoEnqueue, params, args);
    }

    public Proxy newInstanceH(Object h)
    {
	throw new UnsupportedOperationException("This is not a JDK1.3 ProxyClass.");
    }

    public Proxy newInstanceH(Object h, Object[] args)
    {
	throw new UnsupportedOperationException("This is not a JDK1.3 ProxyClass.");
    }

    public Proxy newInstanceH(Object h, Class[] params, Object[] args)
    {
	throw new UnsupportedOperationException("This is not a JDK1.3 ProxyClass.");
    }

    public Proxy newInstance(Object h)
    {
	throw new UnsupportedOperationException("This is not a JDK1.3 ProxyClass.");
    }

}
