# sf
Programming Exercise

Using Apache Maven 3.0.5
Maven home: /usr/share/maven
Java version: 1.7.0_91, vendor: Oracle Corporation
Java home: /usr/lib/jvm/java-7-openjdk-i386/jre
Default locale: en_US, platform encoding: UTF-8
OS name: "linux", version: "3.19.0-25-generic", arch: "i386", family: "unix"

To build:

	mvn assembly:assembly -DdescriptorId=jar-with-dependencies

To Start server:

	cd target/
	java -cp storagetest-1.0-SNAPSHOT-jar-with-dependencies.jar com.schmidt.Server &

To display Client help:

	java -cp storagetest-1.0-SNAPSHOT-jar-with-dependencies.jar com.schmidt.Client -h

Returns:
	usage: Main
	-c <arg>   Data chunk size in bytes.
	-f <arg>   File size in bytes.
	-h         Display Help
	-n <arg>   Client name.
	-s <arg>   Server FQDN or IP address
	-t <arg>   Length of time in seconds for test

To start client:

	java -cp storagetest-1.0-SNAPSHOT-jar-with-dependencies.jar com.schmidt.Client -t 10 -s localhost -n client1
