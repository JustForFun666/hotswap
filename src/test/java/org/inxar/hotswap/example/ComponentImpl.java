package org.inxar.hotswap.example;

import java.util.Date;
import org.inxar.hotswap.example.Component;
import org.inxar.hotswap.ProxyObject;

public class ComponentImpl implements Component, ProxyObject
{
    public ComponentImpl()
    {
	msg = "Hello World!";

	this.birthdate = new Date();
	this.generation = new Integer(0);
    }

    public String toString()
    {
	Class cls = this.getClass();
	ClassLoader loader = cls.getClassLoader();

	return msg +
	    " originally born ["+birthdate+
	    "], now in generation "+generation+" (Object "+System.identityHashCode(this)+
	    " of Class "+cls.getName()+" "+System.identityHashCode(cls)+
	    " of ClassLoader "+loader.getClass().getName()+" "+System.identityHashCode(loader)+
	    ")";
    }

    public void execute()
    {
	System.out.println(this + " Hello Go Home Now!!!  Oh wait you're back ");
    }

    public long getSleepMillis()
    {
	return 2000;
    }

    public boolean hotswap_onPrepare(Object o)
    {
	System.out.println("[ComponentImpl] Preparing...");

	try {
	    ProxyObject po = (ProxyObject)o;
	    this.tmp_birthdate = (Date)po.hotswap_get("birthdate");
	    this.tmp_generation = (Integer)po.hotswap_get("generation");
	} catch (Exception ex) {
	    return false;
	}

	return true;
    }

    public void hotswap_onCommit()
    {
	System.out.println("[ComponentImpl] Committing...");

	this.birthdate = tmp_birthdate;
	if (tmp_generation != null)
	    this.generation = new Integer(tmp_generation.intValue() + 1);
	this.tmp_birthdate = null;
	this.tmp_generation = null;
    }

    public void hotswap_onRollback()
    {
	System.out.println("[ComponentImpl] Rolling back...");
	this.tmp_birthdate = null;
	this.tmp_generation = null;
    }

    public void hotswap_onRelease()
    {
	System.out.println("[ComponentImpl] Goodbye world...");
	this.birthdate = null;
	this.generation = null;
	this.tmp_birthdate = null;
	this.tmp_generation = null;
    }

    public Object hotswap_get(Object key)
    {
	if ("birthdate".equals(key))
	    return birthdate;
	else if ("generation".equals(key))
	    return generation;
	else
	    return null;
    }

    private Date birthdate;	// time of first generation instantiation
    private Integer generation;	// the number of hotswaps since start
    private Date tmp_birthdate;	// temporary time of first generation instantiation
    private Integer tmp_generation; // temporary the number of hotswaps since start

    private static String msg;		// the message of this class
}
