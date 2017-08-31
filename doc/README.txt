OpenWS-J
=============================================

The OpenWS library provides a growing set of tools to work with web services at 
a low level. These tools include classes for creating and reading SOAP messages, 
transport-independent clients for connecting to web services, and various 
transports for use with those clients.

OpenWS-J requires a JAXP 1.3 and DOM Level 3 compliant parser, and this
library assumes the use of Xerces and Xalan for historical reasons. You
may choose to use the built-in parser and transform engine at your own
risk.

The use of JDK 1.6 or higher is required. Older Java versions are not
supported.

OpenWS-J is licensed under the Apache License, version 2.

Current release is made for public usa and source code can be found at:
https://github.com/kazeGit/java-openws-hybrid

It was refactored to use latest http client 4.5.2 instead of old 3.1