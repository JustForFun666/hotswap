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

import java.io.*;

/**
 * Concrete implementation of <code>Runnable</code> that executes a
 * system command using asynchronous IO.  
**/
class KSystemCommand implements Runnable
{
    protected KSystemCommand()
    {
    }

    /**
     * Constructs the instance to execute the given command string.
    **/
    KSystemCommand(String cmd)
    {
	this.cmd = cmd;
    }

    /**
     * Executes the command and gathers stderr and stdout
     * asynchronously.
    **/
    public synchronized void run()
    {
	ASyncReader stdout = null;
	ASyncReader stderr = null;
	Thread t_stdout = null;
	Thread t_stderr = null;

        try {
            
            Process p = Runtime.getRuntime()
		.exec(cmd);
            
	    stdout = 
		new ASyncReader
		    (new BufferedReader
			(new InputStreamReader
			    (p.getInputStream())));

	    stderr = 
		new ASyncReader
		    (new BufferedReader
			(new InputStreamReader
			    (p.getErrorStream())));

	    t_stdout = new Thread(stdout);
	    t_stderr = new Thread(stderr);
	    
	    
        } catch (IOException ex) {
            ex.printStackTrace();
        }

	if (t_stdout != null) t_stdout.start();
	if (t_stderr != null) t_stderr.start();

	try { t_stdout.join(); } catch (InterruptedException iex) {}
	try { t_stderr.join(); } catch (InterruptedException iex) {}
	
	if (stdout != null) out = stdout.getOutput();
	if (stderr != null) err = stderr.getOutput();

    }

    /**
     * Public member that holds the contents of the standard output
     * stream; this will NOT be defined until after the
     * <code>run</code> method completes.
    **/
    public String out;

    /**
     * Public member that holds the contents of the standard error
     * stream; this will NOT be defined until after the
     * <code>run</code> method completes.
    **/
    public String err;

    /**
     * Public member that holds the command string; this will be
     * defined after the constructor returns.
    **/
    public String cmd; 
}

class ASyncReader implements Runnable
{
    private static final int BUF_SIZE = 256;

    ASyncReader(BufferedReader in)
    {
	this.in = in;
    }

    public synchronized void run()
    {
	int len;
	char buf[] = new char[BUF_SIZE];

	try {

	    StringBuffer b = new StringBuffer();

	    while( (len = in.read(buf)) != -1 ) 
		b.append(buf, 0, len);
	    
	    out = b.toString();
	    notify();

	} catch(EOFException eofex) {
	    eofex.printStackTrace();
	} catch(IOException ioex) {
	    ioex.printStackTrace();
	} finally {
	if (in != null) 
	    try {
		in.close();
		in = null;
	    } catch (Exception ex) {
	    }
	}
    }

    public String getOutput()
    {
	return out;
    }

    String out;
    BufferedReader in;
}
