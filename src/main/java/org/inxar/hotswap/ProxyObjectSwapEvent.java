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
 * <code>ProxyEvent</code> message type fired by the
 * <code>Proxy</code> to communicate when an <code>Object</code> has
 * been swapped out.
**/
public class ProxyObjectSwapEvent extends ProxyEvent
{
    /**
     * Flexible subclass constructor.
    **/
    protected ProxyObjectSwapEvent(Object source)
    {
	super(source);
    }

    /**
     * Standard constructor originates from the given
     * <code>Proxy</code> source and carries the given new and old
     * <code>Object</code> instances.
    **/
    public ProxyObjectSwapEvent(Proxy source, Object oldInstance, Object newInstance)
    {
	super(source);
	this.oldInstance = oldInstance;
	this.newInstance = newInstance;
    }

    /**
     * Returns the associated <code>Proxy</code> to which the new and
     * old instances are associated.  This method is equivalent to the
     * expression "<code>(Proxy)event.getSource()</code>".
    **/
    public Proxy getProxy()
    {
	return (Proxy)getSource();
    }

    /**
     * Returns the new instance.  If the object in an
     * <code>instanceof</code> <code>ProxyObject</code>,
     * <code>proxy_onCreate(Object)</code> has already been invoked.
    **/
    public Object getNewInstance()
    {
	return newInstance;
    }

    /**
     * Returns the old instance.  If the object in an
     * <code>instanceof</code> <code>ProxyObject</code>,
     * <code>proxy_onDestroy()</code> has already been invoked.
    **/
    public Object getOldInstance()
    {
	return oldInstance;
    }

    /**
     * Prints a summary of the object swap.
    **/
    public String toString()
    {
	StringBuffer b = new StringBuffer();

	b.append("(ProxyObjectSwapEvent: ").append(newInstance.getClass().getName())
	    .append(" changed from ").append(System.identityHashCode(oldInstance))
	    .append(" to ").append(System.identityHashCode(newInstance)).append(')');

	return b.toString();
    }

    protected Object newInstance;
    protected Object oldInstance;
}
