package network.thunder.core.communication;

import network.thunder.core.communication.objects.subobjects.AuthenticationObject;

public class Node {
	private byte[] pubkey;
	private boolean isAuth;
	private boolean sentAuth;
	private boolean authFinished;
	private boolean isReady;
	private boolean hasOpenChannel;

	public boolean processAuthentication (AuthenticationObject authentication) {
		//TODO: Check authentication based on the supplied pubkey

		return true;
	}

	public AuthenticationObject getAuthenticationObject () {
		//TODO: Produce a proper authentication object..

		return new AuthenticationObject();
	}

	public boolean hasSentAuth () {
		return sentAuth;
	}

	public boolean isAuth () {
		return isAuth;
	}

	public boolean allowsAuth () {
		return !isAuth;
	}

	public void finishAuth () {
		authFinished = true;
	}

	public boolean isAuthFinished () {
		return authFinished;
	}
}
