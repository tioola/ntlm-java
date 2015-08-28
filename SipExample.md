There is example of usage NTLM-java to connect to ms lync server via SIP http://code.google.com/p/ntlm-java/source/browse/trunk/ntlm-examples/sip/src/main/java/org/dvizuha/sip/ntlm/LyncConnector.java.

This example is junit test that connects to lync server and read contacts information.

In order to use this example you have to create appropriate properties file. Run this test and example properties file will be created. Then rename properties file, fill in server URL, user name and password and run test.


Usage is simple:
1) create NtlmAuthenticator instance:
```
            NtlmAuthenticator ntlmAuthentication = new NtlmAuthenticator(NtlmAuthenticator.NtlmVersion.ntlmv2,
 NtlmAuthenticator.ConnectionType.connectionless
                    , NTLM_HOSTNAME, NTLM_DOMAIN, username, password);
```

2) create NtlmSession:
```
NtlmSession ntlmSession = ntlmAuthentication.createSession();
```

3) Generate and send to other point Negotiate Message if this is connection-oriented protocol
```
ntlmSession.generateNegotiateMessage();
```

4) After receiving ChallengeMessage process it:
```
ntlmSession.processChallengeMessage(challengeMessageData);
```

5) Generate and send AuthenticateMessage:
```
ntlmSession.generateAuthenticateMessage();
```

After that your session must be authenticated