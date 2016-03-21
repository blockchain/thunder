# thundernetwork

[![Build Status](https://travis-ci.org/matsjj/thundernetwork.svg?branch=master)](https://travis-ci.org/matsjj/thundernetwork)

P2P Software to send Off-Chain Bitcoin Payments

Based on the idea of lightning.network[1], it will allow to let anyone send money anonymously and instantly. 
See [thunder.network](http://thunder.network) for further information and a preliminary version that runs against a central server. 

This is software in alpha status, don't even think about using it in production with real bitcoin.

Donations appreciated:
	13KBW65G6WZxSJZYrbQQRLC6LWE6hJ8mof

## Feature List
- [X] Encryption
- [X] Authentication
- [X] Channel Opening Process
- [X] Payment Debate 
- [X] Relaying Payment
- [X] Settling Payment
- [X] Peer Seeding
- [X] Providing new Peers with Topology
- [X] Basic Gossip Protocol
- [X] Interface for Connecting into Wallet Software    
- [X] Basic Blockchain Watching Capability    
- [ ] Closing a Channel    
- [ ] Hardening against various DDoS attacks   
- [ ] Backing Database Implementation (currently only in memory)    
- [ ] Restoring state after restart - cheking old TX for cheating
- [ ] Claiming funds after counterparty cheated


## Building

### Prerequisites

You need 
```
JDK 1.8+
Maven
```
to build both the node and the wallet software.

### Installation

Executing 
```
./build.sh
```
will run the tests and create the executables. 

### Running

Starting
```
node.jar
```
will start up an autonomous node that will try to connect to the thunder.network and build channels with random nodes. After doing so, it will write a basic configuration to disk to read from after next start. To be an active part of the network, please configure your firewall to allow incoming connections. The default port is 10000, but it can be set in the config file too.

Starting 
```
wallet.jar
```
will start up the wallet. It will ask for known nodes and get a topology of the network. The user can then chose a node to form a channel with and make and receive payments. 


##Architecture

thunder.network uses netty as underlying networking library. There are several layers for encryption, establishing a channel and making payments. 

Additional features will generally live inside their own layer, decoupled from the other layers. 

### Outlook

thunder.network uses a commitment-transaction design that needs both, CSV and Segregated Witness to be completed. Otherwise the payments are not enforcable on the blockchain and are bad promises at best.


### Dual-TX Approach

thunder.network implemented a commit-transaction design where each payment pays to a 2-of-2 multisig + R || TIMEOUT first. Both parties than create an additional revocable transaction paying to the correct receiver. While this adds additional complexity and makes on-chain resolvement more expensive, it allows for decoupling the revocation delay from the refund timespan, which otherwise would not be possible.[1]

### Anchor

The channel-establish process is due to a rework. It currently uses the anchor-escape-fastescape mechanism described in the deployable-lightning paper. However, it has some downsides and is not necessary anymore once we assume SegWit. 
Both parties will likely create the funding transaction together, sending a half-signed tx back and forth, with one party broadcasting it to the network. Because transaction malleability is non-existent, this party cannot hold the other parties funds hostage.



### Optimizations

As this is still a prototype, various optimizations are left open for now, as they would hinder the active development that is going on. For example JSON was chosen to serialize messages, as Gson allows for very prototype-friendly development.




## Ressources

- [1] Lightning Network by Joseph Poon and Thaddeus Dryja, http://lightning.network/
- [2] LN-Implementation by Rusty Russel, https://github.com/ElementsProject/lightning