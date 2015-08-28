NT LAN Manager (NTLM) is the name of a family of security protocols in Microsoft Windows®. NTLM
is used by application protocols to authenticate remote users and, optionally, to provide session
security when requested by the application.

NTLM is a challenge-response style authentication protocol. This means that to authenticate a user,
the server sends a challenge to the client. The client then sends back a response that is a function of
the challenge, the user's password, and possibly other information. Computing the correct response
requires knowledge of the user's password. The server (or another party trusted by the server) can
validate the response by consulting an account database to get the user's password and computing
the proper response for that challenge.

The NTLM protocols are embedded protocols. Unlike stand-alone application protocols such as MS-SMB or HTTP, NTLM messages are embedded in the packets of an application protocol that requires
authentication of a user. The application protocol semantics determine how and when the NTLM
messages are encoded, framed, and transported from the client to the server and vice versa.

The NT LAN Manager (NTLM) Authentication Protocol is used in Microsoft Windows® for
authentication between clients and servers. For Microsoft Windows® 2000 Server operating system, Windows® XP operating system, Windows Server® 2003 operating system, Windows Vista® operating system, and Windows Server® 2008 operating system, Kerberos authentication MS-KILE replaces NTLM as the preferred authentication
protocol.

However, NTLM can be used when the Kerberos Protocol Extensions (KILE) do not work, such as in
the following scenarios.
  * One of the machines is not Kerberos-capable.
  * The server is not joined to a domain.
  * The KILE configuration is not set up correctly.
  * The implementation chooses to directly use NLMP.




NTLM Specification can be found here http://msdn.microsoft.com/en-us/library/cc236621%28v=PROT.10%29.aspx.