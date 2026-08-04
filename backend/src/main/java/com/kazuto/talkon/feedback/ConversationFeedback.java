package com.kazuto.talkon.feedback;
import com.kazuto.talkon.conversation.ConversationSession;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="conversation_feedbacks")
public class ConversationFeedback {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @OneToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="session_id",unique=true) private ConversationSession session;
 @Column(nullable=false,columnDefinition="TEXT") private String summary;
 @Column(nullable=false,columnDefinition="JSON") private String strengths;
 @Column(nullable=false,columnDefinition="JSON") private String improvements;
 @Column(nullable=false,columnDefinition="JSON") private String corrections;
 @Column(name="overall_comment",nullable=false,columnDefinition="TEXT") private String overallComment;
 @Column(name="created_at",nullable=false) private Instant createdAt;
 protected ConversationFeedback(){}
 public ConversationFeedback(ConversationSession s,FeedbackData d,com.fasterxml.jackson.databind.ObjectMapper m){session=s;summary=d.summary();overallComment=d.overallComment();createdAt=Instant.now();try{strengths=m.writeValueAsString(d.strengths());improvements=m.writeValueAsString(d.improvements());corrections=m.writeValueAsString(d.corrections());}catch(Exception e){throw new IllegalArgumentException(e);}}
 public String getSummary(){return summary;} public String getStrengths(){return strengths;} public String getImprovements(){return improvements;}
 public String getCorrections(){return corrections;} public String getOverallComment(){return overallComment;} public Instant getCreatedAt(){return createdAt;}
}

