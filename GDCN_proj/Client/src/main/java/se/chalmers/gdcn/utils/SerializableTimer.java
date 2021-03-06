package se.chalmers.gdcn.utils;

import java.io.Serializable;
import java.util.*;

/**
 * Created by Leif on 2014-04-16.
 *
 * Fully serializable timer. Creates runnable that can update the thread.
 */
public abstract class SerializableTimer<E> implements Serializable {

    private final long UPDATE_TIME;
    private final PriorityQueue<Timeout<E>> queue = new PriorityQueue<>();

    /**
     * @param updateTime Number of Milliseconds between check queue
     */
    public SerializableTimer(long updateTime) {
        UPDATE_TIME =  updateTime;
    }

    /**
     * Clock that updates this timer. This class must be Serializable which {@link java.util.Timer} isn't.
     * @return Runnable
     */
    public final Runnable createUpdater(){
        return new Runnable() {
            @Override
            public void run() {
                Timer timer = new Timer(true);
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        update();
                    }
                }, UPDATE_TIME/2, UPDATE_TIME);
            }
        };
    }

    /**
     * @param timer Timer to be resumed
     * @return Daemon thread that runs the timer
     */
    public static Thread resume(SerializableTimer timer){
        Thread timerThread = new Thread(timer.createUpdater());
        timerThread.setDaemon(true);

        timerThread.start();
        return timerThread;
    }

    /**
     * Add timeout. Will call {@link se.chalmers.gdcn.utils.SerializableTimer#handleTimeout(E)} after specified time.
     * @param element element
     * @param date absolute date when <code>handleTimeout()</code> will be called
     */
    public final synchronized void add(E element, Date date){
        Timeout<E> timeout = new Timeout<>(element, date);
        queue.add(timeout);
    }

    /**
     * Remove element from queue so that no timeout is called.
     * @param element element
     * @return if an element was removed.
     */
    public final synchronized boolean remove(E element){
        //Should work since Timeout equals only depend on element
        Timeout<E> timeout = new Timeout<>(element, null);
        return queue.remove(timeout);
    }

    /**
     * @param element Element to reset
     * @param date Future date
     * @return if element was removed before adding
     */
    public final synchronized boolean reset(E element, Date date){
        boolean removed = false;
        if(remove(element)){
            removed = true;
        }
        add(element, date);
        return removed;
    }

    /**
     * Called by clock to check the queue.
     */
    private synchronized void update(){
        final Date currentTime = new Date();
        if(queue.peek()==null){
            //Queue empty, ignore
            return;
        }
        while(queue.peek()!=null && queue.peek().getDate().compareTo(currentTime) < 0){
            Timeout<E> outdated = queue.remove();
            handleTimeout(outdated.element);
        }
    }

    /**
     * This element was set to timeout at this time. Handle in subclass.
     * @param element element
     */
    protected abstract void handleTimeout(E element);

    private static class Timeout<E> implements Serializable, Comparable<Timeout>{

        private final Date date;
        private final E element;

        private Timeout(E element, Date date) {
            this.date = date;
            this.element = element;
        }

        public E getElement() {
            return element;
        }

        public Date getDate() {
            return date;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Timeout)) return false;

            Timeout timeout = (Timeout) o;

            if (!element.equals(timeout.element)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return element.hashCode();
        }

        @Override
        public int compareTo(Timeout replicaTimeout) {
            if(replicaTimeout==null){
                return 1;
            }
            return date.compareTo(replicaTimeout.date);
        }
    }



}
