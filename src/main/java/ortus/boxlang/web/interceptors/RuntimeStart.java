package ortus.boxlang.web.interceptors;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.events.BaseInterceptor;
import ortus.boxlang.runtime.events.InterceptionPoint;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.web.bifs.FileUpload;

public class RuntimeStart extends BaseInterceptor {

	public static final Key COMPAT_MODULE_NAME = Key.of( "compat-cfml" );

	@InterceptionPoint
	public void onRuntimeStart( IStruct interceptData ) {
		if ( BoxRuntime.getInstance().getModuleService().getModuleNames().contains( COMPAT_MODULE_NAME ) ) {
			FileUpload.allowPrefixedFileFields = true;
		}
	}

}
