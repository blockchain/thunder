#!/bin/sh

mvn clean install

cd thunder-core
mvn clean install
cd ..
cd thunder-node
mvn clean compile assembly:single
cd ..
cd thunder-clientgui
mvn clean compile package
cd ..

cp thunder-clientgui/target/thunder-clientgui-0.1.jar thunder-wallet.jar

cp thunder-node/target/thunder-node-0.1-jar-with-dependencies.jar thunder-node.jar
