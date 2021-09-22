package asia.leadsgen.psp.exception;


import asia.leadsgen.psp.error.SystemError;

/**
 * Created by HungDX on 22-Jan-16.
 */
public class LoginException extends SystemException {

    public LoginException(SystemError error) {
        super(error);
    }
}
