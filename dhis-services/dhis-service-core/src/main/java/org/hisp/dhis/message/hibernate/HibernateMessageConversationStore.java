package org.hisp.dhis.message.hibernate;

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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.message.MessageConversation;
import org.hisp.dhis.message.MessageConversationStore;
import org.hisp.dhis.message.UserMessage;
import org.hisp.dhis.user.User;
import org.springframework.jdbc.core.RowMapper;

/**
 * @author Lars Helge Overland
 */
public class HibernateMessageConversationStore
    extends HibernateIdentifiableObjectStore<MessageConversation>
    implements MessageConversationStore
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private StatementBuilder statementBuilder;

    public void setStatementBuilder( StatementBuilder statementBuilder )
    {
        this.statementBuilder = statementBuilder;
    }

    // -------------------------------------------------------------------------
    // Implementation methods
    // -------------------------------------------------------------------------

    @Override
    @SuppressWarnings("unchecked")
    public List<MessageConversation> getMessageConversations( User user, boolean followUpOnly, boolean unreadOnly,
        Integer first, Integer max )
    {
        String hql =
            "select mc.id, mc.uid, mc.subject, ui.surname, ui.firstName, mc.lastMessage, ls.surname, ls.firstName, " +
            "um.read, um.followUp, mc.messageCount, mc.created, mc.lastUpdated " +
            "from MessageConversation mc " +
            "inner join mc.userMessages as um " +
            "left join mc.user as ui " +
            "left join mc.lastSender as ls ";

        if ( user != null )
        {
            hql += "where um.user = " + user.getId();
        }

        if ( followUpOnly )
        {
            hql += (user != null ? "and" : "where") + " um.followUp = true ";
        }

        if ( unreadOnly )
        {
            hql += (user != null || followUpOnly ? "and" : "where") + " um.read = false ";
        }

        hql += "order by mc.lastMessage desc ";
        
        Query query = getQuery( hql );

        if ( first != null && max != null )
        {
            query.setFirstResult( first );
            query.setMaxResults( max );
        }

        return (List<MessageConversation>) query.list()
            .stream()
            .map( o -> mapRowToMessageConversations( (Object[]) o ) )
            .collect( Collectors.toList() );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<MessageConversation> getMessageConversations( Collection<String> uids )
    {
        return getSharingCriteria()
            .add( Restrictions.in( "uid", uids ) )
            .list();
    }

    @Override
    public int getMessageConversationCount( User user, boolean followUpOnly, boolean unreadOnly )
    {
        String sql = "select count(*) from messageconversation mc "
            + "left join messageconversation_usermessages mu on mc.messageconversationid=mu.messageconversationid "
            + "left join usermessage um on mu.usermessageid=um.usermessageid " + "where um.userid=" + user.getId()
            + " ";

        if ( followUpOnly )
        {
            sql += "and um.isfollowup=true ";
        }

        if ( unreadOnly )
        {
            sql += "and um.isread=false ";
        }

        return jdbcTemplate.queryForObject( sql, Integer.class );
    }

    @Override
    public long getUnreadUserMessageConversationCount( User user )
    {
        if ( user == null )
        {
            return -1;
        }

        String hql = "select count(*) from MessageConversation m join m.userMessages u where u.user = :user and u.read = false";

        Query query = getQuery( hql );
        query.setEntity( "user", user );

        return (Long) query.uniqueResult();
    }

    @Override
    public int deleteMessages( User sender )
    {
        if ( sender == null )
        {
            return -1;
        }

        String sql = "delete from messageconversation_messages where messageid in ("
            + "select messageid from message where userid = " + sender.getId() + ")";

        getSqlQuery( sql ).executeUpdate();

        String hql = "delete Message m where m.sender = :sender";

        Query query = getQuery( hql );
        query.setEntity( "sender", sender );
        return query.executeUpdate();
    }

    @Override
    public int deleteUserMessages( User user )
    {
        if ( user == null )
        {
            return -1;
        }

        String sql = "delete from messageconversation_usermessages where usermessageid in ("
            + "select usermessageid from usermessage where userid = " + user.getId() + ")";

        getSqlQuery( sql ).executeUpdate();

        String hql = "delete UserMessage u where u.user = :user";

        Query query = getQuery( hql );
        query.setEntity( "user", user );
        return query.executeUpdate();
    }

    @Override
    public int removeUserFromMessageConversations( User lastSender )
    {
        String hql = "update MessageConversation m set m.lastSender = null where m.lastSender = :lastSender";

        Query query = getQuery( hql );
        query.setEntity( "lastSender", lastSender );
        return query.executeUpdate();
    }

    @Override
    public List<UserMessage> getLastRecipients( User user, Integer first, Integer max )
    {
        String sql = " select distinct userinfoid, surname, firstname from userinfo uf"
            + " join usermessage um on (uf.userinfoid = um.userid)"
            + " join messageconversation_usermessages mu on (um.usermessageid = mu.usermessageid)"
            + " join messageconversation mc on (mu.messageconversationid = mc.messageconversationid)"
            + " where mc.lastsenderid = " + user.getId();

        sql += " order by userinfoid desc";

        if ( first != null && max != null )
        {
            sql += " " + statementBuilder.limitRecord( first, max );
        }

        final List<UserMessage> recipients = jdbcTemplate.query( sql, new RowMapper<UserMessage>()
        {
            @Override
            public UserMessage mapRow( ResultSet resultSet, int count )
                throws SQLException
            {
                UserMessage recipient = new UserMessage();

                recipient.setId( resultSet.getInt( 1 ) );
                recipient.setLastRecipientSurname( resultSet.getString( 2 ) );
                recipient.setLastRecipientFirstname( resultSet.getString( 3 ) );

                return recipient;
            }
        } );

        return recipients;
    }

    private MessageConversation mapRowToMessageConversations( Object[] row )
    {
        MessageConversation conversation = new MessageConversation();

        conversation.setId( (int) row[0] );
        conversation.setUid( (String) row[1] );
        conversation.setSubject( (String) row[2] );
        conversation.setUserSurname( (String) row[3] );
        conversation.setUserFirstname( (String) row[4] );
        conversation.setLastMessage( (Date) row[5] );
        conversation.setLastSenderSurname( (String) row[6] );
        conversation.setLastSenderFirstname( (String) row[7] );
        conversation.setRead( (boolean) row[8] );
        conversation.setFollowUp( (boolean) row[9] );
        conversation.setMessageCount( (int) row[10] );
        conversation.setCreated( (Date) row[11] );
        conversation.setLastUpdated( (Date) row[12] );

        return conversation;
    }
}