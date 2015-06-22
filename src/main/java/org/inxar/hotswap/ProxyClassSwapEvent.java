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
 * <code>ProxyClass</code> to communicate when a <code>Class</code>
 * has been swapped out.
**/
public class ProxyClassSwapEvent extends ProxyEvent
{
    /**
     * Flexible subclass constructor.
    **/
    protected ProxyClassSwapEvent(Object source)
    {
	super(source);
    }

    /**
     * Standard constructor originates from the given
     * <code>ProxyClass</code> source and carries the given new and
     * old <code>Class</code> instances.
    **/
    public ProxyClassSwapEvent(ProxyClass source, Class oldClass, Class newClass)
    {
	super(source);
	this.oldClass = oldClass;
	this.newClass = newClass;
    }

    /**
     * Returns the associated <code>ProxyClass</code> to which the new
     * and old classes are associated.  This method is equivalent to
     * the expression "<code>(ProxyClass)event.getSource()</code>".
    **/
    public ProxyClass getProxyClass()
    {
	return (ProxyClass)getSource();
    }

    /**
     * Returns the new <code>Class</code> instance.
     **/
    public Class getNewClass()
    {
	return newClass;
    }

    /**
     * Returns the old <code>Class</code> instance.
    **/
    public Class getOldClass()
    {
	return oldClass;
    }

    /**
     * Prints a summary of the class swap.
    **/
    public String toString()
    {
	StringBuffer b = new StringBuffer();

	b.append("(ProxyClassSwapEvent: ").append(newClass.getName())
	    .append(" changed from ").append(System.identityHashCode(oldClass))
	    .append(" to ").append(System.identityHashCode(newClass)).append(')');

	return b.toString();
    }

    protected Class newClass;
    protected Class oldClass;
}



