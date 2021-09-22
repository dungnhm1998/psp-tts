package asia.leadsgen.psp.exception;


import asia.leadsgen.psp.error.SystemError;

/**
 * Created by HungDX on 22-Jan-16.
 */
public class SystemException extends RuntimeException {

    private SystemError systemError;

    public SystemError getSystemError() {
        return systemError;
    }

    public SystemException(SystemError error) {
        super(error.getMessage());
        this.systemError = error;
    }
}
