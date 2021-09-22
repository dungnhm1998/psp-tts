package asia.leadsgen.psp.exception;


import asia.leadsgen.psp.error.SystemError;

/**
 * Created by HungDX on 22-Jan-16.
 */
public class BadRequestException extends SystemException {

    public BadRequestException(SystemError error) {
        super(error);
    }
}
