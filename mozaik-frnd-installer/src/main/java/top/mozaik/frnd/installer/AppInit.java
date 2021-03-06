/**
 * This file is part of Mozaik CMS            www.mozaik.top
 * Copyright © 2016 Denis N Ivanchik       danykey@gmail.com
**/
package top.mozaik.frnd.installer;

import java.util.HashMap;
import java.util.Map;

import org.zkoss.zk.ui.WebApp;

import top.mozaik.bknd.api.utils.SpringPropertiesUtil;
import top.mozaik.frnd.plus.zk.constraint.SwitchConstraint;

public final class AppInit {
	private static final String CACHE_PARAM_NAME = "cache";
	private static final String CFG_PARAM_NAME = "cfg";
    
	public static void init(WebApp app) {
    	if(app.getAttribute(CACHE_PARAM_NAME) == null) {
    		final Map<String, Object> map = new HashMap<>();
    		
    			final Map<String, Object> constraintMap = new HashMap<>();
    			constraintMap.put("noempty", new SwitchConstraint(false, "no empty"));
    		
    		map.put("constraint", constraintMap);
    		
    		app.setAttribute(CACHE_PARAM_NAME, map);
    	}
    	/// Access to spring app-context properties
    	/// Examples:
    	/// ${application.attributes.cfg['jdbc.url']}
    	/// ${application.attributes.cfg.someProp}
    	if(app.getAttribute(CFG_PARAM_NAME) == null) {
    		//final String someProp = SpringPropertiesUtil.getProperty("someProp");
    		app.setAttribute(CFG_PARAM_NAME, SpringPropertiesUtil.getMap());
    	}
    }
}
