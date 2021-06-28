# MultiHandlerJava
This is a MultiHandler project inspired by metasploit framework's multihandler module which is written in ruby. I figured I could create one in Java. The code is not really that neat and tidy. Hope anyone who use this find this application useful.

## How to use
### Basic usage
Starting from version 1.1.0, you can use the jar by typing `java -jar multihandler-java-v1.1.0-sources <port>` to start listening connections from the specified port. Is is unfortunately not possible to use CTRL+Z on windows. So, I highly encourage everyone to use the application on Linux.

### Advanced usage
Since the application supports CTRL+Z as mentioned, you need to run `trap '' TSTP` to disable `SIGTSTP` which is used for jobs control in Linux. To re-enable it, you need to run `trap - TSTP`.

## Compilation
Even though there are a few .bat files, linux commands are still used.
