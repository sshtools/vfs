package com.sshtools.jafp.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

public abstract class ObjectPool<T, E extends Exception> {
	private long expirationTime;
	private Hashtable<T, Long> locked, unlocked;

	public ObjectPool() {
		expirationTime = 30000; // 30 seconds
		locked = new Hashtable<T, Long>();
		unlocked = new Hashtable<T, Long>();
	}
	
	@SuppressWarnings("unchecked")
	public synchronized Collection<T> shutdown() throws E {
		expirationTime = -1;
		List<T> l = new ArrayList<>();
		l.addAll(locked.keySet());
		l.addAll(unlocked.keySet());
		locked.clear();
		unlocked.clear();
		E e = null;
		for(T t : l) {
			try {
				expire(t);
			} catch (Exception ex) {
				e = (E) ex;
			}
		}
		if(e != null)
			throw e;
		return l;
	}

	protected abstract T create() throws E;

	public abstract boolean validate(T o);

	public abstract void expire(T o) throws E;

	public synchronized T checkOut() throws E {
		if(expirationTime == -1)
			throw new IllegalStateException("Pool is dead.");
		long now = System.currentTimeMillis();
		T t;
		if (unlocked.size() > 0) {
			Enumeration<T> e = unlocked.keys();
			while (e.hasMoreElements()) {
				t = e.nextElement();
				if ((now - unlocked.get(t)) > expirationTime) {
					// object has expired
					unlocked.remove(t);
					expire(t);
					t = null;
				} else {
					if (validate(t)) {
						unlocked.remove(t);
						locked.put(t, now);
						return (t);
					} else {
						// object failed validation
						unlocked.remove(t);
						expire(t);
						t = null;
					}
				}
			}
		}
		// no objects available, create a new one
		t = create();
		locked.put(t, now);
		return (t);
	}

	public synchronized void checkIn(T t) {
		if(expirationTime == -1)
			throw new IllegalStateException("Pool is dead.");
		locked.remove(t);
		unlocked.put(t, System.currentTimeMillis());
	}

	public synchronized void checkInAndExpire(T t) throws E {
		checkIn(t);
		unlocked.remove(t);
		expire(t);
	}
}