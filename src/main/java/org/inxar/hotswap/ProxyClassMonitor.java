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
 * The <code>ProxyClassMonitor</code> interface can be used to
 * implement different change detection mechanics.  One should be
 * registered with the ProxyCompiler.
 *
 * The names are prefixed with <code>hotswap_</code> in order to avoid
 * potential name conflicts with end-user code.
 **/
public interface ProxyClassMonitor
{
    /**
     * Callback to set the ProxyCompiler instance.
     */
    void setCompiler(ProxyCompiler compiler);

    /**
     * Has the proxy class changed?
     */
    Boolean hasChanged(ProxyClass cls);

    /**
     * Begin watching this ProxyClass for changes.
     */
    void watch(ProxyClass cls);

    /**
     * Stop watching this ProxyClass for changes.
     */
    void unwatch(ProxyClass cls);
}
