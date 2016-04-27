#!/bin/sh

mvn clean install

cd thunder-core
mvn clean compile assembly:single
cd ..
cd thunder-clientgui
mvn clean compile package
cd ..

cp thunder-clientgui/target/thunder-clientgui-0.1.jar thunder-wallet.jar

cp thunder-core/target/thunder-core-0.1-jar-with-dependencies.jar thunder-node.jar
