package com.ecommerce.order.messaging;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "dead_letter_events")
public class DeadLetterEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String topic;
    @Column(nullable = false) private String messageKey;
    @JdbcTypeCode(SqlTypes.LONGVARCHAR) @Column(name = "payload", nullable = false) private String payload;
    @Column(nullable = false) private LocalDateTime createdAt;
    @PrePersist void prePersist(){ if(createdAt==null) createdAt=LocalDateTime.now(); }
    public void setTopic(String topic){this.topic=topic;} public void setMessageKey(String messageKey){this.messageKey=messageKey;} public void setPayload(String payload){this.payload=payload;}
}
