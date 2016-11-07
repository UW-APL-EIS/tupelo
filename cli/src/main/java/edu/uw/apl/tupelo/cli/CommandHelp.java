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
package edu.uw.apl.tupelo.cli;

import java.io.InputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Stuart Maclean
 *
 */

public class CommandHelp {

	static CommandHelp help( String name ) {
		String path = "/help/" + name + ".prp";
		InputStream is = CommandHelp.class.getResourceAsStream( path );
		if( is == null ) {
			System.err.println( "No help: " + name );
			return DEFAULTS;
		}
		Properties p = new Properties();
		try {
			p.load( is );
			is.close();
		} catch( IOException ioe ) {
			System.err.println( ioe );
			return DEFAULTS;
		}			
		String summary = p.getProperty( "summary", "SUMMARY" );
		String synopsis = "SYNOPSIS";

		/*
		  Map<String,String> subSynopses = new HashMap();
		for( String name : p.stringPropertyNames() ) {
			if( name.equals( "synopsis" ) ) {
			synopsis = p.getProperty( "synopsis", "SYNOPSIS" );
		*/
		String description = p.getProperty( "description", "DESCRIPTION" );
		List<String> examples = new ArrayList();
		for( int i = 1; i <= 16; i++ ) {
			String ex = p.getProperty( "example." + i );
			if( ex == null )
				continue;
			examples.add( ex );
		}
		return new CommandHelp( name, summary, synopsis,
								description, examples );
	}

	private CommandHelp( String name, String summary, String synopsis,
						 String description, List<String> examples ) {
		this.name = name;
		this.summary = summary;
		this.synopsis = synopsis;
		this.description = description;
		this.examples = examples;
	}

	public String name() {
		return name;
	}
	public String summary() {
		return summary;
	}
	public String synopsis() {
		return synopsis;
	}
	public String description() {
		return description;
	}
	public List<String> examples() {
		return examples == null ? Collections.<String>emptyList() : examples;
	}

	private final String name, summary, synopsis, description;
	private final List<String> examples;
	
	static final CommandHelp DEFAULTS =
		new CommandHelp( "NAME",
						 "SUMMARY",
						 "SYNOPSIS",
						 "DESCRIPTION",
						 null );
	
	
}

// eof
