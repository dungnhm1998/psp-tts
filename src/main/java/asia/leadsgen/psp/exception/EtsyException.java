package asia.leadsgen.psp.exception;

import asia.leadsgen.psp.error.SystemError;

public class EtsyException extends SystemException {
    public EtsyException(SystemError error) {
        super(error);
    }
}
