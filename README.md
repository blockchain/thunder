# thundernetwork

Server Client Architecture to send Off-Chain Bitcoin Payments

Based on lightning.network, with some modifications that enable similar functionalities at the current time.

See [thunder.network](http://thunder.network) for further information and a pre-built wallet. 

This is software in alpha status, don't even think about using it on MainNet.

Donations appreciated:
	13KBW65G6WZxSJZYrbQQRLC6LWE6hJ8mof

# Branch for Updated Channel Design, implementing OP_CSV + OP_CLTV

My current estimate for OP_CLTV and OP_CSV activation is around Q3 2016. I will shift my work towards implementing the changes proposed by Rusty Russel. [1] Changing the design of the channel is trivial, but there are many unresolved problems like routing that are open for discussion on the mailing list.

This will allow for full P2P networks with hubs/nodes of varying sizes and is a no-trust solution. Due to the short timespan until the needed features are available, pushing ThunderNetwork into the masses is not reasonable.

Big nodes and a client-server alike structure can always evolve out of a P2P network due to natural economics - if necessary. The same does not hold true the other way round.

For now I will stick with the basic server/client architecture, such that users can also choose to just be ‘most basic customers’, it’s like full node vs. SPV. The servers on their own will form a P2P mesh, where users can chose their nodes. Client and server mostly differ in their connectivity, as servers will have a very high uptime, where clients should not bother about that.


As we don't depend on half-signed transactions from the other party anymore that we have to store, the database design simplifies. 

Namely, we don't have to store 
- explicit revoke / commit / refund transactions for payments anymore
- revoke and refund transactions for channels.

Instead, we have to store the escape and fast escape refund transactions for establishing the channel. 


## Communication

So far all communications need four messages in total to reach a new final state. See thunder-core\src\network\thunder\core\communication\objects for further details.


# Ressources

Further credits for Rusty Russel, who developed this channel design for lightning network.
https://github.com/ElementsProject/lightning
