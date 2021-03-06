/*
 * Copyright 2008- the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.xmatthew.spy2servers.thread;

/**
 * @Notice this source code is mainly copied from apache activemq source
 * more info please visit http://activemq.apache.org
 * 
 * @version $Revision: 1.1 $
 */
public class DedicatedTaskRunner implements TaskRunner {
    private final Task task;
    private final Thread thread;

    private final Object mutex = new Object();
    private boolean threadTerminated;
    private boolean pending;
    private boolean shutdown;

    public DedicatedTaskRunner(Task task, String name) {
        this(task, name, 5, true);
    }

    public DedicatedTaskRunner(Task task, String name, int priority, boolean daemon) {
        this.task = task;
        thread = new Thread(name) {
            public void run() {
                runTask();
            }
        };
        thread.setDaemon(daemon);
        thread.setName(name);
        thread.setPriority(priority);
        thread.start();
    }

    /**
     */
    public void wakeup() throws InterruptedException {
        synchronized (mutex) {
            if (shutdown)
                return;
            pending = true;
            mutex.notifyAll();
        }
    }

    /**
     * shut down the task
     * 
     * @throws InterruptedException
     */
    public void shutdown() throws InterruptedException {
        synchronized (mutex) {
            shutdown = true;
            pending = true;
            mutex.notifyAll();

            // Wait till the thread stops.
            if (!threadTerminated) {
                mutex.wait();
            }
        }
    }

    /**
     * shut down the task
     * 
     * @throws InterruptedException
     */
    public void shutdownNoWait() throws InterruptedException {
        synchronized (mutex) {
            shutdown = true;
            pending = true;
            mutex.notifyAll();
        }
    }

    private void runTask() {

        try {
            while (true) {

                synchronized (mutex) {
                    pending = false;
                    if (shutdown) {
                        return;
                    }
                }

                if (task.executed()) {
                    // wait to be notified.
                    synchronized (mutex) {
                        while (!pending) {
                            mutex.wait();
                        }
                    }
                }

            }

        } catch (InterruptedException e) {
            // Someone really wants this thread to die off.
            Thread.currentThread().interrupt();
        } finally {
            // Make sure we notify any waiting threads that thread
            // has terminated.
            synchronized (mutex) {
                threadTerminated = true;
                mutex.notifyAll();
            }
        }
    }
}
