# Module clientgui

This is an example of what could be built with the thunder.network library. It is a simple wallet built around the library, based upon the example wallet
from bitcoinJ[1].

## Structure

The wallet is heavily event-driven and is using the buttons to invoke actions on the library and the event helper to update the UI. The UI is using JavaFX in the background.The FXML files used were generated using SceneBuilder[2].

When an update event has been fired by the library, BitcoinUIModel.update() gets invoked, which in turn updates the ObservableLists / Properties that are bound to the UIin MainController, therefore directly reflecting in the Wallet.

Right now only an InMemoryDBHandler is used, which means that data is not saved across restarts of the application. Furthermore, a MockWallet object is used that does not hold real Bitcoin, but can be used as if it were.


[1] https://github.com/bitcoinj/bitcoinj/tree/master/wallettemplate
[2] http://gluonhq.com/open-source/scene-builder/