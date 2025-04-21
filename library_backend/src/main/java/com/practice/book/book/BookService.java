package com.practice.book.book;

import com.practice.book.common.PageResponse;
import com.practice.book.exception.OperationNotPermittedException;
import com.practice.book.file.FileStorageService;
import com.practice.book.history.BookHistoryTransaction;
import com.practice.book.history.BookHistoryTransactionRepository;
import com.practice.book.user.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookHistoryTransactionRepository bookHistoryTranRepo;
    private final BookRepository bookRepository;
    private final BookMapper bookMapper;
    private final FileStorageService fileStorageService;

    public Integer saveBook(BookRequest reqBook, Authentication authedUser) {
        User user = (User) authedUser.getPrincipal();
        Book book = bookMapper.bookRequestToBook(reqBook);
        book.setOwner(user);
        return bookRepository.save(book).getId();
    }

    public BookResponse findById(Integer bookId) {
        return bookRepository.findById(bookId)
                .map(bookMapper::bookToBookResponse)
                .orElseThrow(()-> new EntityNotFoundException("Book is found by id: "+bookId));

    }

    public PageResponse<BookResponse> findAllBooks(int page, int size, Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<Book> books = bookRepository.findAllDisplayableBooks(pageable,user.getId());
        List<BookResponse> bookResponseList = books.stream()
                .map(bookMapper::bookToBookResponse).toList();
        return new PageResponse<>(
                bookResponseList,
                books.getNumber(),
                books.getSize(),
                books.getTotalElements(),
                books.getTotalPages(),
                books.isFirst(),
                books.isLast()
        );
    }

    public PageResponse<BookResponse> findAllBooksByOwner(int page, int size, Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<Book> books = bookRepository.findAll(BookSpecification.withOwnerId(user.getId()),pageable);
        List<BookResponse> bookResponseList = books.stream()
                .map(bookMapper::bookToBookResponse).toList();
        return new PageResponse<>(
                bookResponseList,
                books.getNumber(),
                books.getSize(),
                books.getTotalElements(),
                books.getTotalPages(),
                books.isFirst(),
                books.isLast()
        );
    }

    public PageResponse<BorrowedBookResponse> findAllBorrowedBooks(int page, int size, Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<BookHistoryTransaction> allBorrowedBooks = bookHistoryTranRepo.findAllBorrowedBooks(pageable,user.getId());
        List<BorrowedBookResponse> borrowedBookResponses = allBorrowedBooks.stream()
                .map(bookMapper::toBorrowedBookResponse).toList();
        return new PageResponse<>(
                borrowedBookResponses,
                allBorrowedBooks.getNumber(),
                allBorrowedBooks.getSize(),
                allBorrowedBooks.getTotalElements(),
                allBorrowedBooks.getTotalPages(),
                allBorrowedBooks.isFirst(),
                allBorrowedBooks.isLast()
        );
    }

    public PageResponse<BorrowedBookResponse> findAllReturnedBooks(int page, int size, Authentication connectedUser) {
         User user = ((User) connectedUser.getPrincipal());
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<BookHistoryTransaction> allBorrowedBooks = bookHistoryTranRepo.findAllReturnedBooks(pageable, user.getId());
        List<BorrowedBookResponse> booksResponse = allBorrowedBooks.stream()
                .map(bookMapper::toBorrowedBookResponse)
                .toList();
        return new PageResponse<>(
                booksResponse,
                allBorrowedBooks.getNumber(),
                allBorrowedBooks.getSize(),
                allBorrowedBooks.getTotalElements(),
                allBorrowedBooks.getTotalPages(),
                allBorrowedBooks.isFirst(),
                allBorrowedBooks.isLast()
        );
    }

    public Integer updateShareableStatus(Integer bookId, Authentication connectedUser) throws OperationNotPermittedException {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(()-> new EntityNotFoundException("Book not found by id: "+bookId));
        User user = (User) connectedUser.getPrincipal();
        if(!Objects.equals(book.getOwner().getId(), user.getId())) {
            throw new OperationNotPermittedException("You cannot update book shareable status.");
        }
        book.setShareable(!book.isShareable());
        return bookRepository.save(book).getId();
    }

    public Integer updateArchivedStatus(Integer bookId, Authentication connectedUser) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(()-> new EntityNotFoundException("Book not found by id: "+bookId));
        User user = (User) connectedUser.getPrincipal();
        if(!Objects.equals(book.getOwner().getId(), user.getId())) {
            throw new OperationNotPermittedException("You cannot update book archived status.");
        }
        book.setArchived(!book.isShareable());
        return bookRepository.save(book).getId();
    }

    public Integer borrowBook(Integer bookId, Authentication connectedUser) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(()-> new EntityNotFoundException("Book not found by id: "+bookId));
        if(book.isArchived() || !book.isShareable()) {
            throw new OperationNotPermittedException("Book cannot be borrowed book since it's archived or not shareable status.");
        }

        User user = (User) connectedUser.getPrincipal();
        if(Objects.equals(book.getOwner().getId(), user.getId())) {
            throw new OperationNotPermittedException("You cannot borrow your own book.");
        }

        final boolean isAlreadyBorrowed = bookHistoryTranRepo.isAlreadyBorrowedByUser(bookId, user.getId());
        if(isAlreadyBorrowed) {
            throw new OperationNotPermittedException("The requested book is already borrowed.");
        }

        BookHistoryTransaction  historyTransaction = BookHistoryTransaction.builder()
                .user(user)
                .book(book)
                .returned(false)
                .returnApproved(false)
                .build();
        return bookHistoryTranRepo.save(historyTransaction).getId();
        }

    public Integer returnBorrowedBook(Integer bookId, Authentication connectedUser) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(()-> new EntityNotFoundException("Book not found by id: "+bookId));
        if(book.isArchived() || !book.isShareable()) {
            throw new OperationNotPermittedException("Book cannot be borrowed book since it's archived or not shareable status.");
        }

        User user = (User) connectedUser.getPrincipal();
        if(Objects.equals(book.getOwner().getId(), user.getId())) {
            throw new OperationNotPermittedException("You cannot borrow or return your own book.");
        }

        BookHistoryTransaction bookHistoryTran = bookHistoryTranRepo.findByBookIdAndUserId(book.getId(), user.getId())
                .orElseThrow(()-> new OperationNotPermittedException("You did not borrow this book."));
        bookHistoryTran.setReturned(true);
        return bookHistoryTranRepo.save(bookHistoryTran).getId();
    }

    public Integer approveReturnBorrowedBook(Integer bookId, Authentication connectedUser) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(()-> new EntityNotFoundException("Book not found by id: "+bookId));
        if(book.isArchived() || !book.isShareable()) {
            throw new OperationNotPermittedException("Book cannot be borrowed book since it's archived or not shareable status.");
        }

        User user = (User) connectedUser.getPrincipal();
        if(Objects.equals(book.getOwner().getId(), user.getId())) {
            throw new OperationNotPermittedException("You cannot borrow or return your own book.");
        }

        BookHistoryTransaction bookHistoryTran = bookHistoryTranRepo.findByBookIdAndOwnerId(book.getId(), user.getId())
                .orElseThrow(()-> new OperationNotPermittedException("The book is not returned yet. You cannot approve its return"));
        bookHistoryTran.setReturnApproved(true);
        return bookHistoryTranRepo.save(bookHistoryTran).getId();
    }

    public void uploadBookCoverPicture(MultipartFile file, Authentication connectedUser, Integer bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("No book found with ID:: " + bookId));
         User user = ((User) connectedUser.getPrincipal());
        var profilePicture = fileStorageService.saveFile(file, connectedUser.getName());
        book.setBookCover(profilePicture);
        bookRepository.save(book);
    }
}
