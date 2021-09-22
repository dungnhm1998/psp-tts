package asia.leadsgen.psp.exception;

import java.util.Map;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * Created by HungDX on 22-Jan-16.
 */
public class HttpServiceException extends SystemException {

    public HttpServiceException(int code, String reason, Map informationMap) {

        super(new SystemError(code, reason,
                ParamUtil.getString(informationMap, AppParams.NAME, HttpResponseStatus.NO_CONTENT.reasonPhrase()),
                ParamUtil.getString(informationMap, AppParams.MESSAGE, HttpResponseStatus.NO_CONTENT.reasonPhrase()),
                ParamUtil.getString(informationMap, AppParams.DETAILS),
                ParamUtil.getString(informationMap, AppParams.INFORMATION_LINK)));
    }
}
