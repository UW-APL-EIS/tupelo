/**
 * Copyright © 2016, University of Washington
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 *     * Neither the name of the University of Washington nor the names
 *       of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written
 *       permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL UNIVERSITY OF
 * WASHINGTON BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.uw.apl.tupelo.amqp.objects;

import java.util.ArrayList;
import java.util.List;

/**
 * For transmission, a FileHashQuery is always wrapped in an
 * RPCObject, and so goes on the amqp bus as JSON-encoded text:
 *
 * {"appdata":{"algorithm":"sha1","hashes":["0000000000000000000000000000000000000000","0000000000000000000000000000000000000000"]},"name":"filehashquery","hostname":"rejewski","protocolver":null,"release":null,"platform":"Linux rejewski 3.2.0-68-generic #102-Ubuntu SMP Tue Aug 12 22:02:15 UTC 2014 x86_64 x86_64 x86_64 GNU/Linux","pid":0,"time":1415134659}
 *
 * @see RPCObject for how to wrap, unwrap FileHashQuery instances. Note
 * how locally we hold no RPCObject knowledge at all.
 *
 * @see FileHashQueryTest
*/
public class FileHashQuery {
	
	public FileHashQuery( String algorithm ) {
		this.algorithm = algorithm;
		hashes = new ArrayList<byte[]>();
	}

	public void add( byte[] hash ) {
		hashes.add( hash );
	}

	// Intended to be accessed by amqp.client, amqp.server code only...
	public final String algorithm;
	public final List<byte[]> hashes;
}

// eof


