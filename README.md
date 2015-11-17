# thundernetwork

[![Build Status](https://travis-ci.org/matsjj/thundernetwork.svg?branch=master)](https://travis-ci.org/matsjj/thundernetwork)

P2P Software to send Off-Chain Bitcoin Payments

Based on lightning.network[1], with some modifications that enable similar functionalities at the current time.

See [thunder.network](http://thunder.network) for further information and a preliminary version that runs against a central server. 

This is software in alpha status, don't even think about using it in production with real bitcoin.

Donations appreciated:
	13KBW65G6WZxSJZYrbQQRLC6LWE6hJ8mof

## Channel Design using OP\_CSV + OP\_CLTV

My current estimate for OP\_CLTV and OP\_CSV activation is around Q3 2016. I will shift my work towards implementing the changes proposed by Rusty Russel. [2] Changing the design of the channel is trivial, but there are many unresolved problems like routing that are open for discussion on the mailing list.

This will allow for full P2P networks with hubs/nodes of varying sizes and is a no-trust solution. Due to the short timespan until the needed features are available, pushing a centralised server-client approach into the masses is not reasonable.

Big nodes and a client-server alike structure can always evolve out of a P2P network due to natural economics - if necessary. The same does not hold true the other way round.

For now I will stick with the basic node/client architecture, such that users can also choose to just be ‘most basic customers’, it’s like full node vs. SPV. The servers on their own will form a P2P mesh, where users can chose their nodes. Clients and nodes mostly differ in their connectivity, as servers will have a very high uptime, where clients should not bother about that.

##Architecture

thunder.network uses netty as basic networking library. There are several layers for encryption, establishing a channel and making payments. See https://github.com/matsjj/lightning and related branches for details on the actual implementations.

##TODO
- [X] Encryption
- [X] Authentication
- [ ] Channel Opening Process
- [ ] Payment Debate 
- [ ] Relaying Payment
- [ ] Settling Payment
- [ ] Extended Gossip Protocol + Hardening
- [ ] Interface for Connecting into Wallet Software    


## Ressources

- [1] Lightning Network by Joseph Poon and Thaddeus Dryja, http://lightning.network/
- [2] LN-Implementation by Rusty Russel, https://github.com/ElementsProject/lightning