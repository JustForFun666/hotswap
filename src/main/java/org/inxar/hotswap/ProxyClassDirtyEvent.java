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
 * <code>ProxyClass</code> to communicate it has changed and needs
 * to be swapped.
 **/
public class ProxyClassDirtyEvent extends ProxyEvent
{
    /**
     * Flexible subclass constructor.
    **/
    protected ProxyClassDirtyEvent(Object source)
    {
	super(source);
    }

    /**
     * Standard constructor originates from the given
     * <code>ProxyClass</code> source and carries the given new and
     * old <code>Class</code> instances.
    **/
    public ProxyClassDirtyEvent(ProxyClass source)
    {
	super(source);
    }

    /**
     * Returns the associated <code>ProxyClass</code>.
    **/
    public ProxyClass getProxyClass()
    {
	return (ProxyClass)getSource();
    }

    /**
     * Prints a summary of the class swap.
    **/
    public String toString()
    {
	StringBuffer b = new StringBuffer();
	b.append("(ProxyClassDirtyEvent: ")
            .append(System.identityHashCode(getSource()));
	return b.toString();
    }
}
