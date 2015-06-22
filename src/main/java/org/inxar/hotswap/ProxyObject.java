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
 * The <code>ProxyObject</code> interface is an optional interface
 * that can be implemented by end-user classes to receive lifecycle
 * events.
 *
 * The names are prefixed with <code>hotswap_</code> in order to avoid
 * potential name conflicts with end-user code.
 **/
public interface ProxyObject
{
    /**
     * This is the type-safe window two <code>ProxyObject</code>
     * instances having different <code>Class</code> membership can
     * communicate through.  It is used for passing state from one
     * <code>ProxyObject</code> instance to another within the body of
     * <code>hotswap_onPrepare(ProxyObject oldInstance)</code>.
     **/
    Object hotswap_get(Object key);

    /**
     * Called when the <code>ProxyObject</code> is first involved in a
     * hotswap transaction; the argument is the old
     * <code>Object</code> instance that will be subsequently released
     * (the one that this new object will replace).  The new object
     * may use this old object to synchronize state changes, is
     * desired.
     *
     * <P>The return value of the method should signal whether to
     * commit the swap: if the return value is <code>false</code>,
     * this new instance will <i>not</i> replace the old one; that is,
     * the new object is saying <i>I couldn't successfully accomplish
     * the state translation to the new object, so rather than putting
     * the application in a unpredictable state, just stick with the
     * current one</i>.
     **/
    boolean hotswap_onPrepare(Object oldInstance);

    /**
     * Called after <code>hotswap_onPrepare(Object)</code> in a
     * transaction to signal that consensus has been successfully
     * reached; state changes should be committed.
     **/
    void hotswap_onCommit();

    /**
     * Called after <code>hotswap_onPrepare(Object)</code> in a
     * transaction to signal that consensus has not been successfully
     * reached; state changes be discarded.
     **/
    void  hotswap_onRollback();

    /**
     * Called when the <code>ProxyObject</code> is about to be
     * destroyed.
     **/
    void hotswap_onRelease();
}
