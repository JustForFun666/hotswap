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

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Concrete implementation of <code>ProxyCompiler.Resource</code> that
 * wraps a <code>File</code>.
**/
class KFileResource implements ProxyCompiler.Resource
{
    KFileResource(File file)
    {
	this.file = file;
    }

    public String toString()
    {
	return file.getAbsolutePath();
    }

    public File toFile()
    {
	return file;
    }

    public long lastModified() throws IOException
    {
	return file.lastModified();
    }

    public long length() throws IOException
    {
	return file.length();
    }

    public boolean exists() throws IOException
    {
	return file.exists();
    }

    public InputStream getInputStream() throws IOException
    {
	return new FileInputStream(file);
    }

    protected File file;
}
