package org.hisp.dhis.webapi.documentation.controller.dataelement;

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

import com.google.common.collect.Lists;
import org.hisp.dhis.webapi.DhisWebSpringTest;
import org.hisp.dhis.webapi.documentation.common.ResponseDocumentation;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.restdocs.payload.FieldDescriptor;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class DataElementControllerDocumentation
    extends DhisWebSpringTest
{
    //-------------------------------------------------------------------------------------
    // GET
    //-------------------------------------------------------------------------------------

    @Test
    public void testGetAll() throws Exception
    {
        manager.save( createDataElement( 'A' ) );
        manager.save( createDataElement( 'B' ) );
        manager.save( createDataElement( 'C' ) );
        manager.save( createDataElement( 'D' ) );

        MockHttpSession session = getSession( "ALL" );

        List<FieldDescriptor> fieldDescriptors = new ArrayList<>();
        fieldDescriptors.addAll( ResponseDocumentation.pager() );
        fieldDescriptors.add( fieldWithPath( "dataElements" ).description( "Data elements" ) );

        mvc.perform( get( "/dataElements" ).session( session ).accept( MediaType.APPLICATION_JSON ) )
            .andExpect( status().isOk() )
            .andExpect( content().contentTypeCompatibleWith( MediaType.APPLICATION_JSON ) )
            .andExpect( jsonPath( "$.dataElements" ).isArray() )
            .andExpect( jsonPath( "$.dataElements.length()" ).value( 4 ) )
            .andDo( document( "data-elements/all",
                responseFields( fieldDescriptors.toArray( new FieldDescriptor[fieldDescriptors.size()] ) )
            ) );
    }

    @Test
    public void testGetByIdOk() throws Exception
    {
        manager.save( createDataElement( 'A' ) );
        MockHttpSession session = getSession( "ALL" );

        List<FieldDescriptor> fieldDescriptors = new ArrayList<>();
        fieldDescriptors.addAll( ResponseDocumentation.identifiableObject() );
        fieldDescriptors.addAll( ResponseDocumentation.nameableObject() );
        fieldDescriptors.addAll( Lists.newArrayList(
            fieldWithPath( "displayFormName" ).description( "Property" ),
            fieldWithPath( "aggregationType" ).description( "Property" ),
            fieldWithPath( "domainType" ).description( "Property" ),
            fieldWithPath( "valueType" ).description( "Property" ),
            fieldWithPath( "dimensionItem" ).description( "Property" ),
            fieldWithPath( "zeroIsSignificant" ).description( "Property" ),
            fieldWithPath( "optionSetValue" ).description( "Property" ),
            fieldWithPath( "dimensionItemType" ).description( "Property" ),
            fieldWithPath( "categoryCombo" ).description( "Property" ),
            fieldWithPath( "dataElementGroups" ).description( "Property" ),
            fieldWithPath( "dataSets" ).description( "Property" ),
            fieldWithPath( "aggregationLevels" ).description( "Property" )
        ) );

        mvc.perform( get( "/dataElements/{id}", "deabcdefghA" ).session( session ).accept( MediaType.APPLICATION_JSON ) )
            .andExpect( status().isOk() )
            .andExpect( content().contentTypeCompatibleWith( MediaType.APPLICATION_JSON ) )
            .andExpect( jsonPath( "$.name" ).value( "DataElementA" ) )
            .andExpect( jsonPath( "$.shortName" ).value( "DataElementShortA" ) )
            .andExpect( jsonPath( "$.code" ).value( "DataElementCodeA" ) )
            .andExpect( jsonPath( "$.description" ).value( "DataElementDescriptionA" ) )
            .andDo( document( "data-elements/id",
                responseFields( fieldDescriptors.toArray( new FieldDescriptor[fieldDescriptors.size()] ) )
            ) );
    }

    @Test
    public void testGetById404() throws Exception
    {
        MockHttpSession session = getSession( "ALL" );

        mvc.perform( get( "/dataElements/{id}", "deabcdefghA" ).session( session ).accept( MediaType.APPLICATION_JSON ) )
            .andExpect( status().isNotFound() );
    }

    //-------------------------------------------------------------------------------------
    // DELETE
    //-------------------------------------------------------------------------------------

    @Test
    public void testDeleteByIdOk() throws Exception
    {
        manager.save( createDataElement( 'A' ) );
        MockHttpSession session = getSession( "ALL" );

        mvc.perform( delete( "/dataElements/{id}", "deabcdefghA" ).session( session ).accept( MediaType.APPLICATION_JSON ) )
            .andExpect( status().isNoContent() );
    }

    @Test
    public void testDeleteById404() throws Exception
    {
        MockHttpSession session = getSession( "ALL" );

        mvc.perform( delete( "/dataElements/{id}", "deabcdefghA" ).session( session ).accept( MediaType.APPLICATION_JSON ) )
            .andExpect( status().isNotFound() );
    }
}
