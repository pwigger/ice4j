/*
 * ice4j, the OpenSource Java Solution for NAT and Firewall Traversal.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ice4j.socket;

import java.net.*;

/**
 * A {@code DatagramPacketFilter} which accepts DTLS packets only.
 *
 * @author Boris Grozev
 */
public class DTLSDatagramFilter
    implements DatagramPacketFilter
{
    /**
     * Determines whether {@code p} looks like a DTLS packet.
     *
     * @param p the {@code DatagramPacket} to check.
     * @return {@code true} if {@code p} looks like a DTLS packet; otherwise,
     * {@code false}.
     */
    public static boolean isDTLS(DatagramPacket p)
    {
        int len = p.getLength();

        if (len > 0)
        {
            byte[] data = p.getData();
            int off = p.getOffset();
            int fb = data[off] & 0xff;

            return 19 < fb && fb < 64;
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean accept(DatagramPacket p)
    {
        return isDTLS(p);
    }
}
