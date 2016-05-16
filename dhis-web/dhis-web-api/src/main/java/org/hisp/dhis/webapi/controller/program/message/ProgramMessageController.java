package org.hisp.dhis.webapi.controller.program.message;

/*
 * Copyright (c) 2004-2015, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.program.message.DeliveryChannel;
import org.hisp.dhis.program.message.ProgramMessage;
import org.hisp.dhis.program.message.ProgramMessageCategory;
import org.hisp.dhis.program.message.ProgramMessageQueryParams;
import org.hisp.dhis.program.message.ProgramMessageService;
import org.hisp.dhis.program.message.ProgramMessageStatus;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.hisp.dhis.webapi.service.WebMessageService;
import org.hisp.dhis.webapi.utils.WebMessageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Zubair <rajazubair.asghar@gmail.com>
 */

@RestController
@RequestMapping( value = "/messages" )
public class ProgramMessageController
    extends AbstractCrudController<ProgramMessage>
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private ProgramMessageService programMessageService;

    @Autowired
    private WebMessageService webMessageService;

    @Autowired
    private RenderService renderService;

    // -------------------------------------------------------------------------
    // GET
    // -------------------------------------------------------------------------

    @PreAuthorize( "hasRole('ALL') or hasRole('F_MOBILE_SENDSMS')" )
    @RequestMapping( method = RequestMethod.GET, produces = { "application/json" } )
    public void getProgramMessages( @RequestParam( required = false ) Set<String> ou,
        @RequestParam( required = false ) String trackedEntityInstance,
        @RequestParam( required = false ) String emailAddress, @RequestParam( required = false ) String programInstance,
        @RequestParam( required = false ) String programStageInstance,
        @RequestParam( required = false ) ProgramMessageStatus messageStatus,
        @RequestParam( required = false ) Set<DeliveryChannel> deliveryChannels,
        @RequestParam( required = false ) ProgramMessageCategory messageCatagory,
        @RequestParam( required = false ) Date fromDate, @RequestParam( required = false ) Date afterDate,
        @RequestParam( required = false ) Date beforeDate, @RequestParam( required = false ) Date toDate,
        @RequestParam( required = false ) Integer page, @RequestParam( required = false ) Integer pageSize,
        HttpServletRequest request, HttpServletResponse response)
            throws IOException
    {
        ProgramMessageQueryParams params = programMessageService.getFromUrl( ou, trackedEntityInstance, emailAddress,
            programInstance, programStageInstance, messageStatus, deliveryChannels, messageCatagory, page, pageSize,
            fromDate, toDate, afterDate, beforeDate );

        List<ProgramMessage> programMessages = programMessageService.getProgramMessages( params );

        renderService.toJson( response.getOutputStream(), programMessages );
    }

    // -------------------------------------------------------------------------
    // POST
    // -------------------------------------------------------------------------

    @PreAuthorize( "hasRole('ALL') or hasRole('F_MOBILE_SENDSMS')" )
    @RequestMapping( method = RequestMethod.POST, consumes = { "application/json" }, produces = { "application/json" } )
    public void saveMessages( HttpServletRequest request, HttpServletResponse response )
        throws IOException, WebMessageException
    {
        ProgramMessage programMessage = renderService.fromJson( request.getInputStream(), ProgramMessage.class );

        programMessageService.validateProgramMessagePayload( programMessage );

        String result = programMessageService.sendMessage( programMessage );

        if ( result.equals( "success" ) )
        {
            webMessageService.send( WebMessageUtils.created( "Message sent" ), response, request );
        }
        else
        {
            webMessageService.send( WebMessageUtils.error( "Message failed" ), response, request );
        }
    }
}
