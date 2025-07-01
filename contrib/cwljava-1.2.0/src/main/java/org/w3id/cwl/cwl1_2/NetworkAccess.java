// Copyright Common Workflow Language project contributors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.w3id.cwl.cwl1_2;

import org.w3id.cwl.cwl1_2.utils.Saveable;

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
