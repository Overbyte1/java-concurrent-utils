package collection;

import lock.MReentrantLock;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MCopyOnWriteArrayList<T> implements List<T> {
    private volatile Object[] array;
    private volatile int size;
    private Lock mainLock = new MReentrantLock();

    private static final int DEFAULT_CAPACITY = 16;

    @Override
    public boolean add(T t) {
        if(array == null) {
            init();
        }
        try {
            mainLock.lock();
            Object[] tempArray = Arrays.copyOf(array, array.length + 1);
            tempArray[array.length] = t;
            array = tempArray;
            size++;
        } finally {
            mainLock.unlock();
        }
        return true;
    }
    private void init() {
        array = new Object[DEFAULT_CAPACITY];
    }
    @Override
    public T set(int index, T element) {
        if(index >= size || index < 0) {
            throw new IndexOutOfBoundsException();
        }
        try {
            mainLock.lock();
            T ret;
            int len = array.length;
            Object[] tempArray = Arrays.copyOf(array, len);
            ret = (T)tempArray[index];
            tempArray[index] = element;
            array = tempArray;
            return ret;
        } finally {
            mainLock.unlock();
        }
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean contains(Object o) {
        return false;
    }

    @Override
    public Iterator<T> iterator() {
        return null;
    }

    @Override
    public Object[] toArray() {
        return array;
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        return null;
    }



    @Override
    public boolean remove(Object o) {
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return false;
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        return false;
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        return false;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return false;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return false;
    }

    @Override
    public void clear() {

    }

    @Override
    public T get(int index) {
        return (T)array[index];
    }



    @Override
    public void add(int index, T element) {

    }

    @Override
    public T remove(int index) {
        return null;
    }

    @Override
    public int indexOf(Object o) {
        return 0;
    }

    @Override
    public int lastIndexOf(Object o) {
        return 0;
    }

    @Override
    public ListIterator<T> listIterator() {
        return null;
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        return null;
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        return null;
    }
}
