package network.thunder.core.helper.callback;

public class ConnectionListener {
    public Command onSuccess = NullCommand.get();
    public Command onFailure = NullCommand.get();

    public ConnectionListener setOnSuccess (Command onSuccess) {
        this.onSuccess = onSuccess;
        return this;
    }

    public ConnectionListener setOnFailure (Command onFailure) {
        this.onFailure = onFailure;
        return this;
    }
}
