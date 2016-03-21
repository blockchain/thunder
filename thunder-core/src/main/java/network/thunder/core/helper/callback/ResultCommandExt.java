package network.thunder.core.helper.callback;

import com.google.common.util.concurrent.SettableFuture;
import network.thunder.core.helper.callback.results.Result;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class ResultCommandExt implements ResultCommand {
    private SettableFuture<Result> future = SettableFuture.create();

    public final void execute (Result result) {
        future.set(result);
        executeInternal(result);
    }

    public void executeInternal (Result result) {

    }

    public Result await () {
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public Result await (long timeout, TimeUnit timeUnit) {
        try {
            return future.get(timeout, timeUnit);
        } catch (Exception e) {
            return null;
        }
    }
}
