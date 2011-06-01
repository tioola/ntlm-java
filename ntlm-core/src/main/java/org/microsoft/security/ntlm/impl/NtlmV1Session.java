package org.microsoft.security.ntlm.impl;

import org.microsoft.security.ntlm.NtlmAuthenticator;

import static org.microsoft.security.ntlm.NtlmAuthenticator.*;
import static org.microsoft.security.ntlm.impl.Algorithms.ByteArray;
import static org.microsoft.security.ntlm.impl.Algorithms.*;
import static org.microsoft.security.ntlm.impl.Algorithms.calculateMD5;
import static org.microsoft.security.ntlm.impl.Algorithms.concat;
import static org.microsoft.security.ntlm.impl.NtlmRoutines.*;
import static org.microsoft.security.ntlm.impl.NtlmRoutines.Z;

/**
 * @author <a href="http://profiles.google.com/109977706462274286343">Veritatem Quaeres</a>
 * @version $Id:$
 */
public class NtlmV1Session extends NtlmSessionBase {

    private static final ByteArray MAGIC_KGS_CONSTANT = new ByteArray("KGS!@#$%".getBytes(ASCII_ENCODING));

    private static final boolean LM_AUTHENTICATION = false;
    
    /**
     * 3.1.1.1 Variables Internal to the Protocol
     * A Boolean setting that controls using the NTLM response for the LM
     * response to the server challenge when NTLMv1 authentication is used.<30>
     * <30> Section 3.1.1.1: The default value of this state variable is TRUE. Windows NT Server 4.0 SP3
     * does not support providing NTLM instead of LM responses.
     */
    private static final boolean NO_LM_RESPONSE_NTLM_V1 = true;
    private byte[] ntowfv1;
    private byte[] lmowfv1;


    public NtlmV1Session(ConnectionType connectionType, byte[] ntowfv1, byte[] lmowfv1, WindowsVersion windowsVersion, String hostname, String domain, String username) {
        super(connectionType, windowsVersion, hostname, domain, username);
        this.ntowfv1 = ntowfv1;
        this.lmowfv1 = lmowfv1;
    }


    /*
3.3.1 NTLM v1 Authentication

The following pseudocode defines the details of the algorithms used to calculate the keys used in
NTLM v1 authentication.
Note The LM and NTLM authentication versions are not negotiated by the protocol. It MUST be
configured on both the client and the server prior to authentication. The NTOWF v1 function defined
in this section is NTLM version-dependent and is used only by NTLM v1. The LMOWF v1 function
defined in this section is also version-dependent and is used only by LM and NTLM v1.
The NT and LM response keys MUST be encoded using the following specific one-way functions
where all strings are encoded as RPC_UNICODE_STRING ([MS-DTYP] section 2.3.8).

-- Explanation of message fields and variables:
--  ClientChallenge - The 8-byte challenge message generated by
    the client.
--  LmChallengeResponse - The LM response to the server challenge.
    Computed by the client.
--  NegFlg, User, UserDom - Defined in section 3.1.1.
--  NTChallengeResponse - The NT response to the server challenge.
    Computed by the client.
--  Passwd - Password of the user. If the password is longer than
    14 characters, then the LMOWF v1 cannot be computed. For LMOWF
    v1, if the password is shorter than 14 characters, it is padded
    by appending zeroes.
--  ResponseKeyNT - Temporary variable to hold the results of
    calling NTOWF().
--  ResponseKeyLM - Temporary variable to hold the results of
    calling LMGETKEY.
--  CHALLENGE_MESSAGE.ServerChallenge - The 8-byte challenge message
    generated by the server.
--
-- Functions Used:
--  Z(M)- Defined in section 6.

Define NTOWFv1(Passwd, User, UserDom) as MD4(UNICODE(Passwd))
EndDefine

Define LMOWFv1(Passwd, User, UserDom) as
    ConcatenationOf( DES( UpperCase( Passwd)[0..6],"KGS!@#$%"),
        DES( UpperCase( Passwd)[7..13],"KGS!@#$%"))
EndDefine

Set ResponseKeyNT to NTOWFv1(Passwd, User, UserDom)
Set ResponseKeyLM to LMOWFv1( Passwd, User, UserDom )

Define ComputeResponse(NegFlg, ResponseKeyNT, ResponseKeyLM,
    CHALLENGE_MESSAGE.ServerChallenge, ClientChallenge, Time, ServerName)
As
If (User is set to "" AND Passwd is set to "")
    -- Special case for anonymous authentication
    Set NtChallengeResponseLen to 0
    Set NtChallengeResponseMaxLen to 0
    Set NtChallengeResponseBufferOffset to 0
    Set LmChallengeResponse to Z(1)
ElseIf
    If (LM authentication)
        Set NtChallengeResponseLen to 0
        Set NtChallengeResponseMaxLen to 0
        Set NtChallengeResponseBufferOffset to 0
        Set LmChallengeResponse to DESL(ResponseKeyLM, CHALLENGE_MESSAGE.ServerChallenge)
    ElseIf (NTLMSSP_NEGOTIATE_EXTENDED_SESSIONSECURITY flag is set in NegFlg)
        Set NtChallengeResponse to DESL(ResponseKeyNT, MD5(ConcatenationOf(CHALLENGE_MESSAGE.ServerChallenge, ClientChallenge))[0..7])
        Set LmChallengeResponse to ConcatenationOf{ClientChallenge, Z(16)}
    Else
        Set NtChallengeResponse to DESL(ResponseKeyNT, CHALLENGE_MESSAGE.ServerChallenge)
        If (NoLMResponseNTLMv1 is TRUE)
            Set LmChallengeResponse to NtChallengeResponse
        Else
            Set LmChallengeResponse to DESL(ResponseKeyLM, CHALLENGE_MESSAGE.ServerChallenge)
        EndIf
    EndIf
EndIf

Set SessionBaseKey to MD4(NTOWF)

     */

    @Override
    protected void calculateNTLMResponse(ByteArray time, byte[] clientChallengeArray, ByteArray targetInfo) {
        if (LM_AUTHENTICATION) {
            ntChallengeResponse = null;
            lmChallengeResponse = calculateDESL(lmowfv1, serverChallenge);
        } else if (NTLMSSP_NEGOTIATE_EXTENDED_SESSIONSECURITY.isSet(negotiateFlags)) {
            ntChallengeResponse = calculateDESL(ntowfv1, new ByteArray(calculateMD5(concat(serverChallenge, clientChallengeArray)), 0, 7));
            lmChallengeResponse = concat(clientChallengeArray, Z(16));
        } else {
            ntChallengeResponse = calculateDESL(ntowfv1, serverChallenge);
            lmChallengeResponse = NO_LM_RESPONSE_NTLM_V1 ? ntChallengeResponse : calculateDESL(lmowfv1, serverChallenge);
        }

        sessionBaseKey = calculateMD4(ntowfv1);
    }


    /**
     * 3.4.5.1 KXKEY

     *
     *
     * @return kxkey
     */
    @Override
    protected byte[] kxkey() {
        byte[] kxkey;

        if (NTLMSSP_NEGOTIATE_EXTENDED_SESSIONSECURITY.isSet(negotiateFlags)) {
            /*
3.4.5.1 KXKEY
If NTLM v1 is used and extended session security is negotiated, the key exchange key value is
calculated as follows:
-- Input:
--  SessionBaseKey - A session key calculated from the user's
    password.
--  ServerChallenge - The 8-byte challenge message
    generated by the server.
--  LmChallengeResponse - The LM response to the server challenge.
    Computed by the client.
--
-- Output:
--  KeyExchangeKey - The Key Exchange Key.
--
-- Functions used:
--  ConcatenationOf() - Defined in Section 6.
--  HMAC_MD5() - Defined in Section 6.

Define KXKEY(SessionBaseKey, LmChallengeResponse, ServerChallenge) as
    Set KeyExchangeKey to HMAC_MD5(SessionBaseKey, ConcatenationOf(ServerChallenge, LmChallengeResponse [0..7]))
EndDefine

             */
            kxkey = calculateHmacMD5(sessionBaseKey, concat(serverChallenge, new ByteArray(lmChallengeResponse, 0, 8)));


        } else {

    /*
3.4.5.1 KXKEY
If NTLM v1 is used and extended session security is not negotiated, the 128-bit key exchange key
value is calculated as follows:
-- Input:
--  SessionBaseKey - A session key calculated from the user's
    password.
--  LmChallengeResponse - The LM response to the server challenge.
    Computed by the client.
--  NegFlg - Defined in section 3.1.1.
--
-- Output:
--  KeyExchangeKey - The Key Exchange Key.
--
-- Functions used:
--  ConcatenationOf() - Defined in Section 6.
--  DES() - Defined in Section 6.

Define KXKEY(SessionBaseKey, LmChallengeResponse, ServerChallenge) as
If ( NTLMSSP_NEGOTIATE_LMKEY flag is set in NegFlg)
    Set KeyExchangeKey to ConcatenationOf(DES(LMOWF[0..6], LmChallengeResponse[0..7]),
            DES(ConcatenationOf(LMOWF[7], 0xBDBDBDBDBDBD), LmChallengeResponse[0..7])
        )
Else
    If ( NTLMSSP_REQUEST_NON_NT_SESSION_KEY flag is set in NegFlg)
        Set KeyExchangeKey to ConcatenationOf(LMOWF[0..7], Z(8)),
    Else
        Set KeyExchangeKey to SessionBaseKey
    Endif
Endif
EndDefine

     */
            if (NTLMSSP_NEGOTIATE_LM_KEY.isSet(negotiateFlags)) {
                ByteArray lmChallengeResponse07 = new ByteArray(lmChallengeResponse, 0, 8);
                kxkey = concat(calculateDES(new ByteArray(lmowfv1, 0, 7), lmChallengeResponse07)
                        , calculateDES(new ByteArray(new byte[]{lmowfv1[7], (byte) 0xbd, (byte) 0xbd, (byte) 0xbd, (byte) 0xbd, (byte) 0xbd, (byte) 0xbd, })
                                , lmChallengeResponse07)
                        );
            } else if (NTLMSSP_REQUEST_NON_NT_SESSION_KEY.isSet(negotiateFlags)) {
                kxkey = concat(new ByteArray(lmowfv1, 0, 8), Z(8));
            } else {
                kxkey = sessionBaseKey;
            }
        }
        return kxkey;
    }

    /*
3.3.1 NTLM v1 Authentication

Define NTOWFv1(Passwd, User, UserDom) as MD4(UNICODE(Passwd))
EndDefine
     */
    public static byte[] calculateNTOWFv1(String domain, String username, String password) {
        return calculateMD4(password.getBytes(UNICODE_ENCODING));
    }

    /*
3.3.1 NTLM v1 Authentication
Define LMOWFv1(Passwd, User, UserDom) as
    ConcatenationOf( DES( UpperCase( Passwd)[0..6],"KGS!@#$%"),
        DES( UpperCase( Passwd)[7..13],"KGS!@#$%"))
EndDefine
     */
    public static byte[] calculateLMOWFv1(String domain, String username, String password) {
        try {
            byte[] pwb = password.toUpperCase().getBytes(ASCII_ENCODING);
            byte[] out1 = calculateDES(new ByteArray(pwb, 0, 7), MAGIC_KGS_CONSTANT);
            byte[] out2 = calculateDES(new ByteArray(pwb, 7, 7), MAGIC_KGS_CONSTANT);
            return concat(out1, out2);
        } catch (Exception e) {
            throw new RuntimeException("Internal error", e);
        }
    }


    
}

