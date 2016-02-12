package net.nordu.acp.utils;

import java.util.Map;
import java.util.Calendar;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.Math;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.nordu.acp.utils.CacheObject;

public class Cache<T> {

   private Log log = LogFactory.getLog(Cache.class);
   private Map<String,CacheObject<T>> cache;
   private int ttl;
   
   public Cache(int ttl) {
   	this.cache = new ConcurrentHashMap<String,CacheObject<T>>();
        this.ttl = ttl;
   }

   public T get(String key) {
	CacheObject<T> co = this.cache.get(key);
        Calendar now = Calendar.getInstance();
        now.add(Calendar.SECOND,(int)(Math.random() * this.ttl)); // fundge to avoid invalidating all cache objects created at the same time
        if (now.after(co.expires)) {
        	this.cache.remove(key);
        	return null;
        } else {
		return co.object;
        }
   }

   public void set(String key, T value) {
	CacheObject<T> co = new CacheObject<T>(value, this.ttl);
        this.cache.put(key, co);
   }

   public void remove(String key) {
   	this.cache.remove(key);
   }
}
