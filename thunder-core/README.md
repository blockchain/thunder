# Module core

This is the main library used for interacting with the Lightning Network. The main interface that should be used from outside resides within ThunderContext, which must be supplied with an implementation of

```
Wallet
DBHandler
ServerObject
```

Call 

```
startup(..)
```

to do the initialisation with the network, start listening on the specified port, get addresses from other peers in the network and download the topology of the network.

From there on, use

```
openChannel(..)
closeChannel(..)
makePayment(..)
```

for basic interactions with the LN. 

## Structure

Thunder.network uses netty as the underlying networking framework. The design consists of the different layers that are decoupled from each other and only gets connected using the netty pipeline as specified in 

```
PipelineInitialiser
```

This allows for simpler unit tests (test each layer independent of the others) and easier-to-read code.

There are two objects commonly shared across all layers,

```
ClientObject
ServerObject
```

which hold general information about the connection or the settings of both parties (e.g. the node key).

Netty automatically queues messages, such that there is only one message at a time per connection processed by the handlers in the pipeline. 

For interaction between connections, helper classes are injected into the different layers using a ContextFactory.
