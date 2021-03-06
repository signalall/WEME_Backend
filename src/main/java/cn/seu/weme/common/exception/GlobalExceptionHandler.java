package cn.seu.weme.common.exception;

import cn.seu.weme.common.result.ErrorInfo;
import cn.seu.weme.common.result.ExceptionResultInfo;
import cn.seu.weme.common.result.ResultInfo;
import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by LCN on 2016-12-17.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private Logger logger = Logger.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(value = Exception.class)
    @ResponseBody
    public ErrorInfo<String> exceptionHandler(HttpServletRequest req, Exception e){
        ErrorInfo<String> r = new ErrorInfo<>();
        r.setMessage(e.getMessage());
        r.setCode(ErrorInfo.ERROR);
        r.setData("Server Exception");
        r.setUrl(req.getRequestURL().toString());

        logger.error(e.getMessage());
        return r;
    }

    @ExceptionHandler(value = ExceptionResultInfo.class)
    @ResponseBody
    public ResultInfo MyExceptionHandler(HttpServletRequest req, ExceptionResultInfo e){
       return e.getResultInfo();
    }
}
