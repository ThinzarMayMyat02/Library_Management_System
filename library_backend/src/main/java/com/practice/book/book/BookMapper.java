package com.practice.book.book;

import com.practice.book.file.FileUtils;
import com.practice.book.history.BookHistoryTransaction;
import org.springframework.stereotype.Component;

@Component
public class BookMapper {

    public Book bookRequestToBook(BookRequest book) {
        return Book.builder()
                .id(book.id())
                .title(book.title())
                .author(book.authorName())
                .synopsis(book.synopsis())
                .isbn(book.isbn())
                .archived(false)
                .shareable(book.sharable())
                .build();
    }

    public BookResponse bookToBookResponse(Book book) {
        return BookResponse.builder()
                .id(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .isbn(book.getIsbn())
                .synopsis(book.getSynopsis())
                .rate(book.getRate())
                .archived(book.isArchived())
                .shareable(book.isShareable())
                .owner(book.getOwner().getUsername())
                .cover(FileUtils.readFileFromLocation(book.getBookCover()))
                .build();
    }

    public BorrowedBookResponse toBorrowedBookResponse(BookHistoryTransaction history) {
        return BorrowedBookResponse.builder()
                .id(history.getBook().getId())
                .title(history.getBook().getTitle())
                .author(history.getBook().getAuthor())
                .isbn(history.getBook().getIsbn())
                .rate(history.getBook().getRate())
                .returned(history.isReturned())
                .returnedApproved(history.isReturnApproved())
                .build();
    }
}
