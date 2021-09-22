package asia.leadsgen.psp.exception;

import asia.leadsgen.psp.error.SystemError;

/**
 * Created by HungDX on 22-Jan-16.
 */
public class ResourceConflictException extends SystemException {

    public ResourceConflictException(SystemError error) {
        super(error);
    }

}
