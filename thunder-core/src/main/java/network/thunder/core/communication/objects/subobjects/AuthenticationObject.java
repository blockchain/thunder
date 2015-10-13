package network.thunder.core.communication.objects.subobjects;

public class AuthenticationObject {
	public byte[] pubkeyClient;
	public byte[] pubkeyNode;
	public int timestamp;
	public byte[] signature;
}
