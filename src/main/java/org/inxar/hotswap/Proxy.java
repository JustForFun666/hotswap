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
 * A <code>Proxy</code> contains a reference to another
 * <code>Object</code> whose implementation may change during the life
 * of an application; when the change is detected, the underlying
 * sourcefile is recompiled, the class is reloaded, and a new object
 * instance is constructed.  It also acts a container for object
 * constructor arguments that, if non-<code>null</code>, will be used
 * when reflecting new object instances.
 *
 * <P>
 *
 * To use the proxy library effectively, one must be mindful not to
 * hold direct references to the object instance that the
 * <code>Proxy</code> contains (returned by the
 * <code>Proxy.get()</code> method).  Rather, keep the
 * <code>Proxy</code> around instead and retrieve the object as
 * necessary to ensure that the implementation is always current.
 *
 * <P>
 *
 * If the parent <code>ProxyClass</code> of this <code>Proxy</code>
 * was instantiated as a JDK1.3 dynamic proxy class, this
 * <code>Proxy</code> instance will also implement the interfaces of
 * that <code>Class</code>.  In that case the implementation of the
 * <code>Proxy</code> method signatures can potentially collide with
 * user interface method signatures; hence, all the methods in this
 * interface carry a <code>hotswap</code> prefix.  End-users should
 * avoid naming their methods with this prefix.
**/
public interface Proxy 
{
    /**
     * Returns the parent <code>ProxyClass</code>.
    **/
    ProxyClass hotswap_getProxyClass();

    /**
     * Attempts a hotswap transaction.  If it succeeds, the new
     * instance will be returned.  Otherwise, the old (current)
     * instance will be returned.
    **/
    Object hotswap();

    /**
     * Returns the contained <code>Object</code> without trying to
     * hotswap.
    **/
    Object hotswap_getInstance();

    /**
     * Equivalent to
     * <code>hotswap_getProxyClass().hasChanged()</code>.
    **/
    Boolean hotswap_hasChanged();

    /**
     * Explicitly enqueues this <code>Proxy</code> as a participant in
     * the next hotswap transaction in the parent
     * <code>ProxyClass</code>.  This means that this
     * <code>Proxy</code> instance has decided that it wants to
     * hotswap.
    **/
    void hotswap_enqueue();

    /**
     * Explicitly dequeues this <code>Proxy</code> as a participant in
     * the next hotswap transaction in the parent
     * <code>ProxyClass</code>.  This means that this
     * <code>Proxy</code> instance has decided NOT to hotswap.
    **/
    void hotswap_dequeue();

    /**
     * Returns <code>true</code> if this <code>Proxy</code> instance
     * is registered as a participant in the next hotswap transaction.
    **/
    boolean hotswap_isEnqueued();

    /**
     * Returns the value of the <code>isAutoEnqueue</code> property.
    **/
    boolean hotswap_isAutoEnqueue();

    /**
     * Sets the <code>isAutoEnqueue</code> property.  If set to
     * <code>false</code>, this <code>Proxy</code> instance will only
     * attempt to hotswap when the <code>Proxy.hotswap()</code> method
     * is called on this object.  If set to <code>true</code>, this
     * instance will always register itself with the parent ProxyClass
     * to be included in upcoming hotswap transactions.  See the
     * discussion in the main package about this.
    **/
    void hotswap_isAutoEnqueue(boolean isAutoEnqueue);
    
    /**
     * Explicitly releases any resources associated with this *
     * <code>Proxy</code>.  It usually not necessary to worry about *
     * having to explicitly release a <code>Proxy</code>; it is
     * required only if one of the following two conditions are true:
     *
     * <li>The <code>Object</code> <code><b>po</b></code> returned by
     * the <code>hotswap_getInstance()</code> method is known to be an
     * <code>instanceof</code> <code>ProxyObject</code> AND it is
     * known that the implementation of
     * <code><b>po</b>.hotswap_onRelease()</code> requires explicit
     * release (even when not within the context of a hotswap
     * transaction).  This implementation behavior is not recommended.
     * Your object should have a different method that is exposed on
     * the object itself to do any non-hotswap-transaction related
     * cleanup.
     *
     * <li><code><b>po</b>.isAutoEnqueue()</code> returns
     * <code>true</code>.  If a <code>Proxy</code> instance is
     * <i>strict</i> (not lazy), it means that it is always registered
     * with the parent <code>ProxyClass</code> as a hotswap
     * transaction listener.  Of course, if the parent
     * <code>ProxyClass</code> always contains a reference to this
     * object, it will never be garbage collected.  Calling this
     * method ensures that the parent <code>ProxyClass</code> does not
     * hold any references to this <code>Proxy</code> and hence will
     * presumably be garbage-collectable.
    **/
    void hotswap_release();
    
    /**
     * Returns the object constructor arguments or <code>null</code>
     * if none have been specified.  These are passed to the
     * <code>java.lang.reflect.Constructor</code> during object
     * reflection.
    **/
    Object[] hotswap_getConstructorArguments();

    /**
     * Returns the object constructor parameters or <code>null</code>
     * if none have been specified.  These are used select the correct
     * <code>java.lang.reflect.Constructor</code> from the
     * corresponding <code>Class</code>.
    **/
    Class[] hotswap_getConstructorParameters();

    /**
     * Sets the object constructor arguments.
    **/
    void hotswap_setConstructorArguments(Object[] args);

    /**
     * Sets the object constructor parameters.
    **/
    void hotswap_setConstructorParameters(Class[] params);

    /**
     * Adds the given listener of <code>ProxyObjectSwapEvent</code>s.
    **/
    void hotswap_addObjectSwapListener(ProxyEventListener l);

    /**
     * Removes the given listener of <code>ProxyObjectSwapEvent</code>s.
    **/
    void hotswap_removeObjectSwapListener(ProxyEventListener l);
}
