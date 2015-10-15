package network.thunder.core.communication.objects.subobjects;

public class AuthenticationObject {
	/**
	 * The 'master' pubkey of our node
	 */
	public byte[] pubkeyServer;

	/**
	 * The signature for
	 * [pubkeyServer+pubkeyClientTemp]
	 * signed with keyServer
	 */
	public byte[] signature;
}
