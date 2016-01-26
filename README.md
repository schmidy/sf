# sf
Programming Exercise

Execute from the /bin directory.

To start the server execute
java -cp ../lib/commons-cli-1.3.1.jar:../lib/log4j-1.2.17.jar:. com.schmidt.Server &

To get help from the client

java -cp ../lib/commons-cli-1.3.1.jar:../lib/log4j-1.2.17.jar:. com.schmidt.Client -h

Returns:

usage: Main
 -c <arg>   Data chunk size in bytes.
 -f <arg>   File size in bytes.
 -h         Display Help
 -n <arg>   Client name.
 -s <arg>   Server FQDN or IP address
 -t <arg>   Length of time in seconds for test

Example of executing the client with options: 
 time: 10 seconds
 server: localhost
 client name: client1

java -cp ../lib/commons-cli-1.3.1.jar:../lib/log4j-1.2.17.jar:. com.schmidt.Client -t 10 -s localhost -n client1