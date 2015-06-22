/**
 * $Id$
 *
 * Copyright (C) 2015 Paul Cody Johnston - pcj@inxar.org
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

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchService;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.StandardWatchEventKinds;
import com.sun.nio.file.SensitivityWatchEventModifier;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

/**
 * Implementation of ProxyClassMonitor that uses the Java 7 file
 * watcher service to listen for source file changes.
 **/
public class ProxyClassWatcher implements ProxyClassMonitor
{
    protected ProxyCompiler compiler;
    protected final Map<Path,ProxyClass> classes;
    protected final Map<Path,DirEntry> dirs;
    protected final WatchService service;
    protected volatile Thread listener;

    protected static class DirEntry {
        List<ProxyClass> classes = new ArrayList();
        WatchKey key;
    }

    public ProxyClassWatcher() {
        this.classes = new HashMap();
        this.dirs = new HashMap();
        try {
            this.service = FileSystems.getDefault().newWatchService();
        } catch (IOException ioex) {
            ioex.printStackTrace();
            throw new IllegalStateException(ioex.getMessage());
        }
    }

    public void setCompiler(ProxyCompiler compiler) {
        this.compiler = compiler;
    }
    /**
     * Compare timestamps of source and class file.
     */
    public Boolean hasChanged(ProxyClass cls) {

        ProxyCompiler.Resource sourceFile = cls.getSourceFile();
        ProxyCompiler.Resource classFile = cls.getClassFile();

	// Check the status of the file.
	switch (compiler.filestat(sourceFile, classFile)) {

	    /* if it cannot be determined, propogate this uncertainty */
	case ProxyCompiler.RC_FILESTAT_UNKNOWN:
	    return null;

	    /* if the sourcefile does not exist. */
	case ProxyCompiler.RC_FILESTAT_SOURCE_MISSING:
	    throw new ProxyException("Unknown source file "+sourceFile.toString());

	    /* if the classfile is current there is no need run the
               compilation; return trivial */
	case ProxyCompiler.RC_FILESTAT_CLASS_CURRENT:
	    return Boolean.FALSE;

	    /* if the classfile is expired then continue processing */
	case ProxyCompiler.RC_FILESTAT_CLASS_EXPIRED:
	    return Boolean.TRUE;

	default:
	    throw new InternalError(); // bad coding
	}
    }

    /**
     * Begin watching this ProxyClass for changes.
     */
    public void watch(ProxyClass cls) {
        File src = cls.getSourceFile().toFile();
        if (src == null) {
            System.err.println("Warning: can't watch proxyclass (source is apparently not file)");
            return;
        }

        Path file = src.toPath().toAbsolutePath();
        //String filename = file.toString();
        if (!classes.containsKey(file)) {
            Path dir = file.getParent();
            try {
                register(dir, cls);
                classes.put(file, cls);
                System.out.println("[ProxyClassWatcher]: Watching " + file);
            } catch (IOException ioex) {
                System.err.println("Error while trying to watch " + dir);
                ioex.printStackTrace();
            }
        }
    }

    /**
     * Stop watching this ProxyClass for changes.
     */
    public void unwatch(ProxyClass cls) {
        File src = cls.getSourceFile().toFile();
        if (src == null) {
            System.err.println("Warning: can't watch proxyclass (source is apparently not file)");
            return;
        }

        Path file = src.toPath().toAbsolutePath();
        //String filename = file.toString();
        if (classes.containsKey(file)) {
            Path dir = file.getParent();
            classes.remove(file);
            unregister(dir, cls);
            System.out.println("[ProxyClassWatcher]: Unwatching " + file);
        }
    }

    /**
     * Registering this ProxyClass for changes in the given dir.
     */
    public void register(Path dir, ProxyClass cls) throws IOException {
        DirEntry entry = dirs.get(dir);
        if (entry == null) {
            entry = new DirEntry();
            //entry.key = dir.register(service, ENTRY_MODIFY);

            // http://stackoverflow.com/questions/9588737/is-java-7-watchservice-slow-for-anyone-else
            entry.key = dir.register(service,
                                     new WatchEvent.Kind[]{StandardWatchEventKinds.ENTRY_MODIFY},
                                     SensitivityWatchEventModifier.HIGH);
            if (dirs.isEmpty()) {
                start();
            }

            dirs.put(dir, entry);
        }

        entry.classes.add(cls);
    }

    /**
     * Stop registering this ProxyClass for changes.
     */
    public void unregister(Path dir, ProxyClass cls) {
        DirEntry entry = dirs.get(dir);
        if (entry == null) {
            return;
        }

        entry.classes.remove(cls);

        if (entry.classes.isEmpty()) {
            dirs.remove(dir);
            entry.key.cancel();
        }
    }

    protected void start() {
        listener =
            new Thread() {
                public void run() {
                    try {
                        listen();
                    } catch (InterruptedException ex) {
                        listener = null;
                    }
                };
            };
        listener.start();
    }

    protected void stop() {
        Thread t = listener;
        if (t != null) {
            t.interrupt();
        }
    }

    protected void listen() throws InterruptedException {
        while (true) {
            WatchKey key;
            key = service.take();

            for (WatchEvent<?> event : key.pollEvents()) {
                System.out.println("WatchEvent: " + event.getClass());
                WatchEvent.Kind<?> kind = event.kind();

                if (kind == OVERFLOW) {
                    continue;
                }

                @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                Path context = ev.context();
                Path dir = (Path)key.watchable();
                Path file = dir.resolve(context);
                ProxyClass cls = classes.get(file);
                if (cls != null) {
                    System.out.println(kind.name() + ": " + file);
                    if (kind == ENTRY_MODIFY) {
                        cls.setChanged();
                    } else {
                        System.err.println("Ignoring event kind " + kind + " for " + file);
                    }
                } else {
                    System.err.println("No proxyClass registered under "
                                       + file);
                }
            }

            boolean valid = key.reset();
            if (!valid) {
                break;
            }
        }
    }

}
