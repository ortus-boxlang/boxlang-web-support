package ortus.boxlang.web.interceptors;

import java.util.List;

import ortus.boxlang.runtime.config.Configuration;
import ortus.boxlang.runtime.events.BaseInterceptor;
import ortus.boxlang.runtime.events.InterceptionPoint;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;

public class WebConfigLoader extends BaseInterceptor {

	private static final List<String> DEFAULT_DISALLOWED_EXTENSIONS = List.of(
	    "bat",
	    "exe",
	    "cmd",
	    "cfm",
	    "cfc",
	    "cfs",
	    "bx",
	    "bxm",
	    "bxs",
	    "sh",
	    "php",
	    "pl",
	    "cgi",
	    "386",
	    "dll",
	    "com",
	    "torrent",
	    "js",
	    "app",
	    "jar",
	    "pif",
	    "vb",
	    "vbscript",
	    "wsf",
	    "asp",
	    "cer",
	    "csr",
	    "jsp",
	    "drv",
	    "sys",
	    "ade",
	    "adp",
	    "bas",
	    "chm",
	    "cpl",
	    "crt",
	    "csh",
	    "fxp",
	    "hlp",
	    "hta",
	    "inf",
	    "ins",
	    "isp",
	    "jse",
	    "htaccess",
	    "htpasswd",
	    "ksh",
	    "lnk",
	    "mdb",
	    "mde",
	    "mdt",
	    "mdw",
	    "msc",
	    "msi",
	    "msp",
	    "mst",
	    "ops",
	    "pcd",
	    "prg",
	    "reg",
	    "scr",
	    "sct",
	    "shb",
	    "shs",
	    "url",
	    "vbe",
	    "vbs",
	    "wsc",
	    "wsf",
	    "wsh"
	);

	@InterceptionPoint
	public void onConfigurationLoad( IStruct data ) {
		Configuration config = ( Configuration ) data.get( Key.of( "config" ) );
		// append default disallowed file operation extensions in the web runtime only if none have already been set
		if ( config.security.disallowedFileOperationExtensions.isEmpty() ) {
			config.security.disallowedFileOperationExtensions.addAll( DEFAULT_DISALLOWED_EXTENSIONS );
		}

	}

}
