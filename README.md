# thundernetwork
Server Client Architecture to send Off-Chain Bitcoin Payments

Based on lightning.network, with some modifications that enable similar functionalities at the current time.

See [thunder.network](http://thunder.network) for further information and a pre-built wallet. 

This is software in alpha status, don't even think about using it on MainNet.

Donations appreciated:
	13KBW65G6WZxSJZYrbQQRLC6LWE6hJ8mof

##Build

You need to call 

```
mvn clean install
```

on the parent first, such that the dependencies for all the modules are built first.

###Wallet

Call
```
mvn clean compile assembly:single
```

in the `thunder-clientgui` directory, to get a executable .jar. 
It is a portable version, you can run multiple instances as long as they are based in different directories.

Due to data consistency, you have to wait for the first confirmation of the funds in your wallet, before you can open the channel. 

###Server

Call
```
mvn clean compile assembly:single
```

in the `thunder-server` directory, to get a executable .jar. 
For a working Server instance, you need a running MySQL-Server, you can set your credentials in the `Constants.java` file.

###Client-API

Call
```
mvn install package
```

in the `thunder-client` directory, to get a library .jar to use in other applications.
