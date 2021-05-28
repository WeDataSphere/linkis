package com.webank.wedatasphere.linkis.errorcode.server.restful;

import com.webank.wedatasphere.linkis.errorcode.common.CommonConf;
import com.webank.wedatasphere.linkis.errorcode.common.LinkisErrorCode;
import com.webank.wedatasphere.linkis.errorcode.server.service.LinkisErrorCodeService;
import com.webank.wedatasphere.linkis.server.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;


@Path("/errorcode")
@Component
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LinkisErrorCodeRestful {


    @Autowired
    private LinkisErrorCodeService linkisErrorCodeService;

    @GET
    @Path(CommonConf.GET_ERRORCODE_URL)
    public Response getErrorCodes(@Context HttpServletRequest request){
        List<LinkisErrorCode> errorCodes = linkisErrorCodeService.getAllErrorCodes();
        Message message = Message.ok();
        message.data("errorCodes", errorCodes);
        return Message.messageToResponse(message);
    }
}
