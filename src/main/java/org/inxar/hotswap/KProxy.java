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

import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;
import java.lang.reflect.Constructor;

/**
 * Concrete <code>Proxy</code> implementation.
**/
class KProxy implements Proxy
{
    // ================================
    // Constructors
    // ================================

    KProxy(ProxyClass cls, boolean isAutoEnqueue, Class[] params, Object[] args)
    {
	this.cls = cls;

	if (params != null && args != null) {
	    params = new Class[args.length];
	    for (int i = 0; i < args.length; i++)
		params[i] = args[i].getClass();
	    hotswap_setConstructorParameters(params);
	}

	hotswap_isAutoEnqueue(isAutoEnqueue);
	hotswap_setConstructorParameters(params);
	hotswap_setConstructorArguments(params);

        //log("[KProxy] auto-enqueue: " + isAutoEnqueue);

        if (isAutoEnqueue) {
            cls.enqueue(this);
        }
    }

    KProxy(ProxyClass cls, boolean isAutoEnqueue, Object[] args)
    {
	this(cls, isAutoEnqueue, null, args);
    }

    KProxy(ProxyClass cls, boolean isAutoEnqueue)
    {
	this(cls, isAutoEnqueue, null, null);
    }

    KProxy(ProxyClass cls)
    {
	this(cls, false, null, null);
    }

    protected void log(String msg) {
        System.out.println("["+this.getClass().getName()+"] " + msg);
    }

    // ================================
    // TEHC Methods
    // ================================

    public String toString()
    {
	return "(Proxy of class "+cls.getClassInstance().getName()+": "+obj+")";
    }

    public int hashCode()
    {
	return super.hashCode() ^ hash(params) ^ hash(args);
    }

    protected int hash(Object[] o)
    {
	int hash = 0;
	if (o != null)
	    for (int i = 0; i < o.length; i++) {
		hash += o[i].hashCode();
		hash ^= hash;
	    }
	return hash;
    }

    public boolean equals(Object other)
    {
	if (other == this)
	    return true;

	if (!(other instanceof KProxy))
	    return false;

	KProxy that = (KProxy)other;

	// Compare class equality, parameter types, and argument types.
	return (this.cls.getClassInstance().equals(that.cls.getClassInstance())) &&
	    (equal(this.params, that.params)) &&
	    (equal(this.args, that.args));
    }

    protected boolean equal(Object[] _this, Object[] _that)
    {
	if (_this != null) {
	    if (_that != null) {
		if (_this.length != _that.length)
		    return false;
		for (int i = 0; i < _this.length; i++)
		    if (!_this[i].equals(_that[i]))
			return false;
	    } else if (_this.length != 0)
		return false;
	} else if (_that != null || _that.length != 0)
	    return false;

	return true;
    }

    // ================================
    // Proxy Methods
    // ================================

    public ProxyClass hotswap_getProxyClass()
    {
	return cls;
    }

    /* there may be deadlock conditions around this code */
    public Object hotswap()
    {
        System.out.println("[KProxy] hotswap invoked");

	// If the object is null, this is the first time the object
	// has been constructed (or the arguments have been updated).
	if (obj == null)
	    obj = hotswap_newInstance();

	// Try to recompile.
	hotswap_enqueue();
	cls.hotswap();

	return hotswap_getInstance();
    }

    synchronized public Object hotswap_getInstance()
    {
	// If the object is null, this is the first time the object
	// has been constructed (or the arguments have been updated).
	if (obj == null)
	    obj = hotswap_newInstance();

	return obj;
    }

    synchronized public Boolean hotswap_hasChanged()
    {
	return ((bits & CHANGED_MASK) != 0)
	    ? Boolean.TRUE
	    : cls.hasChanged();
    }

    public Object[] hotswap_getConstructorArguments()
    {
	return args;
    }

    public Class[] hotswap_getConstructorParameters()
    {
	return params;
    }

    synchronized public void hotswap_setConstructorArguments(Object[] args)
    {
	this.args = args;
	this.obj = null;
    }

    synchronized public void hotswap_setConstructorParameters(Class[] params)
    {
	this.params = params;
	this.obj = null;
    }

    synchronized public void hotswap_addObjectSwapListener(ProxyEventListener l)
    {
	if (listeners == null)
	    listeners = new LinkedList();
	listeners.add(l);
    }

    synchronized public void hotswap_removeObjectSwapListener(ProxyEventListener l)
    {
	if (listeners != null)
	    listeners.remove(l);
    }

    synchronized public boolean hotswap_isAutoEnqueue()
    {
	return (bits & AUTO_MASK) != 0;
    }

    synchronized public void hotswap_isAutoEnqueue(boolean isAutoEnqueue)
    {
	this.bits |= AUTO_MASK;
    }

    synchronized public void hotswap_enqueue()
    {
	if (!cls.isEnqueued(this))
	    cls.enqueue(this);
    }

    synchronized public void hotswap_dequeue()
    {
	if (cls.isEnqueued(this))
	    cls.dequeue(this);
    }

    synchronized public boolean hotswap_isEnqueued()
    {
	return cls.isEnqueued(this);
    }

    synchronized public void hotswap_release()
    {
	// Release from the proxy class
	cls.dequeue(this);

	if (tmp instanceof ProxyObject)
	    ((ProxyObject)tmp).hotswap_onRelease();
	tmp = null;
    }

    // ================================
    // Package Methods
    // ================================

    synchronized boolean hotswap_prepare(Class newClass)
    {
	try {

	    tmp = newClass.getDeclaredConstructor(params)
		.newInstance(args);

	} catch (Exception ex) {
	    ex.printStackTrace();
	    return false;
	}

	// Prepare this instance.
	if (tmp instanceof ProxyObject)
	    return ((ProxyObject)tmp).hotswap_onPrepare(obj);
	else
	    return true;
    }

    synchronized void hotswap_commit()
    {
	if (tmp instanceof ProxyObject)
	    ((ProxyObject)tmp).hotswap_onCommit();

	// Swap the old instance with the new.
	Object old = obj;
	obj = tmp;
	tmp = old;

	fire( new ProxyObjectSwapEvent(this, old, obj) );

	// Reenqueue if we are auto.
	if (hotswap_isAutoEnqueue())
	    hotswap_enqueue();
    }

    synchronized void hotswap_rollback()
    {
	if (tmp instanceof ProxyObject)
	    ((ProxyObject)tmp).hotswap_onRollback();
	tmp = null;

	if (hotswap_isAutoEnqueue())
	    hotswap_enqueue();
    }

    // ================================
    // Protected Methods
    // ================================

    synchronized protected Object hotswap_newInstance()
    {
	try {

	    return cls.getClassInstance()
		.getDeclaredConstructor(params)
		.newInstance(args);

	} catch (Exception ex) {
	    throw new ProxyException(ex);
	}

    }

    protected void fire(ProxyObjectSwapEvent evt)
    {
	if (listeners != null && listeners.size() > 0) {
	    Iterator i = listeners.iterator();
	    while (i.hasNext())
		((ProxyEventListener)i.next()).notify(evt);
	}
    }

    // ================================
    // Other Methods
    // ================================

    /* 32 bytes per object */

    protected Class[] params;
    protected Object[] args;
    protected ProxyClass cls;
    protected List listeners;
    protected Object obj;
    protected Object tmp;
    protected long timestamp;

    /* used as a bitset */
    protected int bits;

    protected static final int CHANGED_MASK = 0x0000000F;
    protected static final int AUTO_MASK    = 0x000000F0;
}
