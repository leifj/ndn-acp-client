package net.nordu.acp.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Calendar;

public class CacheObject<T> {

   private Log log = LogFactory.getLog(CacheObject.class);
   public T object;
   public Calendar expires;

   public CacheObject(T object, Calendar expires) {
      this.object = object;
      this.expires = expires;
   }

   public CacheObject(T object, int seconds) {
      this.object = object;
      this.expires = Calendar.getInstance();
      this.expires.add(Calendar.SECOND,seconds);
   }
}
