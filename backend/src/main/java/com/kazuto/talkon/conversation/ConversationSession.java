package com.kazuto.talkon.conversation;
import com.kazuto.talkon.user.User;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="conversation_sessions")
public class ConversationSession {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id") private User user;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private ConversationStatus status;
 @Column(name="started_at",nullable=false) private Instant startedAt;
 @Column(name="finished_at") private Instant finishedAt;
 @Column(name="created_at",nullable=false) private Instant createdAt;
 @Column(name="updated_at",nullable=false) private Instant updatedAt;
 protected ConversationSession(){}
 public ConversationSession(User user){this.user=user;this.status=ConversationStatus.ACTIVE;this.startedAt=Instant.now();this.createdAt=startedAt;this.updatedAt=startedAt;}
 public Long getId(){return id;} public User getUser(){return user;} public ConversationStatus getStatus(){return status;}
 public Instant getStartedAt(){return startedAt;} public Instant getFinishedAt(){return finishedAt;}
 public void generating(){status=ConversationStatus.FEEDBACK_GENERATING;updatedAt=Instant.now();}
 public void active(){status=ConversationStatus.ACTIVE;updatedAt=Instant.now();}
 public void complete(){status=ConversationStatus.COMPLETED;finishedAt=Instant.now();updatedAt=finishedAt;}
}

