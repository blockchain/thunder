# clientgui

This is an example of what can be built with the thunder.network library. It is a simple wallet built on the thunder.network library and the bitcoinJ example wallet[1].

## Structure

The wallet is event-driven and uses the buttons to invoke actions on the library and the event helper to update the UI. JavaFX is used as the GUI library. The FXML files were generated using SceneBuilder[2].

When an update event has been fired by the library, `BitcoinUIModel.update()` gets invoked, which in turn updates the `ObservableLists` / `Properties` that are bound to the UI in `MainController` and all changes are reflected immediately in the Wallet UI.

Right now only an `InMemoryDBHandler` is used, which means that data is not saved across restarts of the application. Furthermore, a MockWallet object is used that does not hold real Bitcoin, but can be used as if it were.


[1] https://github.com/bitcoinj/bitcoinj/tree/master/wallettemplate
[2] http://gluonhq.com/open-source/scene-builder/
