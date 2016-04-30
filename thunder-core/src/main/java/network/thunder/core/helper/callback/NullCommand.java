package network.thunder.core.helper.callback;

public class NullCommand implements Command {
    private static final NullCommand instance = new NullCommand();

    public static NullCommand get () {
        return instance;
    }

    private NullCommand () {
    }

    @Override
    public void execute () {

    }
}
