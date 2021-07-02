# MultiHandlerJava
This is a MultiHandler project inspired by metasploit framework's multihandler module which is written in ruby. I figured I could create one in Java. The code is not really that neat and tidy. Hope anyone who use this find this application useful.

### Beta Features
I created a beta build for the project. The beta is available inside the `builds/` directory. It features an improved terminal input string buffering which allows the use of arrow keys. The implemented function of the arrow keys are as follows:

  LEFT | RIGHT |  UP   | DOWN  
-------|-------|-------|-------
Caret left | Caret right | Previous command | Newly typed command

## How to use
### Basic usage
Starting from version 1.1.0, you can use the jar by typing `java -jar multihandler-java-v1.1.0.jar [<port>]` (the default port is 4444) in the terminal to start listening connections from the specified port. Is is unfortunately not possible to use CTRL+Z on windows. So, I highly encourage everyone to use the application on Linux.

Since the application supports CTRL+Z as mentioned (to background a session), you need to run `trap '' TSTP` to disable `SIGTSTP` which is used for jobs control in Linux. To re-enable it, you need to run `trap - TSTP`.

### Enabling Beta Features
You can enable beta features by simply typing `java -jar multihandler-java-v2.0.0-beta.jar <port> true` in the terminal.

### Help
You may type `help` to find commands which you can use.
Since the application supports CTRL+Z as mentioned, you need to run `trap '' TSTP` to disable `SIGTSTP` which is used for jobs control in Linux. To re-enable it, you need to run `trap - TSTP`.

## Compilation
Even though there are a few .bat files, linux commands are still used.
