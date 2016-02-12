package net.nordu.acp.client;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.httpclient.Cookie;

public class CookieUtils {

    private static final Pattern cookiePattern = Pattern.compile("([^=]+)=([^\\;]*);?\\s?");

    public static List<Cookie> parseCookieString(String cookies) {

    	Matcher matcher = cookiePattern.matcher(cookies);
    	List<Cookie> cookieList = new ArrayList<Cookie>();
    	while (matcher.find()) {
        	int groupCount = matcher.groupCount();
        	System.out.println("matched: " + matcher.group(0));
        	for (int groupIndex = 0; groupIndex <= groupCount; ++groupIndex) {
            		System.out.println("group[" + groupIndex + "]=" + matcher.group(groupIndex));
        	}
        	String cookieKey = matcher.group(1);
        	String cookieValue = matcher.group(2);
        	Cookie cookie = new Cookie();
                cookie.setName(cookieKey);
                cookie.setValue(cookieValue);
        	cookieList.add(cookie);
    	}
	return cookieList;
   }

   public static String getCookieString(String cookies, String name) {

        Matcher matcher = cookiePattern.matcher(cookies);
        List<Cookie> cookieList = new ArrayList<Cookie>();
        while (matcher.find()) {
                int groupCount = matcher.groupCount();
                System.out.println("matched: " + matcher.group(0));
                for (int groupIndex = 0; groupIndex <= groupCount; ++groupIndex) {
                        System.out.println("group[" + groupIndex + "]=" + matcher.group(groupIndex));
                }
                String cookieKey = matcher.group(1);
                String cookieValue = matcher.group(2);
                if (cookieKey.equals(name))
		    return cookieValue;
        }
        return null;
   }
}
