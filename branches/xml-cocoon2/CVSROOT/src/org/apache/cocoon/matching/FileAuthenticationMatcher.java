package org.apache.cocoon.matching;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import org.apache.avalon.Configurable;
import org.apache.avalon.Configuration;
import org.apache.avalon.ConfigurationException;
import org.apache.cocoon.Request; 
import org.apache.cocoon.Response; 
import org.apache.avalon.utils.Parameters; 

public class FileAuthenticationMatcher extends AuthenticationMatcher /* implements Configurable */ {

	private Hashtable users = new Hashtable();

	public void setConfiguration(Configuration conf) throws ConfigurationException {
		Enumeration params = conf.getConfigurations("param");
		while (params.hasMoreElements()) {
			Configuration param = (Configuration)params.nextElement();
			String name = param.getAttribute("name");
			String value = param.getAttribute("value");
			if (!name.equals("src")) {
				/** load the file indicated by value (relative to what?)
				    fill hashtable with users/passwords (plaintext for now)
			     **/
			}
		}
	}

	public Map match(String pattern, Request request) {
            return (Map) new Hashtable();
        }
	public boolean match(String user, String password) {
		if (user == null || password == null) {
			return false;
		}
		String value = (String)users.get(user);
		if (value == null || !value.equals(password)) {
			return false;
		} else {
			return true;
		}
	}

        public void setup (Request req, Response res, String src, Parameters param) {
        }

}
