/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.contrib.cwl.cwl1_2;

import org.hkijena.jipipe.contrib.cwl.cwl1_2.utils.Saveable;

/**
* Auto-generated interface for <I>https://w3id.org/cwl/cwl#NetworkAccess</I><BR>This interface is implemented by {@link NetworkAccessImpl}<BR> <BLOCKQUOTE>
 Indicate whether a process requires outgoing IPv4/IPv6 network
 access.  Choice of IPv4 or IPv6 is implementation and site
 specific, correct tools must support both.
 
 If `networkAccess` is false or not specified, tools must not
 assume network access, except for localhost (the loopback device).
 
 If `networkAccess` is true, the tool must be able to make outgoing
 connections to network resources.  Resources may be on a private
 subnet or the public Internet.  However, implementations and sites
 may apply their own security policies to restrict what is
 accessible by the tool.
 
 Enabling network access does not imply a publicly routable IP
 address or the ability to accept inbound connections.
  </BLOCKQUOTE>
 */
public interface NetworkAccess extends ProcessRequirement, Saveable {
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#NetworkAccess/class</I><BR>
   * <BLOCKQUOTE>
   * Always 'NetworkAccess'   * </BLOCKQUOTE>
   */

  NetworkAccess_class getClass_();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#NetworkAccess/networkAccess</I><BR>

   */

  Object getNetworkAccess();
}
