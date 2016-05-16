package org.hisp.dhis.webapi.controller;

/*
 * Copyright (c) 2004-2016, University of Oslo
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
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.hisp.dhis.webapi.utils.WebMessageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.common.collect.Maps;

/**
* @author Lars Helge Overland
*/
@Controller
@RequestMapping( value = TokenController.RESOURCE_PATH )
public class TokenController
{
    public static final String RESOURCE_PATH = "/tokens";
    
    @Autowired
    private DhisConfigurationProvider config;
        
    @RequestMapping( value = "/google", method = RequestMethod.GET, produces = "application/json" )
    public @ResponseBody Map<String, Object> getEarthEngineToken( HttpServletResponse response )
        throws WebMessageException, IOException
    {
        if ( !config.getGoogleCredential().isPresent() )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Token not available" ) );
        }
        
        GoogleCredential credential = config.getGoogleCredential().get();
        
        Map<String, Object> map = Maps.newHashMap();
        
        if ( credential.refreshToken() )
        {
            map.put( "access_token", credential.getAccessToken() );
            map.put( "expires_in", credential.getExpiresInSeconds() );
            map.put( "client_id", config.getProperty( ConfigurationKey.GOOGLE_SERVICE_ACCOUNT_CLIENT_ID ) );
            
            ContextUtils.setCacheControl( response, CacheControl.
                maxAge( credential.getExpiresInSeconds(), TimeUnit.SECONDS ).cachePublic() );
        }
        
        return map;
    }
}
