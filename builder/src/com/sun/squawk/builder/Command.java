/*
 * Copyright 2004-2008 Sun Microsystems, Inc. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 * 
 * This code is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * only, as published by the Free Software Foundation.
 * 
 * This code is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License version 2 for more details (a copy is
 * included in the LICENSE file that accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License
 * version 2 along with this work; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA
 * 
 * Please contact Sun Microsystems, Inc., 16 Network Circle, Menlo
 * Park, CA 94025 or visit www.sun.com if you need additional
 * information or have any questions.
 */

package com.sun.squawk.builder;

import java.util.*;

/**
 * A Command instance describes a builder command.
 */
public abstract class Command {

    /**
     * A sentinel iterator over an empty collection.
     */
    public final static Iterator EMPTY_ITERATOR = new Iterator() {

        /**
         * {@inheritDoc}
         *
         * @return <tt>false</tt>
         */
        public boolean hasNext() {
            return false;
        }

        /**
         * {@inheritDoc}
         *
         * Throws NoSuchElementException.
         */
        public Object next() {
            throw new NoSuchElementException();
        }

        /**
         * {@inheritDoc}
         *
         * Throws  UnsupportedOperationException.
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }
    };

    /**
     * An iterator that translates a command name to a command as it iterates
     * over a collection of command names.
     */
    class CommandNameIterator implements Iterator {

        private final Iterator nameIterator;

        CommandNameIterator(Collection commandNames) {
            nameIterator = commandNames.iterator();
        }

        /**
         * {@inheritDoc}
         *
         * @return <tt>false</tt>
         */
        public boolean hasNext() {
            return nameIterator.hasNext();
        }

        /**
         * {@inheritDoc}
         *
         * Throws NoSuchElementException.
         */
        public Object next() {
            String name = (String)nameIterator.next();
            Command command = env.getCommand(name);
            if (command == null) {
                throw new BuildException("command not found: " + name);
            }
            return command;
        }

        /**
         * {@inheritDoc}
         *
         * Throws  UnsupportedOperationException.
         */
        public void remove() {
            nameIterator.remove();
        }

    }

    protected final Build env;
    protected final String name;
    private ArrayList dependencies;
    private ArrayList triggeredCommands;

    /**
     * Creates a new command.
     *
     * @param   env   the builder environment in which this command will run
     * @param   name  the name of this command
     */
    public Command(Build env, String name) {
        this.env = env;
        this.name = name;
    }

    /**
     * Runs the command.
     *
     * @param  args  the command line argmuents
     * @throws BuildException if the command failed
     */
    public abstract void run(String[] args) throws BuildException;

    /**
     * Gets the name of this command.
     *
     * @return the name of this command
     */
    public String getName() {
        return name;
    }

    /**
     * Gets a brief one-line description of what this command does.
     *
     * @return a brief one-line description of what this command does
     */
    public String getDescription() {
        return "<< no description available >>";
    }

    /**
     * Adds one or more commands that this command depends upon. The dependencies of a command
     * are run before the command itself is run.
     *
     * @param names   the names of the commands to add separated by spaces
     * @return this command
     */
    public final void dependsOn(String names) {
        StringTokenizer st = new StringTokenizer(names);
        while (st.hasMoreTokens()) {
            String name = st.nextToken();
            if (dependencies == null) {
                dependencies = new ArrayList();
            }
            dependencies.add(name);
        }
    }

    /**
     * Gets an iteration of the dependencies of this command.
     *
     * @return an iteration of the dependencies of this command
     */
    public final Iterator getDependencies() {
        return dependencies == null ? EMPTY_ITERATOR : new CommandNameIterator(dependencies);
    }

    /**
     * Adds a command that is triggered by this command. That is, a command that
     * will always be run after this command has been run.
     *
     * @param names   the names of the commands to add separated by spaces
     * @return this command
     */
    public final void triggers(String names) {
        StringTokenizer st = new StringTokenizer(names);
        while (st.hasMoreTokens()) {
            String name = st.nextToken();
            if (triggeredCommands == null) {
                triggeredCommands = new ArrayList();
            }
            triggeredCommands.add(name);
        }
    }

    /**
     * Gets an iteration of the commands triggered by this command.
     *
     * @return an iteration of the commands triggered by this command
     */
    public final Iterator getTriggeredCommands() {
        return triggeredCommands == null ? EMPTY_ITERATOR : new CommandNameIterator(triggeredCommands);
    }

    /**
     * Removes all the files generated by running this command.
     */
    public void clean() {
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return name;
    }
}
