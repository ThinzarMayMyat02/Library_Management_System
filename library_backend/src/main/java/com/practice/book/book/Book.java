package com.practice.book.book;

import com.practice.book.history.BookHistoryTransaction;
import com.practice.book.user.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.List;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@ToString
public class Book extends BaseEntity {

    private String title;
    private String author;
    private String isbn;
    private String synopsis;
    private String publisher;
    private String bookCover;
    private boolean archived;
    private boolean shareable;

    @ManyToOne
    @JoinColumn (name = "owner_id")
    private User owner;

    @OneToMany(mappedBy = "book")
    private List<Feedback>feedbacks;

    @OneToMany(mappedBy = "book")
    private List<BookHistoryTransaction> histories;

    @Transient
    public double getRate(){
        if(feedbacks!=null && feedbacks.size()>0){
            return 0.0;
        }else{
            var rate = this.feedbacks.stream()
                    .mapToDouble(Feedback::getNote)
                    .average()
                    .orElse(0.0);
            return Math.round(rate * 10.0) / 10.0;
        }
    }
}
