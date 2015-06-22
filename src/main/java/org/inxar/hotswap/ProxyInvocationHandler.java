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

import java.lang.reflect.*;

/**
 * Reference <code>InvocationHandler</code> implementation.
 * Subclasses can override the <code>preInvoke</code> and
 * <code>postInvoke</code> methods to provide custom behavior.
 * The current implementation looks like:
 *
 * <P>
 * <table border=0 cellpadding="0" cellspacing="0" bgcolor="#999999" width="100%">
 * <tr><td>
 * <table cellpadding="0" cellspacing="1"><tr><td><font face="helvetica" color="#eeeeee">
 * Implementation of <code>AInvocationHandler.invoke</code> method.
 * </font></td></tr></table>
 * </td></tr>
 * <tr><td>
 * <table border=0 cellpadding="7" cellspacing="1" bgcolor="#eeeeff" width="100%"><tr><td>
 * <pre>
 *  public Object invoke(Object src, Method method, Object[] args) throws Throwable
 *  {
 *      preInvoke(src, method, args);
 *
 *      Object result = method.getName().startsWith("hotswap") 
 *          ? method.invoke(proxy,                       args)
 *          : method.invoke(proxy.hotswap_getInstance(), args);
 *
 *      postInvoke(src, method, args, result);
 *
 *      return result;
 *  }
 *
 *  // Set within the setProxy method.
 *  protected Proxy proxy;
 * </td></tr></table>
 * </td></tr>
 * </table>
 * <P>
 *
**/
public class ProxyInvocationHandler implements InvocationHandler
{
    // ================================
    // Constructors 
    // ================================

    public ProxyInvocationHandler()
    {
    }

    // ================================
    // InvocationHandler Methods 
    // ================================

    /**
     * Implements <code>InvocationHandler</code>.
    **/
    public Object invoke(Object src, Method method, Object[] args) throws Throwable
    {
	preInvoke(src, method, args);

	Object result = method.getName().startsWith("hotswap") 
	    ? method.invoke(proxy,                       args)
	    : method.invoke(proxy.hotswap_getInstance(), args);

	postInvoke(src, method, args, result);
	
	return result;
    }

    // ================================
    // Other Methods 
    // ================================

    /**
     * Sets the internal <code>Proxy</code> to the given instance.
    **/
    public void setProxy(Proxy proxy)
    {
	this.proxy = proxy;
    }

    /**
     * Returns the internal <code>Proxy</code> instance.
    **/
    public Proxy getProxy()
    {
	return this.proxy;
    }

    /**
     * Called before method dispatch to the internal proxy.
    **/
    public void preInvoke(Object src, Method method, Object[] args)
    {
	proxy.hotswap();
    }

    /**
     * Called after method dispatch to the internal proxy.
    **/
    public void postInvoke(Object src, Method method, Object[] args, Object result)
    {
    }

    // ================================
    // Instance Fields 
    // ================================

    protected Proxy proxy;
}
