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
package org.inxar.hotswap.example;

/**
 * Interface that should be implemented by test classes in the
 * <code>$HOTSWAP_HOME/examples</code> directory.  For simplicity,
 * <code>Component</code> implementations need to be public classes
 * and should have a public no-argument constructor.
**/
public interface Component
{
    /**
     * This method will be called during each frame in a thread wait
     * loop.  During each.
     **/
    void execute();

    /**
     * Return the number of milliseconds to sleep inbetween hotswap
     * attempts; a negative number will stop the Hotswap
     * <code>Thread</code> and exit the program.
    **/
    long getSleepMillis();
}
