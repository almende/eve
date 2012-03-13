package eve.agent.annotation;

import java.io.Serializable;

public class User implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public String authToken;
	public String[] roles;

	public User(String authToken, String[] roles) {
		this.authToken = authToken;
		this.roles = roles;
	}
}
